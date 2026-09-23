package com.ecommerce.auth.repository;

import com.ecommerce.auth.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {

    Optional<Otp> findTopByIdentifierOrderByIdDesc(String identifier);

    List<Otp> findByIdentifier(String identifier);

    @Modifying
    @Query("delete from Otp o where o.identifier = :identifier")
    void deleteByIdentifier(@Param("identifier") String identifier);

    @Modifying
    @Query("delete from Otp o where o.expiryTime < :now or o.verified = true")
    int deleteStale(@Param("now") LocalDateTime now);
}
