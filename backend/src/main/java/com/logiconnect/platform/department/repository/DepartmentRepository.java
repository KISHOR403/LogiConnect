package com.logiconnect.platform.department.repository;

import com.logiconnect.platform.department.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    Optional<Department> findByCode(String code);

    Optional<Department> findByName(String name);

    boolean existsByCode(String code);

    boolean existsByName(String name);

    @Query("""
            SELECT d FROM Department d
            WHERE (:status IS NULL OR d.status = :status)
            AND (:search IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(d.code) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(d.description) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Department> findAllFiltered(
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT COUNT(t) FROM Team t WHERE t.department.id = :departmentId AND t.status = 'ACTIVE'")
    long countActiveTeamsByDepartmentId(@Param("departmentId") UUID departmentId);

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.department.id = :departmentId AND e.status IN ('ACTIVE', 'PROBATION', 'ON_LEAVE')")
    long countActiveEmployeesByDepartmentId(@Param("departmentId") UUID departmentId);
}
