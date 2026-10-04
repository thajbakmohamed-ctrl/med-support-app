package com.app.med_support.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    private String passwordResetToken;
    private String newPassword;
}
