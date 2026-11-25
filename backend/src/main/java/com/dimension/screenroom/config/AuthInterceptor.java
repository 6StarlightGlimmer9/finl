package com.dimension.screenroom.config;

import com.dimension.screenroom.domain.AuthToken;
import com.dimension.screenroom.domain.Role;
import com.dimension.screenroom.domain.User;
import com.dimension.screenroom.repository.AuthTokenRepository;
import com.dimension.screenroom.util.PasswordUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final AuthTokenRepository authTokenRepository;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(AuthTokenRepository authTokenRepository, ObjectMapper objectMapper) {
        this.authTokenRepository = authTokenRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublic(path, request.getMethod())) return true;

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            writeError(response, 401, "请先登录");
            return false;
        }

        String tokenHash = PasswordUtil.hashToken(header.substring(7).trim());
        AuthToken token = authTokenRepository.findByTokenHash(tokenHash).orElse(null);
        if (token == null || token.expiresAt.isBefore(Instant.now())) {
            writeError(response, 401, "登录状态已过期，请重新登录");
            return false;
        }

        User user = token.user;
        request.setAttribute("currentUser", user);
        request.setAttribute("authToken", token);

        if (path.startsWith("/api/admin/") && user.role != Role.ADMIN) {
            writeError(response, 403, "需要管理员权限");
            return false;
        }
        return true;
    }

    private boolean isPublic(String path, String method) {
        if (path.equals("/api") || path.equals("/api/")) return true;
        if (path.equals("/api/auth/login") || path.equals("/api/auth/register")) return true;
        if (path.equals("/api/anime") || path.equals("/api/search") || path.equals("/api/community")) return true;
        if ("GET".equals(method) && (path.equals("/api/reviews") || path.equals("/api/bilibili/resolve")
                || path.matches("/api/community/topics/\\d+"))) return true;
        return "GET".equals(method) && (path.matches("/api/anime/\\d+")
                || path.matches("/api/anime/\\d+/rating")
                || path.matches("/api/anime/\\d+/comments"));
    }

    private void writeError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
