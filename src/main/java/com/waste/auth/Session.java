package com.waste.auth;

import com.waste.enums.Role;

/** 登录会话 */
public record Session(Long userId, String username, String name, Role role, String teamName) {
}
