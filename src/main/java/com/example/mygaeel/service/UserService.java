package com.example.mygaeel.service;

import com.example.mygaeel.entity.UserEntity;
import com.example.mygaeel.model.CustomUserDetails;
import com.example.mygaeel.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ■ ユーザー管理サービスクラス
 *
 * 【Service 層の役割】
 * アプリケーションのビジネスロジック（業務ルール）を担当します。
 * Controller（入口）と Repository（DB窓口）の間に位置します。
 *
 * 処理の流れ：
 *   HTTP リクエスト → Controller → Service（ここ） → Repository → DB
 *
 * 【UserDetailsService の実装について】
 * Spring Security が「ログイン時にユーザー情報を DB から取得する」ために
 * UserDetailsService インターフェースの実装が必要です。
 * loadUserByUsername メソッドを実装することで、Spring Security がログイン時に
 * このメソッドを自動で呼び出します。
 *
 * @Service : このクラスがサービス層のコンポーネントであることを Spring に伝えます。
 *            @Component の特別版で、Spring がこのクラスを自動検出・管理します。
 */
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * コンストラクタインジェクション（依存性注入）。
     *
     * 【依存性注入（DI）とは？】
     * このクラスが必要とするオブジェクト（userRepository, passwordEncoder）を
     * 自分で new するのではなく、Spring が外から渡してくれる仕組みです。
     * メリット：テストしやすい、クラス間の結合度が下がる
     *
     * コンストラクタが1つだけの場合、@Autowired アノテーションは省略できます。
     */
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Spring Security がログイン時に呼び出すメソッド。
     *
     * メールアドレスをキーに DB からユーザーを検索し、
     * Spring Security が認証に使える形式（CustomUserDetails）に変換して返します。
     *
     * @param email ログインフォームに入力されたメールアドレス
     * @throws UsernameNotFoundException ユーザーが見つからない場合（Spring Security の例外）
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // DB からユーザーを取得。見つからなければ例外を投げる
        // orElseThrow：Optional が空の場合に指定した例外を投げるメソッド
        UserEntity entity = userRepository.findById(email)
                .orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません: " + email));

        // DB のエンティティを Spring Security 用の型に変換して返す
        return new CustomUserDetails(
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.getAllowedRegions()
        );
    }

    /**
     * 新規ユーザーを登録します。
     *
     * 【特別なルール】
     * 最初に登録されたユーザー（DB にまだ誰もいない状態）は自動的に ADMIN になります。
     * 2人目以降は INSPECTOR として登録されます。
     *
     * @param email       登録するメールアドレス
     * @param rawPassword 生のパスワード（ハッシュ化前）
     */
    public void register(String email, String rawPassword) {
        // 同じメールアドレスが既に登録されていないかチェック
        if (userRepository.existsById(email)) {
            throw new IllegalArgumentException("このメールアドレスは既に登録されています");
        }
        // DB にユーザーが0件なら最初のユーザー → ADMIN、それ以外 → INSPECTOR
        String role = userRepository.count() == 0 ? "ADMIN" : "INSPECTOR";
        // パスワードをBCryptでハッシュ化してから保存（生のパスワードは保存しない）
        UserEntity entity = new UserEntity(email, passwordEncoder.encode(rawPassword), role);
        userRepository.save(entity);
        System.out.println("ユーザー登録: " + email + " role=" + role);
    }

    /**
     * 管理者がユーザーのロールと担当リージョンを更新します。
     *
     * @param email          更新対象のメールアドレス
     * @param role           新しいロール（"ADMIN" または "INSPECTOR"）
     * @param allowedRegions アクセスを許可するリージョンIDのリスト
     */
    public void updateUser(String email, String role, List<String> allowedRegions) {
        UserEntity entity = userRepository.findById(email)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません: " + email));
        entity.setRole(role);
        entity.setAllowedRegions(allowedRegions);
        userRepository.save(entity);
        System.out.println("ユーザー更新: " + email + " role=" + role);
    }

    /**
     * 全ユーザーの一覧を取得します（管理画面用）。
     *
     * DB の UserEntity をそのまま返さず、UserInfo という軽量な形式に変換して返します。
     * これにより、パスワードハッシュなどの不要な情報が外部に漏れるのを防ぎます。
     *
     * stream().map().toList() : Java のストリームAPIを使ったコレクション変換。
     *   「全件取得 → 各要素を UserInfo に変換 → リストにまとめる」という処理です。
     */
    public List<UserInfo> findAll() {
        return userRepository.findAll().stream()
                .map(e -> new UserInfo(e.getEmail(), e.getRole(), e.getAllowedRegions()))
                .toList();
    }

    /**
     * 指定したユーザーを削除します（管理者用）。
     *
     * @param email 削除対象のメールアドレス
     */
    public void deleteUser(String email) {
        userRepository.deleteById(email);
        System.out.println("ユーザー削除: " + email);
    }

    /**
     * ユーザー情報の軽量な転送用データクラス（DTO）。
     *
     * 【record とは？】
     * Java 16 以降で使える、シンプルなデータ保持専用クラスです。
     * コンストラクタ・ゲッター・equals・hashCode・toString が自動生成されます。
     * UserEntity 全体を渡す代わりに、必要な情報だけをまとめた形で返します。
     */
    public record UserInfo(String email, String role, List<String> allowedRegions) {}
}
