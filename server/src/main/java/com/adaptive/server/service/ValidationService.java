package com.adaptive.server.service;

import org.springframework.stereotype.Service;

@Service
public class ValidationService {

    public boolean usernameRegex(String username ){

        return username.matches("^[A-Za-z]{2,10}$");
    }

    public boolean passwordRegex(String password ){

        return password.matches("^[A-Za-z!0-9@#*]{2,10}$" );
    }

    public boolean emailRegex(String email){

        return email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\\\.[A-Za-z]{2,}$");
    }
    public boolean isGender(String gender){
        return gender.isEmpty();
    }

}