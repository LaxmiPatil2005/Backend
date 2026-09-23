package com.ecommerce.auth.repository;

import com.ecommerce.auth.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByToken(String token);

    List<Session> findByUserId(Long userId);

    @Modifying
    @Query("delete from Session s where s.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from Session s where s.user.id = :userId and s.token <> :currentToken")
    int deleteOtherSessions(@Param("userId") Long userId, @Param("currentToken") String currentToken);

    @Modifying
    @Query("delete from Session s where s.expiryTime < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
