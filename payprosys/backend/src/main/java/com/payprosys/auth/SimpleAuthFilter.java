package com.payprosys.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payprosys.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/** Demo: require Bearer token for /api/** except /api/auth/login and /api/auth/activate. Sets request attribute "session". */
@RequiredArgsConstructor
public class SimpleAuthFilter extends OncePerRequestFilter {

    public static final String SESSION_ATTR = "session";

    private final SessionStore sessionStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = normalizePath(request.getRequestURI());
        if (path != null && (path.equals("/api/auth/login") || path.equals("/api/auth/activate"))) {
            filterChain.doFilter(request, response);
            return;
        }
        if (path == null || !path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
        }
        if (token == null || token.isEmpty()) {
            sendUnauthorized(response, "Missing or invalid Authorization header");
            return;
        }

        Optional<SessionInfo> session = sessionStore.get(token);
        if (session.isEmpty()) {
            sendUnauthorized(response, "Invalid or expired token");
            return;
        }

        request.setAttribute(SESSION_ATTR, session.get());
        filterChain.doFilter(request, response);
    }

    /** Strip query string, trailing slashes, and duplicate slashes so public auth paths match reliably. */
    private static String normalizePath(String raw) {
        if (raw == null) {
            return null;
        }
        String p = raw;
        int q = p.indexOf('?');
        if (q >= 0) {
            p = p.substring(0, q);
        }
        while (p.contains("//")) {
            p = p.replace("//", "/");
        }
        while (p.length() > 1 && p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(message)));
    }
}
