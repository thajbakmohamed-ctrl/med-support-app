package com.app.med_support.request;

import com.app.med_support.model.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    private String email;
    private String password;
}

