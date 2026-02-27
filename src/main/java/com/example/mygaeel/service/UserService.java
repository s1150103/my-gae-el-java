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

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity entity = userRepository.findById(email)
                .orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません: " + email));

        return new CustomUserDetails(
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.getAllowedRegions()
        );
    }

    /**
     * 新規ユーザー登録。最初のユーザーは ADMIN になる。
     */
    public void register(String email, String rawPassword) {
        if (userRepository.existsById(email)) {
            throw new IllegalArgumentException("このメールアドレスは既に登録されています");
        }
        String role = userRepository.count() == 0 ? "ADMIN" : "INSPECTOR";
        UserEntity entity = new UserEntity(email, passwordEncoder.encode(rawPassword), role);
        userRepository.save(entity);
        System.out.println("ユーザー登録: " + email + " role=" + role);
    }

    /**
     * 管理者がユーザーのロールと担当リージョンを更新する。
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
     * 全ユーザー一覧を取得（管理者用）。
     */
    public List<UserInfo> findAll() {
        return userRepository.findAll().stream()
                .map(e -> new UserInfo(e.getEmail(), e.getRole(), e.getAllowedRegions()))
                .toList();
    }

    /**
     * ユーザーを削除する（管理者用）。
     */
    public void deleteUser(String email) {
        userRepository.deleteById(email);
        System.out.println("ユーザー削除: " + email);
    }

    public record UserInfo(String email, String role, List<String> allowedRegions) {}
}
