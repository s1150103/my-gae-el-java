package com.example.mygaeel.service;

import com.example.mygaeel.entity.ElTargetEntity;
import com.example.mygaeel.entity.ElWorkRecordEntity;
import com.example.mygaeel.model.ElState;
import com.example.mygaeel.model.SensorData;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ■ EL（センサー）の状態管理サービスクラス
 *
 * このシステムの「頭脳」に相当する重要なクラスです。
 * センサーのリアルタイム状態を管理し、稼働開始・終了を自動検出して
 * ElWorkRecord（稼働記録）をDBに書き込みます。
 *
 * 【主な責務】
 *   1. アプリ起動時に XML ファイルからセンサー設定を読み込む（@PostConstruct）
 *   2. センサーデータを受信するたびに「今の値」と「前回の値」を比較する
 *   3. 3.0 という閾値をもとに「稼働開始/継続/終了」を判定する
 *   4. 判定結果に応じて ElWorkRecord を作成・更新する
 *
 * 【ConcurrentHashMap を使う理由】
 * HashMap の並行処理版です。
 * 複数のリクエストが同時に来た場合でも、データの不整合が起きないようにします。
 * 通常の HashMap をマルチスレッド環境で使うと、まれにデータが壊れることがあります。
 */
@Service
public class ElStateService {

    /**
     * XML ファイルから読み込んだ静的なセンサー情報マップ。
     * Key: sysId（センサーID）, Value: ElState（センサーの状態オブジェクト）
     */
    private final Map<String, ElState> stateMap = new ConcurrentHashMap<>();

    /**
     * 実際にデータを受信したセンサーの動的状態マップ。
     * Key: sysId, Value: ElState（最新の状態を保持）
     * stateMap と別に管理することで、実際に稼働中のセンサーだけを追跡します。
     */
    private final Map<String, ElState> dynamicStateMap = new ConcurrentHashMap<>();

    private final ElTargetService elTargetService;
    private final ElWorkRecordService elWorkRecordService;

    public ElStateService(ElTargetService elTargetService, ElWorkRecordService elWorkRecordService) {
        this.elTargetService = elTargetService;
        this.elWorkRecordService = elWorkRecordService;
    }

