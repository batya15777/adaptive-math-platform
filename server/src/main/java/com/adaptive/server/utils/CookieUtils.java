package com.adaptive.server.utils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class CookieUtils {

    // פונקציה ליצירת עוגיית ההתחברות (HTTP Only)
    public static void setSessionCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("session_token", token);
        cookie.setHttpOnly(true); // חסימה מ-JavaScript
        cookie.setPath("/");
        cookie.setMaxAge(24 * 60 * 60); // תוקף ל-24 שעות
        response.addCookie(cookie);
    }

    // פונקציה למחיקת העוגייה (עבור Logout)
    public static void clearSessionCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("session_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // גיל 0 גורם לדפדפן למחוק את העוגייה מיד
        response.addCookie(cookie);
    }
}
