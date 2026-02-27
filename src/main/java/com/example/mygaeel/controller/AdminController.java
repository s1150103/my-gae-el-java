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
 * ■ 管理者画面コントローラー
 *
 * ADMIN ロールを持つユーザーだけがアクセスできるページを管理します。
 * （SecurityConfig で /admin/** は hasRole("ADMIN") に制限済み）
 *
 * 主な機能：
 *   - ユーザー一覧の表示
 *   - ユーザーのロール・担当リージョンの変更
 *   - ユーザーの削除
 *
 * @RequestMapping("/admin") : このクラスのすべてのメソッドの URL が "/admin" から始まります
 *   例：@GetMapping("/users") → 実際の URL は /admin/users
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final ElStateService elStateService;

    public AdminController(UserService userService, ElStateService elStateService) {
        this.userService = userService;
        this.elStateService = elStateService;
    }

    /**
     * ユーザー一覧ページを表示します（GET /admin/users）。
     *
     * @param model テンプレートに渡すデータ
     * @return templates/admin/users.html を表示
     */
    @GetMapping("/users")
    public String userList(Model model) {
        // 全ユーザーを取得して HTML に渡す
        model.addAttribute("users", userService.findAll());
        return "admin/users";
    }

    /**
     * ユーザー編集画面を表示します（GET /admin/users/{email}/edit）。
     *
     * @PathVariable : URL の {email} 部分を変数として受け取ります
     *   例：/admin/users/tanaka@example.com/edit → email = "tanaka@example.com"
     *
     * @param email 編集対象ユーザーのメールアドレス
     * @param model テンプレートに渡すデータ
     */
    @GetMapping("/users/{email}/edit")
    public String editUserForm(@PathVariable String email, Model model) {
        List<UserService.UserInfo> all = userService.findAll();
        // 指定のメールアドレスに一致するユーザーを検索
        // stream().filter().findFirst() = リストから条件に合う最初の1件を取得
        UserService.UserInfo target = all.stream()
                .filter(u -> u.email().equals(email))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません: " + email));

        model.addAttribute("user", target);
        // リージョン選択のチェックボックス用に、既知のリージョン一覧を渡す
        model.addAttribute("knownRegions", elStateService.getKnownRegionIds());
        return "admin/user_edit";
    }

    /**
     * ユーザー情報を更新します（POST /admin/users/{email}/edit）。
     *
     * @param email          更新対象ユーザーのメールアドレス（URLから取得）
     * @param role           選択されたロール（フォームから取得）
     * @param allowedRegions 許可するリージョンのリスト（チェックボックスから取得）
     *                       required = false: チェックボックスが全てオフの場合は null になるため省略可能
     */
    @PostMapping("/users/{email}/edit")
    public String updateUser(
            @PathVariable String email,
            @RequestParam String role,
            @RequestParam(required = false) List<String> allowedRegions) {

        // allowedRegions が null（チェックなし）の場合は空リストで代替
        List<String> regions = allowedRegions != null ? allowedRegions : List.of();
        userService.updateUser(email, role, regions);
        // 更新成功後、ユーザー一覧ページにリダイレクト（"?updated=true" は成功メッセージ表示用）
        return "redirect:/admin/users?updated=true";
    }

    /**
     * ユーザーを削除します（POST /admin/users/{email}/delete）。
     *
     * 削除操作は GET ではなく POST で実装しています。
     * （GET だとリンクを踏むだけで削除されてしまう危険があるため）
     *
     * @param email 削除対象ユーザーのメールアドレス
     */
    @PostMapping("/users/{email}/delete")
    public String deleteUser(@PathVariable String email) {
        userService.deleteUser(email);
        return "redirect:/admin/users?deleted=true";
    }
}
