package com.adaptive.server.controllers;

import com.adaptive.server.DTOs.ForgotPasswordRequest;
import com.adaptive.server.DTOs.LoginRequest;
import com.adaptive.server.DTOs.LoginSuccessData;
import com.adaptive.server.DTOs.RegisterRequest;
import com.adaptive.server.DTOs.ResetPasswordRequest;
import com.adaptive.server.DTOs.UserResponseDTO;
import com.adaptive.server.DTOs.VerifyEmailRequest;
import com.adaptive.server.entity.SessionToken;
import com.adaptive.server.responses.BasicResponse;
import com.adaptive.server.responses.LoginResponse;
import com.adaptive.server.service.AuthService;
import com.adaptive.server.service.SessionValidationService;
import com.adaptive.server.utils.CookieUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final SessionValidationService sessionValidationService;

    public AuthController(AuthService authService, SessionValidationService sessionValidationService) {
        this.authService = authService;
        this.sessionValidationService = sessionValidationService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest loginRequest
            , HttpServletRequest request, HttpServletResponse response){

        LoginResponse responseData = authService.login(loginRequest);
        if (responseData.isSuccess() && responseData.getLoginData() != null) {
            // "Remember me" controls whether the cookie persists across browser restarts.
            // secure flag follows the request scheme: http (dev) → false so the cookie is kept;
            // https (production) → true.
            CookieUtils.setSessionCookie(response , responseData.getLoginData().getToken(), loginRequest.isRemember(), request.isSecure());
            //אם התחברתי בהצלחה מגידירם את HTTP ONLY וסידור עוגייה
            stripTokensFormatBody(responseData.getLoginData());
        }
        return responseData;
    }

    @PostMapping("/forgot-password")
    public BasicResponse forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request.getEmail());
    }

    @PostMapping("/reset-password")
    public BasicResponse resetPassword(@RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request.getEmail(), request.getCode(), request.getPassword());
    }

    //זה פונקציית עזר שמוחקת לי את הטוקן מJSON
    private void stripTokensFormatBody(LoginSuccessData data) {
        data.setToken(null);
    }

    @PostMapping("/logout")
    public BasicResponse logout(@CookieValue(value = "session_token" , required = false)
             String token , HttpServletRequest request, HttpServletResponse response){

        authService.logout(token);//מחיקת טוקן מDB
        CookieUtils.clearSessionCookie(response, request.isSecure());
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

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> me(
            @CookieValue(value = "session_token", required = false) String sessionToken) {
        SessionToken token = sessionValidationService.validateAndGetUser(sessionToken);
        return ResponseEntity.ok(new UserResponseDTO(token.getUser()));
    }

    @PostMapping("/survey-complete")
    public ResponseEntity<BasicResponse> surveyComplete(
            @CookieValue(value = "session_token", required = false) String sessionToken) {
        SessionToken token = sessionValidationService.validateAndGetUser(sessionToken);
        authService.markSurveyComplete(token.getUser().getId());
        return ResponseEntity.ok(new BasicResponse(true, "Survey marked complete."));
    }

}
