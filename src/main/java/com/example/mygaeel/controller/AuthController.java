package com.example.mygaeel.controller;

import com.example.mygaeel.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 認証（ログイン・ユーザー登録）を担当するコントローラー。
 * ログイン処理自体は Spring Security が行い、このクラスは画面表示と登録処理のみを担当する。
 */
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /login - ログインページを表示する。
     * URLパラメータに "error" があればエラーメッセージを、
     * "logout" があればログアウト完了メッセージをテンプレートに渡す。
     */
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,   // ログイン失敗時に Spring Security が付与
            @RequestParam(required = false) String logout,  // ログアウト後に付与
            Model model) {

        if (error != null) {
            model.addAttribute("errorMessage", "メールアドレスまたはパスワードが違います");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "ログアウトしました");
        }
        return "login"; // src/main/resources/templates/login.html を表示
    }

    /**
     * GET /register - ユーザー登録画面を表示する。
     */
    @GetMapping("/register")
    public String registerPage() {
        return "register"; // src/main/resources/templates/register.html を表示
    }

    /**
     * POST /register - 新規ユーザーを登録する。
     * 登録成功時はログインページへ、失敗時は登録ページにエラーを表示する。
     *
     * @param email    登録するメールアドレス
     * @param password 登録するパスワード（平文）※サービス内でハッシュ化される
     */
    @PostMapping("/register")
    public String register(
            @RequestParam String email,
            @RequestParam String password,
            Model model) {
        try {
            userService.register(email, password);
            return "redirect:/login?registered=true"; // 登録成功 → ログインページへ
        } catch (IllegalArgumentException e) {
            // メールアドレスが既に使われている場合などはエラーメッセージを表示
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        }
    }
}
