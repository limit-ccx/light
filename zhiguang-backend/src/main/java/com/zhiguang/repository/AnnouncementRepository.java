package com.zhiguang.repository;

import com.zhiguang.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    Page<Announcement> findByStatus(String status, Pageable pageable);

    List<Announcement> findByStatusOrderByPublishAtDesc(String status);
}
