package com.caseflow.hearing.dto;
import com.caseflow.hearing.entity.Hearing;
import lombok.Data;
import java.time.LocalDate;

@Data
public class HearingResponse {
    private Long hearingId; private Long caseId; private String judgeId;
    private LocalDate hearingDate; private String hearingTime;
    private Hearing.HearingStatus status; private String scheduledBy;
    private String rescheduleReason; private String hearingNotes;
}
