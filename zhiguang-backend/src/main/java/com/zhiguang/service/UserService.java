package com.zhiguang.service;

import com.zhiguang.dto.AuthResponseDto;
import com.zhiguang.dto.AuthSnapshot;
import com.zhiguang.dto.LoginRequestDto;
import com.zhiguang.dto.PublicUserDto;
import com.zhiguang.dto.RegisterRequestDto;
import com.zhiguang.dto.UserUpdateDto;
import com.zhiguang.entity.User;
import com.zhiguang.exception.BadRequestException;
import com.zhiguang.exception.ResourceNotFoundException;
import com.zhiguang.repository.ArticleRepository;
import com.zhiguang.repository.UserFollowRepository;
import com.zhiguang.repository.UserRepository;
import com.zhiguang.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Duration USER_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration AUTH_CACHE_TTL = Duration.ofMinutes(10);
    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "12345678", "password123", "qwerty123", "admin123", "letmein123",
            "abc12345", "iloveyou", "passw0rd", "11111111", "welcome123"
    );

    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final UserFollowRepository userFollowRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CacheService cacheService;
    private final TransactionTemplate transactionTemplate;
    private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    /** 登录失败锁定：同一账号 15 分钟窗口内失败 5 次即锁定（防在线爆破，配合 IP 限流形成双保险） */
    private static final int MAX_LOGIN_FAILURES = 5;
    private static final java.time.Duration LOGIN_LOCK_WINDOW = java.time.Duration.ofMinutes(15);
    private static final String LOGIN_FAIL_PREFIX = "login:fail:";

    public AuthResponseDto register(RegisterRequestDto request) {
        String username = normalizeUsername(request.getUsername());
        String email = normalizeEmail(request.getEmail());
        validatePassword(request.getPassword());

        try {
            User saved = transactionTemplate.execute(status -> {
                if (userRepository.existsByUsername(username)) {
                    throw new BadRequestException("用户名已存在");
                }
                if (userRepository.existsByEmail(email)) {
                    throw new BadRequestException("邮箱已被注册");
                }

                User user = new User();
                user.setUsername(username);
                user.setEmail(email);
                user.setPassword(passwordEncoder.encode(request.getPassword()));
                user.setAvatar("https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png");
                user.setRole("USER");
                user.setStatus("NORMAL");
                user.setSecurityVersion(0);
                return userRepository.save(user);
            });

            if (saved != null) {
                return issueAuthResponse(saved);
            }
            throw new BadRequestException("注册失败，请稍后重试");
        } catch (DataIntegrityViolationException e) {
            if (userRepository.existsByUsername(username)) {
                throw new BadRequestException("用户名已存在");
            }
            throw new BadRequestException("邮箱已被注册");
        }
    }

    public AuthResponseDto login(LoginRequestDto request) {
        String identifier = firstNonBlank(request.getIdentifier(), request.getEmail());
        if (identifier == null) {
            throw new BadRequestException("请输入邮箱或用户名");
        }

        String normalized = identifier.trim();
        checkLoginLockout(normalized);
        try {
            AuthResponseDto response = doLogin(request, normalized);
            clearLoginFailures(normalized);
            return response;
        } catch (BadRequestException e) {
            recordLoginFailure(normalized);
            throw e;
        }
    }

    private AuthResponseDto doLogin(LoginRequestDto request, String normalized) {
        User user = normalized.contains("@")
                ? userRepository.findByEmail(normalizeEmail(normalized))
                        .orElseThrow(() -> new BadRequestException("账号或密码错误"))
                : userRepository.findByUsername(normalized)
                        .orElseThrow(() -> new BadRequestException("账号或密码错误"));

        if (request.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("账号或密码错误");
        }

        if ("BANNED".equalsIgnoreCase(user.getStatus())) {
            throw new BadRequestException("该账号已被封禁，请联系管理员");
        }

        return issueAuthResponse(user);
    }

    private void checkLoginLockout(String identifier) {
        try {
            String count = stringRedisTemplate.opsForValue().get(LOGIN_FAIL_PREFIX + identifier);
            if (count != null && Integer.parseInt(count) >= MAX_LOGIN_FAILURES) {
                throw new BadRequestException("登录失败次数过多，请 15 分钟后再试");
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            // 锁定组件故障不阻断登录（还有端点级 IP 限流兜底）
        }
    }

    private void recordLoginFailure(String identifier) {
        try {
            String key = LOGIN_FAIL_PREFIX + identifier;
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(key, LOGIN_LOCK_WINDOW);
            }
        } catch (Exception ignore) {
        }
    }

    private void clearLoginFailures(String identifier) {
        try {
            stringRedisTemplate.delete(LOGIN_FAIL_PREFIX + identifier);
        } catch (Exception ignore) {
        }
    }

    public AuthResponseDto issueAuthResponse(User user) {
        return new AuthResponseDto(jwtTokenProvider.generateToken(user), buildUserDto(user));
    }

    public AuthResponseDto issueAuthResponseByUserId(Long userId) {
        User user = getUserById(userId);
        return issueAuthResponse(user);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", id));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(normalizeUsername(username))
                .orElseThrow(() -> new ResourceNotFoundException("用户", "username", username));
    }

    public User updateUser(Long userId, UserUpdateDto updateDto) {
        User saved = transactionTemplate.execute(status -> {
            User user = getUserById(userId);

            if (updateDto.getUsername() != null && !normalizeUsername(updateDto.getUsername()).equals(user.getUsername())) {
                String username = normalizeUsername(updateDto.getUsername());
                if (userRepository.existsByUsername(username)) {
                    throw new BadRequestException("用户名已存在");
                }
                user.setUsername(username);
            }

            if (updateDto.getAvatar() != null) {
                user.setAvatar(updateDto.getAvatar());
            }

            if (updateDto.getBio() != null) {
                user.setBio(updateDto.getBio());
            }

            return userRepository.save(user);
        });

        if (saved != null) {
            evictUserCache(userId);
            return saved;
        }
        throw new BadRequestException("资料更新失败");
    }

    public AuthResponseDto.UserDto getUserDto(Long userId) {
        String cacheKey = "user:dto:" + userId;
        Optional<AuthResponseDto.UserDto> cached = cacheService.getOrLoad(
                cacheKey, USER_CACHE_TTL,
                () -> {
                    User user = getUserById(userId);
                    return Optional.of(buildUserDto(user));
                },
                AuthResponseDto.UserDto.class
        );
        return cached.orElseThrow(() -> new ResourceNotFoundException("用户", "id", userId));
    }

    public PublicUserDto getPublicUserDto(Long userId) {
        return buildPublicUserDto(getUserById(userId));
    }

    public Map<String, Object> getUserStats(Long userId) {
        String cacheKey = "user:stats:" + userId;
        Optional<Map<String, Object>> cached = cacheService.getOrLoad(
                cacheKey, USER_CACHE_TTL,
                () -> Optional.of(loadUserStats(userId)),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
        );
        return cached.orElseGet(() -> Map.of(
                "articleCount", 0,
                "followCount", 0,
                "fansCount", 0
        ));
    }

    public Optional<AuthSnapshot> findAuthSnapshot(Long userId) {
        String cacheKey = "user:auth:" + userId;
        return cacheService.getOrLoad(
                cacheKey, AUTH_CACHE_TTL,
                () -> userRepository.findById(userId).map(this::toAuthSnapshot),
                AuthSnapshot.class
        );
    }

    public User buildPrincipal(AuthSnapshot snapshot) {
        User principal = new User();
        principal.setId(snapshot.getId());
        principal.setUsername(snapshot.getUsername());
        principal.setEmail(snapshot.getEmail());
        principal.setAvatar(snapshot.getAvatar());
        principal.setBio(snapshot.getBio());
        principal.setRole(snapshot.getRole());
        principal.setAdminPosition(snapshot.getAdminPosition());
        principal.setStatus(snapshot.getStatus());
        principal.setCreatorBadges(snapshot.getCreatorBadges());
        principal.setSecurityVersion(snapshot.getSecurityVersion());
        principal.setCreatedAt(snapshot.getCreatedAt());
        principal.setUpdatedAt(snapshot.getUpdatedAt());
        return principal;
    }

    public void invalidateAuthSnapshot(Long userId) {
        cacheService.evict("user:auth:" + userId);
    }

    public void bumpSecurityVersion(Long userId) {
        transactionTemplate.executeWithoutResult(status -> userRepository.incrementSecurityVersion(userId));
        invalidateAuthSnapshot(userId);
    }

    private Map<String, Object> loadUserStats(Long userId) {
        long articleCount = articleRepository.countByAuthorIdAndStatus(userId, 2);
        long followCount = userFollowRepository.countByFollowerId(userId);
        long fansCount = userFollowRepository.countByFollowingId(userId);

        return Map.of(
                "articleCount", articleCount,
                "followCount", followCount,
                "fansCount", fansCount
        );
    }

    private void evictUserCache(Long userId) {
        cacheService.evict("user:dto:" + userId);
        cacheService.evict("user:stats:" + userId);
        invalidateAuthSnapshot(userId);
    }

    public AuthResponseDto.UserDto buildUserDto(User user) {
        return new AuthResponseDto.UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAvatar(),
                user.getBio(),
                user.getCreatorBadges(),
                user.getRole() != null ? user.getRole() : "USER",
                user.getAdminPosition(),
                user.getStatus() != null ? user.getStatus() : "NORMAL"
        );
    }

    public PublicUserDto buildPublicUserDto(User user) {
        return new PublicUserDto(
                user.getId(),
                user.getUsername(),
                user.getAvatar(),
                user.getBio(),
                user.getCreatorBadges(),
                user.getCreatedAt()
        );
    }

    private AuthSnapshot toAuthSnapshot(User user) {
        AuthSnapshot snapshot = new AuthSnapshot();
        snapshot.setId(user.getId());
        snapshot.setUsername(user.getUsername());
        snapshot.setEmail(user.getEmail());
        snapshot.setAvatar(user.getAvatar());
        snapshot.setBio(user.getBio());
        snapshot.setRole(user.getRole() != null ? user.getRole() : "USER");
        snapshot.setAdminPosition(user.getAdminPosition());
        snapshot.setStatus(user.getStatus() != null ? user.getStatus() : "NORMAL");
        snapshot.setCreatorBadges(user.getCreatorBadges());
        snapshot.setSecurityVersion(user.getSecurityVersion() == null ? 0 : user.getSecurityVersion());
        snapshot.setCreatedAt(user.getCreatedAt());
        snapshot.setUpdatedAt(user.getUpdatedAt());
        return snapshot;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<User> searchUsers(String keyword, Long excludeUserId) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        return userRepository.findByUsernameContainingIgnoreCase(
                keyword.trim(),
                excludeUserId,
                org.springframework.data.domain.PageRequest.of(0, 10)
        ).getContent();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("邮箱不能为空");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new BadRequestException("用户名不能为空");
        }
        String normalized = username.trim();
        if (!normalized.matches("[A-Za-z0-9_\\u4e00-\\u9fa5]{3,20}")) {
            throw new BadRequestException("用户名只能包含中英文、数字或下划线，长度3-20个字符");
        }
        return normalized;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new BadRequestException("密码长度必须在8-72个字符之间");
        }
        if (!password.matches("(?s).*[A-Za-z].*") || !password.matches("(?s).*\\d.*")) {
            throw new BadRequestException("密码必须同时包含字母和数字");
        }
        if (COMMON_PASSWORDS.contains(password.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("密码过于常见，请更换");
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
