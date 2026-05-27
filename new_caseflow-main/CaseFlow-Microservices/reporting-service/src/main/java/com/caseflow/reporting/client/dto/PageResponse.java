package com.caseflow.reporting.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Minimal Page representation for deserializing Spring Data Page<T> responses
 * across service boundaries via Feign. Only the fields actually used by
 * reporting are mapped.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageResponse<T> {
    private List<T> content;
    private int totalElements;
    private int totalPages;
    private int number;
    private int size;
    private boolean first;
    private boolean last;
    private boolean empty;
}
