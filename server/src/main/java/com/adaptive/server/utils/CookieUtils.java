package com.adaptive.server.utils;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import javax.servlet.http.HttpServletResponse;

public class CookieUtils {

    private static final long REMEMBER_ME_MAX_AGE = 30L * 24 * 60 * 60; // 30 ימים (שניות)

    // עוגיית ההתחברות (HttpOnly). rememberMe=true → מתמשכת ל-30 יום; false → עוגיית סשן.
    // secure: ב-HTTPS (פרודקשן) true, ב-http מקומי false — אחרת הדפדפן זורק את העוגייה.
    // SameSite: בפרודקשן ה-client וה-API על דומיינים שונים (cross-site), ולכן צריך
    // SameSite=None;Secure כדי שהעוגייה תישמר ותישלח. בפיתוח (http, same-site) משתמשים ב-Lax,
    // כי None דורש Secure ולא יעבוד מעל http.
    public static void setSessionCookie(HttpServletResponse response, String token, boolean rememberMe, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from("session_token", token)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(rememberMe ? REMEMBER_ME_MAX_AGE : -1)
                .sameSite(secure ? "None" : "Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // מחיקת העוגייה (Logout) — אותן תכונות + maxAge=0 כדי שהדפדפן ימחק אותה מיד.
    public static void clearSessionCookie(HttpServletResponse response, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from("session_token", "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(0)
                .sameSite(secure ? "None" : "Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
