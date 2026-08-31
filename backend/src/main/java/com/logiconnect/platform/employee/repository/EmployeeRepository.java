package com.logiconnect.platform.employee.repository;

import com.logiconnect.platform.employee.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByEmployeeCode(String employeeCode);

    Optional<Employee> findByEmail(String email);

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmail(String email);

    @Query("""
            SELECT e FROM Employee e
            LEFT JOIN FETCH e.department d
            LEFT JOIN FETCH e.team t
            LEFT JOIN FETCH e.manager m
            WHERE e.id = :id
            """)
    Optional<Employee> findByIdWithDetails(@Param("id") UUID id);

    @Query("""
            SELECT e FROM Employee e
            LEFT JOIN e.department d
            LEFT JOIN e.team t
            WHERE (:departmentId IS NULL OR d.id = :departmentId)
            AND (:teamId IS NULL OR t.id = :teamId)
            AND (:status IS NULL OR e.status = :status)
            AND (:location IS NULL OR LOWER(e.location) LIKE LOWER(CONCAT('%', :location, '%')))
            AND (:search IS NULL
                 OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(e.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(e.designation) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Employee> findAllFiltered(
            @Param("departmentId") UUID departmentId,
            @Param("teamId") UUID teamId,
            @Param("status") com.logiconnect.platform.employee.entity.EmployeeStatus status,
            @Param("location") String location,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            SELECT e FROM Employee e
            LEFT JOIN e.department d
            WHERE d.id = :departmentId
            AND (:status IS NULL OR e.status = :status)
            """)
    Page<Employee> findByDepartmentId(
            @Param("departmentId") UUID departmentId,
            @Param("status") com.logiconnect.platform.employee.entity.EmployeeStatus status,
            Pageable pageable
    );

    @Query("""
            SELECT e FROM Employee e
            LEFT JOIN e.team t
            WHERE t.id = :teamId
            AND (:status IS NULL OR e.status = :status)
            """)
    Page<Employee> findByTeamId(
            @Param("teamId") UUID teamId,
            @Param("status") com.logiconnect.platform.employee.entity.EmployeeStatus status,
            Pageable pageable
    );
}
