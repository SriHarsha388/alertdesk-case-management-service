package com.alertdesk.case_management.repository;

import com.alertdesk.case_management.domain.CaseEntity;
import com.alertdesk.case_management.domain.CasePriority;
import com.alertdesk.case_management.domain.CaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CaseRepository extends JpaRepository<CaseEntity, String> {

    List<CaseEntity> findByStatusAndPriority(CaseStatus status, CasePriority priority);

    List<CaseEntity> findByStatus(CaseStatus status);

    List<CaseEntity> findByPriority(CasePriority priority);

    Optional<CaseEntity> findByCaseId(String caseId);

    boolean existsByCaseId(String caseId);
}
