package com.zhiguang.service;

import com.zhiguang.entity.AdminLog;
import com.zhiguang.repository.AdminLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLogService {

    private final AdminLogRepository adminLogRepository;

    /**
     * 记录管理员操作日志
     */
    public void log(Long adminId, String action, String targetType, Long targetId, String detail, String ip) {
        try {
            AdminLog adminLog = new AdminLog();
            adminLog.setAdminId(adminId);
            adminLog.setAction(action);
            adminLog.setTargetType(targetType);
            adminLog.setTargetId(targetId);
            adminLog.setDetail(detail);
            adminLog.setIp(ip);
            adminLogRepository.save(adminLog);
        } catch (Exception e) {
            log.error("记录审计日志失败", e);
        }
    }

    /**
     * 记录管理员操作日志（无 IP）
     */
    public void log(Long adminId, String action, String targetType, Long targetId, String detail) {
        log(adminId, action, targetType, targetId, detail, null);
    }

    /**
     * 分页查询操作日志（支持日期范围筛选）
     */
    public Page<AdminLog> getLogs(String action, LocalDate startDate, LocalDate endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;
        return adminLogRepository.searchLogs(
                action != null && !action.isBlank() ? action : null,
                start, end, pageable);
    }
}
