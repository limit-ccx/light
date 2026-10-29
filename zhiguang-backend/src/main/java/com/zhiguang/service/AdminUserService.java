package com.zhiguang.service;

import com.zhiguang.dto.AdminUserDto;
import com.zhiguang.dto.PageResponse;
import com.zhiguang.entity.User;
import com.zhiguang.exception.BadRequestException;
import com.zhiguang.exception.ResourceNotFoundException;
import com.zhiguang.repository.ArticleRepository;
import com.zhiguang.repository.CommentRepository;
import com.zhiguang.repository.UserFollowRepository;
import com.zhiguang.repository.UserRepository;
import com.zhiguang.security.AdminPermissions;
import com.zhiguang.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final UserFollowRepository userFollowRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminLogService adminLogService;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    /**
     * 分页查询用户（支持用户名/邮箱/角色/状态筛选）
     */
    public PageResponse<AdminUserDto> getUsers(String username, String email, String role, String status,
                                               boolean adminOnly, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        // 运营管理只负责管理员账号，列表强制收敛到 ADMIN
        String effectiveRole = adminOnly ? "ADMIN" : role;
        Page<User> userPage = userRepository.searchForAdmin(username, email, effectiveRole, status, pageable);

        List<AdminUserDto> list = userPage.getContent().stream()
                .map(this::toAdminUserDto)
                .toList();

        return new PageResponse<>(list, userPage.getTotalElements(), userPage.getNumber(),
                userPage.getSize(), userPage.getTotalPages());
    }

    /**
     * 运营管理：管理员账号列表（仅 ADMIN，可按职位/关键词筛选）
     */
    public PageResponse<AdminUserDto> getAdmins(String keyword, String position, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String pos = (position == null || position.isBlank()) ? null : position;
        Page<User> userPage = userRepository.searchAdmins(kw, pos, pageable);

        List<AdminUserDto> list = userPage.getContent().stream()
                .map(this::toAdminUserDto)
                .toList();

        return new PageResponse<>(list, userPage.getTotalElements(), userPage.getNumber(),
                userPage.getSize(), userPage.getTotalPages());
    }

    /**
     * 获取用户详情
     */
    public AdminUserDto getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", userId));
        return toAdminUserDto(user);
    }

    /**
     * 封禁用户
     * 同时将用户 Token 加入黑名单，使其立即失效
     */
    @Transactional
    public void banUser(Long userId, Long adminId) {
        assertNotAdminManager(adminId, "运营管理不能封禁账号");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", userId));

        if ("ADMIN".equals(user.getRole())) {
            throw new BadRequestException("无法封禁管理员账号");
        }
        if ("BANNED".equals(user.getStatus())) {
            throw new BadRequestException("该用户已被封禁");
        }

        String oldStatus = user.getStatus();
        user.setStatus("BANNED");
        userRepository.save(user);
        userService.bumpSecurityVersion(userId);
        refreshTokenService.revokeAll(userId);

        adminLogService.log(adminId, "BAN_USER", "USER", userId,
                "封禁用户: " + user.getUsername() + ", 状态: " + oldStatus + " -> BANNED");
    }

    /**
     * 解封用户
     */
    @Transactional
    public void unbanUser(Long userId, Long adminId) {
        assertNotAdminManager(adminId, "运营管理不能解封账号");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", userId));

        if (!"BANNED".equals(user.getStatus())) {
            throw new BadRequestException("该用户未被封禁");
        }

        user.setStatus("NORMAL");
        userRepository.save(user);
        userService.bumpSecurityVersion(userId);
        refreshTokenService.revokeAll(userId);

        adminLogService.log(adminId, "UNBAN_USER", "USER", userId,
                "解封用户: " + user.getUsername());
    }

    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    /**
     * 重置用户密码
     * 生成随机临时密码（8位），不再使用硬编码弱口令
     */
    @Transactional
    public String resetPassword(Long userId, Long adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", userId));

        // 运营管理只负责普通管理员，不能重置超级管理员密码
        User operator = userRepository.findById(adminId).orElse(null);
        if (operator != null && AdminPermissions.isAdminManagerOnly(operator.getAdminPosition())) {
            if (!"ADMIN".equals(user.getRole())) {
                throw new BadRequestException("运营管理只能重置管理员密码");
            }
            if (AdminPermissions.isSuper(user.getAdminPosition())) {
                throw new BadRequestException("运营管理不能重置超级管理员密码");
            }
        }

        String tempPassword = generateTempPassword(12);
        user.setPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);
        userService.bumpSecurityVersion(userId);
        refreshTokenService.revokeAll(userId);

        adminLogService.log(adminId, "RESET_PASSWORD", "USER", userId,
                "重置用户 " + user.getUsername() + " 的密码为随机临时密码");

        return tempPassword;
    }

    /**
     * 生成随机临时密码
     */
    private String generateTempPassword(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(random.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * 设置用户角色
     * 安全限制：
     * 1. 管理员不能修改自己的角色（防止自降级锁死）
     * 2. 管理员不能修改其他管理员的角色（防止互相提权/降权）
     * 3. 系统至少保留一个管理员
     */
    @Transactional
    public void updateUserRole(Long userId, String role, Long adminId) {
        requireSuper(adminId);
        if (!"USER".equals(role) && !"ADMIN".equals(role) && !"CREATOR".equals(role)) {
            throw new BadRequestException("无效的角色值");
        }

        // 禁止管理员修改自身角色
        if (userId.equals(adminId)) {
            throw new BadRequestException("不能修改自己的角色");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", userId));

        // 若将当前用户降级为非 ADMIN，需确保系统中至少还剩一个管理员
        if ("ADMIN".equals(user.getRole()) && !"ADMIN".equals(role)) {
            long adminCount = userRepository.countByRole("ADMIN");
            if (adminCount <= 1) {
                throw new BadRequestException("系统至少需要保留一个管理员账号");
            }
        }

        String oldRole = user.getRole();
        user.setRole(role);
        if ("ADMIN".equals(role)) {
            if (user.getAdminPosition() == null) {
                user.setAdminPosition(AdminPermissions.CONTENT);
            }
        } else {
            user.setAdminPosition(null);
        }
        userRepository.save(user);
        userService.bumpSecurityVersion(userId);
        refreshTokenService.revokeAll(userId);

        adminLogService.log(adminId, "UPDATE_ROLE", "USER", userId,
                "用户 " + user.getUsername() + " 角色: " + oldRole + " -> " + role);
    }

    /**
     * 分配后台职位，仅超级管理员可操作。
     */
    @Transactional
    public void updateAdminPosition(Long userId, String position, Long adminId) {
        User operator = requireAdminManager(adminId);
        if (!AdminPermissions.isPosition(position)) {
            throw new BadRequestException("无效的后台职位");
        }
        if (userId.equals(adminId)) {
            throw new BadRequestException("不能修改自己的职位");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", userId));
        if (!"ADMIN".equals(user.getRole())) {
            throw new BadRequestException("该用户不是管理员，无法分配职位");
        }
        // 运营管理只能管理普通管理员，不能调整超级管理员
        if (AdminPermissions.isAdminManagerOnly(operator.getAdminPosition())
                && AdminPermissions.isSuper(user.getAdminPosition())) {
            throw new BadRequestException("运营管理不能调整超级管理员");
        }
        // 运营管理不能把他人提升为超级管理员，只有超级管理员可以
        if (AdminPermissions.SUPER.equals(position) && !isSuperAdmin(adminId)) {
            throw new BadRequestException("只有超级管理员可以指派超级管理员");
        }

        String old = user.getAdminPosition();
        user.setAdminPosition(position);
        userRepository.save(user);
        userService.bumpSecurityVersion(userId);
        refreshTokenService.revokeAll(userId);
        adminLogService.log(adminId, "UPDATE_POSITION", "USER", userId,
                "管理员 " + user.getUsername() + " 职位: " + old + " -> " + position);
    }

    private boolean isSuperAdmin(Long adminId) {
        return userRepository.findById(adminId)
                .map(admin -> AdminPermissions.isSuper(admin.getAdminPosition()))
                .orElse(false);
    }

    private User requireAdminManager(Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", adminId));
        if (!AdminPermissions.canManageAdmins(admin.getAdminPosition())) {
            throw new BadRequestException("仅超级管理员或运营管理可分配职位");
        }
        return admin;
    }    private void assertNotAdminManager(Long adminId, String message) {
        User admin = userRepository.findById(adminId).orElse(null);
        if (admin != null && AdminPermissions.isAdminManagerOnly(admin.getAdminPosition())) {
            throw new BadRequestException(message);
        }
    }

    private void requireSuper(Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", adminId));
        if (!AdminPermissions.isSuper(admin.getAdminPosition())) {
            throw new BadRequestException("仅超级管理员可执行该操作");
        }
    }

    /**
     * 用户统计概览
     */
    public Map<String, Object> getUserOverview() {
        long totalUsers = userRepository.count();
        long todayNewUsers = userRepository.countByCreatedToday();
        long bannedUsers = userRepository.countByStatus("BANNED");
        long adminUsers = userRepository.countByRole("ADMIN");
        long activeUsers = userRepository.countByStatus("NORMAL");
        long creatorCount = userRepository.countByRole("CREATOR");

        return Map.of(
                "totalUsers", totalUsers,
                "todayNewUsers", todayNewUsers,
                "bannedUsers", bannedUsers,
                "adminCount", adminUsers,
                "activeUsers", activeUsers,
                "creatorCount", creatorCount
        );
    }

    private AdminUserDto toAdminUserDto(User user) {
        long articleCount = articleRepository.countByAuthorIdAndStatus(user.getId(), 2);
        long commentCount = commentRepository.countByUserId(user.getId());
        long followerCount = userFollowRepository.countByFollowingId(user.getId());

        return new AdminUserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAvatar(),
                user.getBio(),
                user.getRole() != null ? user.getRole() : "USER",
                user.getAdminPosition(),
                user.getStatus() != null ? user.getStatus() : "NORMAL",
                user.getCreatorBadges(),
                articleCount,
                commentCount,
                followerCount,
                user.getCreatedAt()
        );
    }
}
