package com.logiconnect.platform.user.repository;

import com.logiconnect.platform.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmployee_EmployeeCode(String employeeCode);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.employee e LEFT JOIN FETCH e.department LEFT JOIN FETCH e.team LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE LOWER(u.email) = LOWER(:identifier) OR LOWER(e.employeeCode) = LOWER(:identifier)")
    Optional<User> findByIdentifierWithDetails(@Param("identifier") String identifier);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.employee e LEFT JOIN FETCH e.department LEFT JOIN FETCH e.team LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE u.id = :id")
    Optional<User> findByIdWithDetails(@Param("id") UUID id);
}