    /**
     * アプリ起動直後に自動実行される初期化メソッド。
     *
     * 【@PostConstruct とは？】
     * Spring がこのクラスをインスタンス化し、依存性注入を完了した直後に
     * 自動で呼び出されるアノテーションです。
     * コンストラクタの後、最初のリクエスト処理の前に実行されます。
     *
     * 処理内容：
     *   EoE_Eden_Number.xml（センサー設定ファイル）を読み込み、
     *   各センサー（sysId）の初期状態を stateMap に登録します。
     *
     * XML の構造イメージ：
     *   <root>
     *     <region num="6">
     *       <client sysid="DAQA005" />
     *       <client sysid="DAQA006" />
     *     </region>
     *   </root>
     */
    @PostConstruct
    public void initialize() {
        try {
            // クラスパス（resources フォルダ）から XML ファイルを読み込む
            InputStream xmlStream = getClass().getClassLoader().getResourceAsStream("EoE_Eden_Number.xml");
            if (xmlStream == null) {
                System.out.println("EoE_Eden_Number.xml が見つかりません");
                return;
            }

            // XML パーサーを使ってファイルを解析（文字列 → 構造化データ）
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlStream);
            doc.getDocumentElement().normalize();  // 余分な空白ノードを整理

            // XML の全 <region> タグを取得してループ処理
            NodeList regions = doc.getElementsByTagName("region");
            for (int i = 0; i < regions.getLength(); i++) {
                org.w3c.dom.Element region = (org.w3c.dom.Element) regions.item(i);
                String regionId = region.getAttribute("num");

                // 各 <region> の中の <client> タグ（センサー）を取得
                NodeList clients = region.getElementsByTagName("client");
                for (int j = 0; j < clients.getLength(); j++) {
                    org.w3c.dom.Element client = (org.w3c.dom.Element) clients.item(j);
                    String sysId = client.getAttribute("sysid");
                    System.out.println("ElState 登録: sysId=" + sysId + ", regionId=" + regionId);
                    stateMap.put(sysId, new ElState(sysId, regionId));
                }
            }
            System.out.println("ElStateService 初期化完了 (" + stateMap.size() + "件)");
        } catch (Exception e) {
            System.out.println("ElStateService 初期化エラー: " + e.getMessage());
        }
    }

    /**
     * センサーデータを受信して状態を更新し、稼働記録を管理します。
     *
     * 【稼働判定ロジック（閾値: 3.0）】
     *
     *   前回値 ≤ 3.0 かつ 今回値 > 3.0  → 稼働「開始」
     *     ⇒ ElWorkRecord を新規作成してDBに保存し、activeRecords に追加
     *
     *   前回値 ≥ 3.0 かつ 今回値 < 3.0  → 稼働「終了」
     *     ⇒ ElWorkRecord の endTime を更新し、activeRecords から削除
     *
     *   今回値 > 3.0 かつ activeRecords に存在  → 稼働「継続」（最大値更新）
     *     ⇒ 今回の値が最大値を超えていれば ElWorkRecord の maxData を更新
     *
     * @param sensorData IoT デバイスから受信したセンサーデータ
     */
    public void updateState(SensorData sensorData) {
        String sysId = sensorData.getSysId();

        // DB からこのセンサーの登録情報を取得（sysId → regionId, targetId を確認）
        Optional<ElTargetEntity> targetOpt = elTargetService.getTargetBySysId(sysId);
        if (targetOpt.isEmpty()) {
            System.out.println("sysId " + sysId + " に対応する targetId が見つかりません");
            return;
        }

        ElTargetEntity target = targetOpt.get();
        String targetId = target.getTargetId();
        String regionId = target.getRegionId();

        // このセンサーの状態がまだ dynamicStateMap に存在しなければ新規作成
        // computeIfAbsent: キーが存在しない場合だけ、第2引数のラムダ式を実行して値を作成・格納
        dynamicStateMap.computeIfAbsent(sysId, id -> {
            System.out.println("ElState 作成: sysId=" + sysId + ", regionId=" + regionId);
            return new ElState(sysId, regionId);
        });

        ElState state = dynamicStateMap.get(sysId);
        // data1 文字列 → {ch1: 2.5, ch2: 0.0, ...} の形式に解析
        Map<String, Double> parsedData = state.parseSensorData(sensorData.getData1());

        // 初回受信時：前回値を 0.0 で初期化（比較できるようにするため）
        if (state.getLastData() == null) {
            Map<String, Double> init = new HashMap<>();
            for (String ch : parsedData.keySet()) init.put(ch, 0.0);
            state.setLastData(init);
        }

        Map<String, Double> lastData = state.getLastData();

        // チャンネルごとに稼働判定を実行
        for (Map.Entry<String, Double> entry : lastData.entrySet()) {
            String chKey = entry.getKey();  // 例: "ch1"
            double prevValue = entry.getValue() != null ? entry.getValue() : 0.0;  // 前回値
            double newValue  = parsedData.getOrDefault(chKey, 0.0) != null
                    ? parsedData.getOrDefault(chKey, 0.0) : 0.0;  // 今回値

            // チャンネル番号を数値として取得（"ch1" → 1）
            int chNum;
            try {
                chNum = Integer.parseInt(chKey.replace("ch", ""));
            } catch (NumberFormatException e) {
                continue;  // "ch" で始まらない不正なキーはスキップ
            }

            // このセンサーの「現在稼働中のチャンネルごとのレコード」を取得
            Map<String, ElWorkRecordEntity> activeRecords = state.getActiveRecords();

            if (prevValue <= 3.0 && newValue > 3.0) {
                // ── 稼働「開始」 ──────────────────────────────────
                long startTime = System.currentTimeMillis();
                System.out.println("ElWorkRecord 作成: " + regionId + " - " + chNum);
                ElWorkRecordEntity record = new ElWorkRecordEntity(regionId, targetId, startTime, newValue);
                elWorkRecordService.save(record);
                activeRecords.put(chKey, record);  // 稼働中リストに追加

            } else if (prevValue >= 3.0 && newValue < 3.0 && activeRecords.containsKey(chKey)) {
                // ── 稼働「終了」 ──────────────────────────────────
                long endTime = System.currentTimeMillis();
                elWorkRecordService.updateEndTime(activeRecords.get(chKey), endTime);
                activeRecords.remove(chKey);  // 稼働中リストから削除
                System.out.println("ElWorkRecord 更新: " + regionId + " - " + chNum + " (終了)");

            } else if (newValue > 3.0 && activeRecords.containsKey(chKey)) {
                // ── 稼働「継続」（最大値の更新チェック） ─────────────
                ElWorkRecordEntity record = activeRecords.get(chKey);
                if (record.getMaxData() < newValue) {
                    record.setMaxData(newValue);
                    elWorkRecordService.save(record);
                }
            }

            // 今回の値を「前回値」として保存（次回受信時の比較用）
            lastData.put(chKey, newValue);
        }
    }

    /** 現在の動的状態マップを返します（デバッグ・管理画面用）。 */
    public Map<String, ElState> getDynamicStateMap() {
        return dynamicStateMap;
    }

    /**
     * XML から読み込んだすべてのリージョンIDを重複なし・昇順で返します。
     * 管理画面でリージョン一覧を表示する際に使用します。
     *
     * stream().map().distinct().sorted().toList() の意味：
     *   map()      → ElState の中から regionId だけ取り出す
     *   distinct() → 重複を除去
     *   sorted()   → 昇順に並べ替え
     *   toList()   → List に変換
     */
    public List<String> getKnownRegionIds() {
        return stateMap.values().stream()
                .map(ElState::getRegionId)
                .distinct()
                .sorted()
                .toList();
    }
}
