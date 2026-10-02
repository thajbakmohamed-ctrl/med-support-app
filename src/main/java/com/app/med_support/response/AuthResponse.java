package com.app.med_support.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// if the token null dont show it in the ver link throw the email
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private String message;
    private String token;
    public AuthResponse(String message) {
        this.message = message;
        this.token = null;
    }
}
