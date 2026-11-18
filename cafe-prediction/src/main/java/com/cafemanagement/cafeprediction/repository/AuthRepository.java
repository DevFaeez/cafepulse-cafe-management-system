package com.cafemanagement.cafeprediction.repository;

import com.cafemanagement.cafeprediction.model.Staff;
import com.cafemanagement.cafeprediction.model.enumoption.STAFF_ROLES;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthRepository extends JpaRepository<Staff, Long> {

    List<Staff> findByRoleNotOrderByIdAsc(STAFF_ROLES role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Staff s where s.id = :id")
    Optional<Staff> lockById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Staff s where s.email = :email")
    Optional<Staff> lockByEmail(@Param("email") String email);

    boolean existsByRole(STAFF_ROLES role);

    Optional<Staff> findByEmail(String email); //select * from staff where email = email

}
