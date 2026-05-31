package com.adaptive.server.controllers;

import com.adaptive.server.DTOs.LoginRequest;
import com.adaptive.server.DTOs.LoginSuccessData;
import com.adaptive.server.responses.BasicResponse;
import com.adaptive.server.service.AuthService;
import com.adaptive.server.utils.CookieUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public BasicResponse<LoginSuccessData> login(@RequestBody LoginRequest loginRequest
            , HttpServletResponse response){

        BasicResponse<LoginSuccessData> responseData = authService.login(loginRequest);
        if (responseData.isSuccess() && responseData.getData() != null) {
            CookieUtils.setSessionCookie(response , responseData.getData().getToken());
            //אם התחברתי בהצלחה מגידירם את HTTP ONLY וסידור עוגייה
            stripTokensFormatBody(responseData.getData());
        }
        return responseData;
    }

    //זה פונקציית עזר שמוחקת לי את הטוקן מJSON
    private void stripTokensFormatBody(LoginSuccessData data) {
        data.setToken(null);
    }

    @PostMapping("/logout")
    public BasicResponse<String> logout(@CookieValue(value = "session_token" , required = false)
             String token , HttpServletResponse response){

        authService.logout(token);//מחיקת טוקן מDB
        CookieUtils.clearSessionCookie(response);
        return new BasicResponse<>("Logged out successfully");
    }


//    @PostMapping("/register")

}
