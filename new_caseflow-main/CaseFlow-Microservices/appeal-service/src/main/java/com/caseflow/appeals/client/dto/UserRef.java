package com.caseflow.appeals.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Minimal projection of a User from iam-service — only the fields we need
 * to fan out notifications to all users with a given role.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRef {
    private String userId;
    private String role;
    private String status;
}
