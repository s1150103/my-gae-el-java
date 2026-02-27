package com.example.mygaeel.service;

import com.example.mygaeel.entity.ElTargetEntity;
import com.example.mygaeel.repository.ElTargetRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * ■ 監視対象（ELターゲット）管理サービスクラス
 *
 * 監視対象機器（ElTarget）の登録・検索に関するビジネスロジックを担当します。
 * センサーから受信した sysId をもとにリージョンIDを取得するなど、
 * SensorController や ElStateService から頻繁に呼ばれます。
 */
@Service
public class ElTargetService {

    private final ElTargetRepository repository;

    /** コンストラクタインジェクション：Spring が自動で repository を渡してくれます。 */
    public ElTargetService(ElTargetRepository repository) {
        this.repository = repository;
    }

    /**
     * 監視対象を DB に保存します（新規登録 or 上書き更新）。
     * JPA の save() は主キーが既存なら UPDATE、なければ INSERT を実行します。
     */
    public void save(ElTargetEntity entity) {
        repository.save(entity);
        System.out.println("ElTarget 登録: regionId=" + entity.getRegionId()
                + ", targetId=" + entity.getTargetId());
    }

    /**
     * sysId（センサーID）に対応する ElTarget を検索します。
     * IoT デバイスがデータを送ってきた際に、そのデバイスがどのリージョンに属するかを
     * 調べるために使います。
     *
     * @param sysId IoT デバイスのシステムID
     * @return 見つかれば Optional に包んだ ElTargetEntity、見つからなければ Optional.empty()
     */
    public Optional<ElTargetEntity> getTargetBySysId(String sysId) {
        return repository.findByTargetId(sysId);
    }

    /**
     * targetId で ElTarget を検索します（getTargetBySysId と同じ処理）。
     * 呼び出し元のコンテキストに応じて名前を使い分けています。
     */
    public Optional<ElTargetEntity> getTarget(String targetId) {
        return repository.findByTargetId(targetId);
    }

    /**
     * targetId から regionId（リージョンID）だけを取得する便利メソッド。
     *
     * Optional.map() : Optional の中身が存在する場合だけ変換処理を適用します。
     * orElse(null)   : 値がなければ null を返します。
     *
     * 使用例：
     *   elTargetService.getRegionIdByTarget("DAQA005") → "6"
     */
    public String getRegionIdByTarget(String targetId) {
        return getTarget(targetId).map(ElTargetEntity::getRegionId).orElse(null);
    }

    /**
     * regionId と targetId の組み合わせで ElTarget を検索します。
     * 主キー "regionId#targetId" 形式で DB を直接検索します。
     *
     * @param regionId リージョンID
     * @param targetId ターゲットID
     */
    public Optional<ElTargetEntity> getTargetByKey(String regionId, String targetId) {
        return repository.findById(regionId + "#" + targetId);
    }
}
