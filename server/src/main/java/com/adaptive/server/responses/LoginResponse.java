package com.adaptive.server.responses;

import com.adaptive.server.DTOs.LoginSuccessData;

public class LoginResponse extends BasicResponse{

    private LoginSuccessData loginData;

    public LoginResponse (){
        super();
    }

    public LoginResponse (boolean success , String message , LoginSuccessData loginData){
        super(success, message);
        this.loginData = loginData;
    }

    public LoginSuccessData getLoginData() {
        return loginData;
    }

    public void setLoginData(LoginSuccessData loginData) {
        this.loginData = loginData;
    }
}
