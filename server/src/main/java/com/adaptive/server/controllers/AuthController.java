package com.adaptive.server.controllers;

import com.adaptive.server.DTOs.LoginRequest;
import com.adaptive.server.DTOs.LoginSuccessData;
import com.adaptive.server.DTOs.RegisterRequest;
import com.adaptive.server.DTOs.VerifyEmailRequest;
import com.adaptive.server.responses.BasicResponse;
import com.adaptive.server.responses.LoginResponse;
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
    public LoginResponse login(@RequestBody LoginRequest loginRequest
            , HttpServletResponse response){

        LoginResponse responseData = authService.login(loginRequest);
        if (responseData.isSuccess() && responseData.getLoginData() != null) {
            CookieUtils.setSessionCookie(response , responseData.getLoginData().getToken());
            //אם התחברתי בהצלחה מגידירם את HTTP ONLY וסידור עוגייה
            stripTokensFormatBody(responseData.getLoginData());
        }
        return responseData;
    }

    //זה פונקציית עזר שמוחקת לי את הטוקן מJSON
    private void stripTokensFormatBody(LoginSuccessData data) {
        data.setToken(null);
    }

    @PostMapping("/logout")
    public BasicResponse logout(@CookieValue(value = "session_token" , required = false)
             String token , HttpServletResponse response){

        authService.logout(token);//מחיקת טוקן מDB
        CookieUtils.clearSessionCookie(response);
        return new BasicResponse(true , "Logged out successfully");
    }


    @PostMapping("/register")
    public BasicResponse register(@RequestBody RegisterRequest registerRequest) {
        return authService.register(registerRequest);
    }

    @PostMapping("/verify")
    public BasicResponse verify(@RequestBody VerifyEmailRequest verifyEmailRequest) {
        return authService.verify(verifyEmailRequest);
    }

//    @PostMapping("/login")


}
