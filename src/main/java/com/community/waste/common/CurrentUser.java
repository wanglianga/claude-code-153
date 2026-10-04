package com.community.waste.common;

import com.community.waste.domain.DomainEnums.Role;

/** 当前请求角色（由鉴权过滤器写入） */
public final class CurrentUser {
    private static final ThreadLocal<Role> ROLE = new ThreadLocal<>();
    private static final ThreadLocal<String> TOKEN = new ThreadLocal<>();

    private CurrentUser() {}

    public static void set(Role role, String token) {
        ROLE.set(role);
        TOKEN.set(token);
    }

    public static Role role() {
        Role r = ROLE.get();
        if (r == null) {
            throw new BusinessException("未认证：缺少 X-Auth-Token");
        }
        return r;
    }

    public static boolean hasRole(Role role) {
        return ROLE.get() == role;
    }

    public static void require(Role role) {
        if (ROLE.get() != role) {
            throw new BusinessException("无权操作：需要角色 " + role.label + "，当前 "
                    + (ROLE.get() == null ? "未认证" : ROLE.get().label));
        }
    }

    public static void requireAny(Role... roles) {
        Role current = ROLE.get();
        for (Role r : roles) {
            if (current == r) return;
        }
        throw new BusinessException("无权操作：当前角色 " + (current == null ? "未认证" : current.label));
    }

    public static void clear() {
        ROLE.remove();
        TOKEN.remove();
    }
}
