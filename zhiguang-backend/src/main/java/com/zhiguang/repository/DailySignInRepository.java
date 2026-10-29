package com.zhiguang.repository;

import com.zhiguang.entity.DailySignIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailySignInRepository extends JpaRepository<DailySignIn, Long> {

    Optional<DailySignIn> findByUserIdAndSignDate(Long userId, LocalDate signDate);

    Optional<DailySignIn> findTopByUserIdOrderBySignDateDesc(Long userId);

    long countByUserIdAndSignDate(Long userId, LocalDate signDate);

    List<DailySignIn> findByUserIdAndSignDateBetweenOrderBySignDateAsc(Long userId, LocalDate startDate, LocalDate endDate);

    List<DailySignIn> findByUserIdAndSignDateIn(Long userId, List<LocalDate> signDates);
}
