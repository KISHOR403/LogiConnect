package com.logiconnect.platform.team.repository;

import com.logiconnect.platform.team.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    Optional<Team> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByDepartmentIdAndName(UUID departmentId, String name);

    @Query("""
            SELECT t FROM Team t JOIN FETCH t.department d
            WHERE (:departmentId IS NULL OR d.id = :departmentId)
            AND (:status IS NULL OR t.status = :status)
            AND (:search IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(t.code) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Team> findAllFiltered(
            @Param("departmentId") UUID departmentId,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.team.id = :teamId AND e.status IN ('ACTIVE', 'PROBATION', 'ON_LEAVE')")
    long countActiveEmployeesByTeamId(@Param("teamId") UUID teamId);
}
