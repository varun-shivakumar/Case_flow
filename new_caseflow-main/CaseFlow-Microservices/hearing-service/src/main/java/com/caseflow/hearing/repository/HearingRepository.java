package com.caseflow.hearing.repository;
import com.caseflow.hearing.entity.Hearing;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HearingRepository extends JpaRepository<Hearing, Long> {
    List<Hearing> findByCaseId(Long caseId);
    List<Hearing> findByJudgeId(String judgeId);
    List<Hearing> findByStatus(Hearing.HearingStatus status);
}
