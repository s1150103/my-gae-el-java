package com.example.mygaeel.service;

import com.example.mygaeel.model.CustomUserDetails;
import com.google.cloud.datastore.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService implements UserDetailsService {

    private final Datastore datastore;
    private final PasswordEncoder passwordEncoder;

    public UserService(Datastore datastore, PasswordEncoder passwordEncoder) {
        this.datastore = datastore;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Key key = datastore.newKeyFactory().setKind("User").newKey(email);
        Entity entity = datastore.get(key);

        if (entity == null) {
            throw new UsernameNotFoundException("ユーザーが見つかりません: " + email);
        }

        String role = entity.contains("role") ? entity.getString("role") : "INSPECTOR";
        List<String> allowedRegions = readRegionList(entity);

        return new CustomUserDetails(
                entity.getString("email"),
                entity.getString("passwordHash"),
                role,
                allowedRegions
        );
    }

    /**
     * 新規ユーザー登録。
     * ユーザーが1人もいない場合は最初のユーザーを ADMIN にする。
     */
    public void register(String email, String rawPassword) {
        Key key = datastore.newKeyFactory().setKind("User").newKey(email);
        if (datastore.get(key) != null) {
            throw new IllegalArgumentException("このメールアドレスは既に登録されています");
        }

        String role = countUsers() == 0 ? "ADMIN" : "INSPECTOR";

        Entity entity = Entity.newBuilder(key)
                .set("email", email)
                .set("passwordHash", passwordEncoder.encode(rawPassword))
                .set("role", role)
                .set("allowedRegions", ListValue.of(List.of()))
                .build();
        datastore.put(entity);
        System.out.println("ユーザー登録: " + email + " role=" + role);
    }

    /**
     * 管理者がユーザーのロールと担当リージョンを更新する。
     */
    public void updateUser(String email, String role, List<String> allowedRegions) {
        Key key = datastore.newKeyFactory().setKind("User").newKey(email);
        Entity existing = datastore.get(key);
        if (existing == null) {
            throw new IllegalArgumentException("ユーザーが見つかりません: " + email);
        }

        List<StringValue> regionValues = allowedRegions.stream()
                .map(StringValue::of)
                .toList();

        Entity updated = Entity.newBuilder(existing)
                .set("role", role)
                .set("allowedRegions", ListValue.of(regionValues))
                .build();
        datastore.put(updated);
        System.out.println("ユーザー更新: " + email + " role=" + role + " regions=" + allowedRegions);
    }

    /**
     * 全ユーザー一覧を取得（管理者用）。
     */
    public List<UserInfo> findAll() {
        Query<Entity> query = Query.newEntityQueryBuilder().setKind("User").build();
        QueryResults<Entity> results = datastore.run(query);

        List<UserInfo> users = new ArrayList<>();
        results.forEachRemaining(entity -> {
            String role = entity.contains("role") ? entity.getString("role") : "INSPECTOR";
            List<String> regions = readRegionList(entity);
            users.add(new UserInfo(entity.getString("email"), role, regions));
        });
        return users;
    }

    /**
     * ユーザーを削除する（管理者用）。
     */
    public void deleteUser(String email) {
        Key key = datastore.newKeyFactory().setKind("User").newKey(email);
        datastore.delete(key);
        System.out.println("ユーザー削除: " + email);
    }

    private long countUsers() {
        Query<Key> query = Query.newKeyQueryBuilder().setKind("User").build();
        QueryResults<Key> results = datastore.run(query);
        long count = 0;
        while (results.hasNext()) { results.next(); count++; }
        return count;
    }

    private List<String> readRegionList(Entity entity) {
        if (!entity.contains("allowedRegions")) return List.of();
        List<Value<?>> values = entity.getList("allowedRegions");
        return values.stream()
                .map(v -> (String) v.get())
                .toList();
    }

    /** ユーザー情報の表示用DTO */
    public record UserInfo(String email, String role, List<String> allowedRegions) {}
}
