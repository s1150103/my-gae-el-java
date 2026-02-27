package com.example.mygaeel.controller;

import com.example.mygaeel.service.ElStateService;
import com.example.mygaeel.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final ElStateService elStateService;

    public AdminController(UserService userService, ElStateService elStateService) {
        this.userService = userService;
        this.elStateService = elStateService;
    }

    /** ユーザー一覧 */
    @GetMapping("/users")
    public String userList(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/users";
    }

    /** ユーザー編集画面 */
    @GetMapping("/users/{email}/edit")
    public String editUserForm(@PathVariable String email, Model model) {
        List<UserService.UserInfo> all = userService.findAll();
        UserService.UserInfo target = all.stream()
                .filter(u -> u.email().equals(email))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません: " + email));

        model.addAttribute("user", target);
        model.addAttribute("knownRegions", elStateService.getKnownRegionIds());
        return "admin/user_edit";
    }

    /** ユーザー更新 */
    @PostMapping("/users/{email}/edit")
    public String updateUser(
            @PathVariable String email,
            @RequestParam String role,
            @RequestParam(required = false) List<String> allowedRegions) {

        List<String> regions = allowedRegions != null ? allowedRegions : List.of();
        userService.updateUser(email, role, regions);
        return "redirect:/admin/users?updated=true";
    }

    /** ユーザー削除 */
    @PostMapping("/users/{email}/delete")
    public String deleteUser(@PathVariable String email) {
        userService.deleteUser(email);
        return "redirect:/admin/users?deleted=true";
    }
}
