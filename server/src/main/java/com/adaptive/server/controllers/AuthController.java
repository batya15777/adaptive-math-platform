package com.adaptive.server.controllers;

import com.adaptive.server.DTOs.RegisterRequest;
import com.adaptive.server.responses.BasicResponse;
import com.adaptive.server.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public BasicResponse register(@RequestBody RegisterRequest registerRequest) {
        return authService.register(registerRequest);
    }

//    @PostMapping("/login")


}
