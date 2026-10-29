package com.zhiguang.repository;

import com.zhiguang.entity.SignInSupplement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SignInSupplementRepository extends JpaRepository<SignInSupplement, Long> {

    Optional<SignInSupplement> findByUserIdAndMonthKey(Long userId, String monthKey);
}
