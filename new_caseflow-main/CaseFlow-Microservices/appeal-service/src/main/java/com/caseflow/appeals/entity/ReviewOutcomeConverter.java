package com.caseflow.appeals.entity;

import com.caseflow.appeals.entity.Review.ReviewOutcome;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Maps legacy ReviewOutcome strings still in the DB to the current enum values.
 * Without this, Hibernate throws JpaSystemException on any row with an old value.
 */
@Converter
@Slf4j
public class ReviewOutcomeConverter implements AttributeConverter<ReviewOutcome, String> {

    private static final Map<String, ReviewOutcome> LEGACY = Map.of(
        "APPEAL_UPHELD",    ReviewOutcome.APPROVED,
        "PARTIALLY_UPHELD", ReviewOutcome.APPROVED,
        "RETRIAL_ORDERED",  ReviewOutcome.APPROVED,
        "APPEAL_DISMISSED", ReviewOutcome.REJECTED,
        "REMANDED",         ReviewOutcome.REJECTED
    );

    @Override
    public String convertToDatabaseColumn(ReviewOutcome attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public ReviewOutcome convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            return ReviewOutcome.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            ReviewOutcome mapped = LEGACY.get(dbData);
            if (mapped != null) {
                log.warn("Legacy ReviewOutcome '{}' mapped to '{}' — consider a DB migration", dbData, mapped);
                return mapped;
            }
            log.error("Unknown ReviewOutcome '{}' in database — treating as null", dbData);
            return null;
        }
    }
}
