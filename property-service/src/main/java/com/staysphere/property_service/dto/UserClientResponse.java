package com.staysphere.property_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserClientResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
}