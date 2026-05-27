package com.caseflow.appeals.repository;

import com.caseflow.appeals.entity.Review;
import com.caseflow.appeals.entity.Review.ReviewOutcome;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByAppealId(Long appealId);
    List<Review> findByCaseId(Long caseId);
    List<Review> findByJudgeId(String judgeId);

    Page<Review> findByCaseId(Long caseId, Pageable pageable);
    Page<Review> findByJudgeId(String judgeId, Pageable pageable);

    long countByOutcome(ReviewOutcome outcome);

    /**
     * Conflict-of-interest check: a judge who reviewed an appeal that was later
     * CANCELLED (and thus never decided) can be reassigned to a new appeal on
     * the same case. Only ACTIVE / DECIDED reviews block reassignment.
     */
    @Query("""
        select case when count(r) > 0 then true else false end
          from Review r
          join Appeal a on a.appealId = r.appealId
         where r.caseId   = :caseId
           and r.judgeId  = :judgeId
           and a.status  <> com.caseflow.appeals.entity.Appeal$AppealStatus.CANCELLED
        """)
    boolean existsActiveAssignmentForJudge(@Param("caseId") Long caseId,
                                           @Param("judgeId") String judgeId);
}
