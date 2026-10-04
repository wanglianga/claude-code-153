package com.waste.auth;

import com.waste.common.BizException;
import com.waste.entity.User;
import com.waste.enums.Role;
import com.waste.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 简单令牌认证（演示系统：令牌存内存，重启后需重新登录） */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final Map<String, Session> tokens = new ConcurrentHashMap<>();
    private static final ThreadLocal<Session> CURRENT = new ThreadLocal<>();

    public Map<String, Object> login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException(401, "用户名或密码错误"));
        if (!user.getPassword().equals(password)) {
            throw new BizException(401, "用户名或密码错误");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        Session session = new Session(user.getId(), user.getUsername(), user.getName(), user.getRole(), user.getTeamName());
        tokens.put(token, session);
        return Map.of("token", token, "user", session);
    }

    public void logout(String token) {
        if (token != null) {
            tokens.remove(token);
        }
    }

    public Session parse(String token) {
        return token == null ? null : tokens.get(token);
    }

    public static void set(Session session) {
        CURRENT.set(session);
    }

    public static void clear() {
        CURRENT.remove();
    }

    /** 当前登录用户，未登录抛 401 */
    public static Session current() {
        Session s = CURRENT.get();
        if (s == null) {
            throw new BizException(401, "未登录或登录已过期");
        }
        return s;
    }

    /** 要求当前用户具备指定角色之一 */
    public static Session require(Role... roles) {
        Session s = current();
        for (Role r : roles) {
            if (s.role() == r || s.role() == Role.ADMIN) {
                return s;
            }
        }
        throw new BizException(403, "当前角色无权执行该操作");
    }
}
