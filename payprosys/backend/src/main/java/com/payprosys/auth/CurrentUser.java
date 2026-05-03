package com.payprosys.auth;

import com.payprosys.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Demo: resolve current session from request and enforce roles. */
@Component
@RequiredArgsConstructor
public class CurrentUser {

    public SessionInfo getSession(HttpServletRequest request) {
        Object attr = request.getAttribute(SimpleAuthFilter.SESSION_ATTR);
        return attr instanceof SessionInfo ? (SessionInfo) attr : null;
    }

    public SessionInfo requireSession(HttpServletRequest request) {
        SessionInfo session = getSession(request);
        if (session == null) {
            throw new ForbiddenException("Not authenticated");
        }
        return session;
    }

    /** Throws ForbiddenException if current user has none of the given roles. */
    public SessionInfo requireRole(HttpServletRequest request, String... allowedRoles) {
        SessionInfo session = getSession(request);
        if (session == null) {
            throw new ForbiddenException("Not authenticated");
        }
        List<String> userRoles = session.getRoles();
        for (String role : allowedRoles) {
            if (userRoles.contains(role)) {
                return session;
            }
        }
        throw new ForbiddenException("Insufficient role");
    }
}
