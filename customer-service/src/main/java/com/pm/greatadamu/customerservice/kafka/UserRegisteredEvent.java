package com.pm.greatadamu.customerservice.kafka;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UserRegisteredEvent {
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;
    private String gender;

}
