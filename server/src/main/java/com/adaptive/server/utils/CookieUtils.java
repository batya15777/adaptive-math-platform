package com.adaptive.server.utils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class CookieUtils {

    private static final int REMEMBER_ME_MAX_AGE = 30 * 24 * 60 * 60; // 30 ימים

    // פונקציה ליצירת עוגיית ההתחברות (HTTP Only)
    // rememberMe=true → עוגייה מתמשכת ל-30 יום; rememberMe=false → עוגיית סשן
    // (maxAge=-1) שנמחקת כשסוגרים את הדפדפן.
    public static void setSessionCookie(HttpServletResponse response, String token, boolean rememberMe) {
        Cookie cookie = new Cookie("session_token", token);
        cookie.setHttpOnly(true); // חסימה מ-JavaScript
        cookie.setPath("/");
        cookie.setSecure(true);
        cookie.setMaxAge(rememberMe ? REMEMBER_ME_MAX_AGE : -1);
        response.addCookie(cookie);
    }

    // פונקציה למחיקת העוגייה (עבור Logout)
    public static void clearSessionCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("session_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setSecure(true);
        cookie.setMaxAge(0); // גיל 0 גורם לדפדפן למחוק את העוגייה מיד
        response.addCookie(cookie);
    }
}
