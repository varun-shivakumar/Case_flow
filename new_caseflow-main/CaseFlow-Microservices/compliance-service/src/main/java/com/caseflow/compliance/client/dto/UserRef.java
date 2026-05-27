package com.caseflow.compliance.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRef {
    private String userId;
    private String role;
    private String status;
}
