package com.example.mygaeel.controller;

import com.example.mygaeel.service.ElStateService;
import com.example.mygaeel.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理者専用の操作を担当するコントローラー。
 * /admin/** のURLは ADMIN ロールを持つユーザーのみアクセス可（SecurityConfig で設定）。
 *
 * 管理できる操作:
 * - ユーザー一覧の表示
 * - ユーザーのロール・担当リージョンの編集
 * - ユーザーの削除
 */
@Controller
@RequestMapping("/admin") // このクラスのURLはすべて /admin から始まる
public class AdminController {

    private final UserService userService;
    private final ElStateService elStateService;

    // コンストラクタインジェクション（Spring が自動的に依存オブジェクトを渡してくれる）
    public AdminController(UserService userService, ElStateService elStateService) {
        this.userService = userService;
        this.elStateService = elStateService;
    }

    /**
     * GET /admin/users - ユーザー一覧画面を表示する。
     * userService から全ユーザーを取得して、テンプレート "admin/users" に渡す。
     */
    @GetMapping("/users")
    public String userList(Model model) {
        model.addAttribute("users", userService.findAll()); // テンプレートに "users" 変数を渡す
        return "admin/users"; // src/main/resources/templates/admin/users.html を表示
    }

    /**
     * GET /admin/users/{email}/edit - ユーザー編集画面を表示する。
     * @param email パスに含まれるユーザーのメールアドレス
     */
    @GetMapping("/users/{email}/edit")
    public String editUserForm(@PathVariable String email, Model model) {
        List<UserService.UserInfo> all = userService.findAll();

        // メールアドレスが一致するユーザーを検索（見つからなければ例外）
        UserService.UserInfo target = all.stream()
                .filter(u -> u.email().equals(email))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません: " + email));

        model.addAttribute("user", target);
        model.addAttribute("knownRegions", elStateService.getKnownRegionIds()); // 選択肢用のリージョン一覧
        return "admin/user_edit";
    }

    /**
     * POST /admin/users/{email}/edit - ユーザーのロールと担当リージョンを更新する。
     * @param email          更新対象ユーザーのメールアドレス
     * @param role           新しいロール（"ADMIN" or "INSPECTOR"）
     * @param allowedRegions 新しいアクセス許可リージョンのリスト（チェックボックスで選択）
     */
    @PostMapping("/users/{email}/edit")
    public String updateUser(
            @PathVariable String email,
            @RequestParam String role,
            @RequestParam(required = false) List<String> allowedRegions) {

        // チェックボックスが1つも選ばれていない場合、allowedRegions は null になるので空リストに変換
        List<String> regions = allowedRegions != null ? allowedRegions : List.of();
        userService.updateUser(email, role, regions);
        return "redirect:/admin/users?updated=true"; // 更新後は一覧ページにリダイレクト
    }

    /**
     * POST /admin/users/{email}/delete - ユーザーを削除する。
     * @param email 削除対象ユーザーのメールアドレス
     */
    @PostMapping("/users/{email}/delete")
    public String deleteUser(@PathVariable String email) {
        userService.deleteUser(email);
        return "redirect:/admin/users?deleted=true"; // 削除後は一覧ページにリダイレクト
    }
}
