package com.zhiguang.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"password", "articles", "comments", "creatorBadges"})
@EqualsAndHashCode(exclude = {"articles", "comments", "creatorBadges"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @Column(columnDefinition = "TEXT")
    private String avatar;

    @Column(length = 500)
    private String bio;

    /**
     * 角色：USER-普通用户 ADMIN-管理员 CREATOR-创作者
     */
    @Column(length = 20)
    private String role = "USER";

    /** 后台职位：SUPER / CONTENT / COMMUNITY / USER / SYSTEM / DATA */
    @Column(name = "admin_position", length = 20)
    private String adminPosition;

    /**
     * 状态：NORMAL-正常 BANNED-已封禁
     */
    @Column(length = 20)
    private String status = "NORMAL";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_creator_badges", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "badge")
    private List<String> creatorBadges = new ArrayList<>();

    @Column(name = "security_version", nullable = false)
    private Integer securityVersion = 0;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Article> articles = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}