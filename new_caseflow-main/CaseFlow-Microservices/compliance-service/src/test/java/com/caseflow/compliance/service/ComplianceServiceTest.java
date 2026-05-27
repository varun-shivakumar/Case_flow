package com.caseflow.compliance.service;

import com.caseflow.compliance.entity.*;
import com.caseflow.compliance.exception.*;
import com.caseflow.compliance.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplianceServiceTest {
    @Mock private ComplianceRecordRepository complianceRecordRepository;
    @Mock private AuditRepository auditRepository;
    @InjectMocks private ComplianceServiceImpl complianceService;

    @Test void getAuditById_notFound_throws() {
        when(auditRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> complianceService.getAuditById(99L));
    }

    @Test void getComplianceByCase_empty() {
        when(complianceRecordRepository.findByCaseId(1L)).thenReturn(List.of());
        assertEquals(0, complianceService.getComplianceRecordsByCase(1L).size());
    }
}
