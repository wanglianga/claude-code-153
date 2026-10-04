package com.waste.auth;

import com.waste.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证", description = "登录获取令牌，后续请求头携带 Authorization: Bearer {token}")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    public record LoginRequest(@NotBlank(message = "用户名不能为空") String username,
                               @NotBlank(message = "密码不能为空") String password) {
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody @jakarta.validation.Valid LoginRequest req) {
        return ApiResponse.ok(authService.login(req.username(), req.password()));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authorization) {
        authService.logout(authorization.replace("Bearer ", ""));
        return ApiResponse.ok();
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public ApiResponse<Session> me() {
        return ApiResponse.ok(AuthService.current());
    }
}
