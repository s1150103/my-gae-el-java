package com.example.mygaeel.service;

import com.google.cloud.datastore.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserService implements UserDetailsService {

    private final Datastore datastore;
    private final PasswordEncoder passwordEncoder;

    public UserService(Datastore datastore, PasswordEncoder passwordEncoder) {
        this.datastore = datastore;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Spring Security がログイン時に呼び出す。email をユーザー名として使用。
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Key key = datastore.newKeyFactory().setKind("User").newKey(email);
        Entity entity = datastore.get(key);

        if (entity == null) {
            throw new UsernameNotFoundException("ユーザーが見つかりません: " + email);
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(entity.getString("email"))
                .password(entity.getString("passwordHash"))
                .authorities(Collections.emptyList())
                .build();
    }

    /**
     * 新規ユーザー登録。email が既に存在する場合は例外を投げる。
     */
    public void register(String email, String rawPassword) {
        Key key = datastore.newKeyFactory().setKind("User").newKey(email);

        if (datastore.get(key) != null) {
            throw new IllegalArgumentException("このメールアドレスは既に登録されています");
        }

        Entity entity = Entity.newBuilder(key)
                .set("email", email)
                .set("passwordHash", passwordEncoder.encode(rawPassword))
                .build();
        datastore.put(entity);
        System.out.println("ユーザー登録: " + email);
    }
}
