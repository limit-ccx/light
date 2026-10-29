package com.zhiguang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_statistics", indexes = {
        @Index(name = "idx_daily_stat_date", columnList = "stat_date")
},
        uniqueConstraints = @UniqueConstraint(name = "uk_date_metric", columnNames = {"stat_date", "metric"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(nullable = false, length = 50)
    private String metric;

    @Column
    private Long value = 0L;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
