package com.zhiguang.repository;

import com.zhiguang.entity.Banner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    Page<Banner> findByStatus(String status, Pageable pageable);

    List<Banner> findByStatusAndPositionOrderBySortOrderDesc(String status, String position);
}
