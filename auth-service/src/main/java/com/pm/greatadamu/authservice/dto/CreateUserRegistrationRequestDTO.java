package com.pm.greatadamu.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
/// NEW USER SIGNUP
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRegistrationRequestDTO {
    @Email
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8 , max = 25, message ="Password must be between 8 and 25 characters")
    private String password; //->RAW password

    @NotBlank(message = "Password must match")
    @Size(min = 8 , max = 25, message ="Password must be between 8 and 25 characters")
    private String confirmPassword;

   // Customer Profile Data:
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "last name is required")
    private String lastName;

    @NotBlank(message = "Phone number is required")
   private String phoneNumber;

    @NotBlank(message = "Address is required")
   private String address;

    @NotBlank(message = "Must be MALE/FEMALE/OTHER")
    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "Gender must be MALE, FEMALE, or OTHER")
    private String gender;


}
