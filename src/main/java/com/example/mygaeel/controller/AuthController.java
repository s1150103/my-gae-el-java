package com.example.mygaeel.controller;

import com.example.mygaeel.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * ■ 認証（ログイン・ユーザー登録）コントローラー
 *
 * 【Controller とは？】
 * HTTP リクエストの「受付窓口」です。
 * ユーザーがブラウザで URL にアクセスすると、対応するコントローラーのメソッドが呼ばれます。
 *
 * 【@Controller と @RestController の違い】
 * @Controller    : HTML テンプレート（Thymeleaf）を返す場合に使う
 * @RestController: JSON を返す場合に使う（API 用）
 *
 * このクラスは画面（HTML）を返すので @Controller を使っています。
 *
 * 【Thymeleaf とは？】
 * Java のテンプレートエンジンです。
 * HTML ファイルの中に変数（${errorMessage} など）を埋め込んで、
 * サーバー側で値を差し込んでからブラウザに返します。
 */
@Controller
public class AuthController {

    private final UserService userService;

    /** コンストラクタインジェクション */
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * ログインページを表示します（GET /login）。
     *
     * @GetMapping : GET リクエストを受け取るメソッドであることを示します
     *
     * @param error  ログイン失敗時に "error" というパラメータが付与されます（?error=true）
     * @param logout ログアウト後に "logout" というパラメータが付与されます（?logout=true）
     * @param model  テンプレートに渡すデータを格納するオブジェクト
     *               model.addAttribute("key", value) で HTML 側から ${key} で参照できます
     * @return "login" → templates/login.html を表示
     */
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            Model model) {

        if (error != null) {
            // ?error が URL についていれば、エラーメッセージを HTML に渡す
            model.addAttribute("errorMessage", "メールアドレスまたはパスワードが違います");
        }
        if (logout != null) {
            // ?logout が URL についていれば、ログアウトメッセージを HTML に渡す
            model.addAttribute("logoutMessage", "ログアウトしました");
        }
        return "login";  // templates/login.html を表示
    }

    /**
     * ユーザー登録ページを表示します（GET /register）。
     *
     * @return "register" → templates/register.html を表示
     */
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    /**
     * ユーザー登録フォームの送信を処理します（POST /register）。
     *
     * @PostMapping : POST リクエストを受け取るメソッドであることを示します
     *
     * @param email    フォームに入力されたメールアドレス
     * @param password フォームに入力されたパスワード
     * @param model    エラーメッセージをテンプレートに渡す用
     * @return 成功時：ログインページにリダイレクト（"redirect:/login?registered=true"）
     *         失敗時：登録ページに戻ってエラーを表示
     */
    @PostMapping("/register")
    public String register(
            @RequestParam String email,
            @RequestParam String password,
            Model model) {
        try {
            userService.register(email, password);
            // "redirect:" を付けると、指定 URL にリダイレクト（HTTP 302）します
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException e) {
            // 同じメールが既に登録済みなど、バリデーションエラーの場合
            model.addAttribute("errorMessage", e.getMessage());
            return "register";  // 登録ページに留まってエラーを表示
        }
    }
}
