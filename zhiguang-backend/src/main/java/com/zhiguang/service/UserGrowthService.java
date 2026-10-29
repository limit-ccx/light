package com.zhiguang.service;

import com.zhiguang.entity.*;
import com.zhiguang.exception.BadRequestException;
import com.zhiguang.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserGrowthService {

    private static final int BASE_EXP_PER_LEVEL = 100;
    private static final int SIGN_IN_BASE_POINTS = 10;
    private static final int SIGN_IN_CONTINUOUS_BONUS = 2;
    private static final int SIGN_IN_MAX_POINTS = 30;
    private static final int SIGN_IN_BASE_EXP = 5;
    private static final int SIGN_IN_MAX_EXP = 20;

    private final UserLevelRepository userLevelRepository;
    private final ExperienceLogRepository experienceLogRepository;
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserPointRepository userPointRepository;
    private final PointLogRepository pointLogRepository;
    private final DailySignInRepository dailySignInRepository;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public UserLevel addExperience(Long userId, String source, int points) {
        if (points <= 0) {
            return getOrCreateUserLevel(userId);
        }

        UserLevel userLevel = getOrCreateUserLevel(userId);
        userLevel.setExperience(userLevel.getExperience() + points);
        userLevel.setLevel(calculateLevel(userLevel.getExperience()));

        ExperienceLog log = new ExperienceLog();
        log.setUserId(userId);
        log.setSource(source);
        log.setPoints(points);
        log.setBalance(userLevel.getExperience());
        experienceLogRepository.save(log);

        userLevelRepository.save(userLevel);
        return userLevel;
    }

    @Transactional
    public UserLevel getUserLevel(Long userId) {
        return getOrCreateUserLevel(userId);
    }

    @Transactional
    public List<Achievement> checkAchievements(Long userId) {
        List<Achievement> allAchievements = achievementRepository.findAll();
        Set<Long> obtainedIds = userAchievementRepository.findAchievementIdsByUserId(userId);

        List<Achievement> newlyObtained = new ArrayList<>();
        for (Achievement achievement : allAchievements) {
            if (obtainedIds.contains(achievement.getId())) {
                continue;
            }
            if (isAchievementMet(achievement.getCode(), userId)) {
                grantAchievement(userId, achievement);
                newlyObtained.add(achievement);
            }
        }

        if (!newlyObtained.isEmpty()) {
            log.info("用户 {} 新获得成就: {}", userId, newlyObtained.stream().map(Achievement::getCode).toList());
        }
        return newlyObtained;
    }

    @Transactional
    public Map<String, Object> signIn(Long userId) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        if (dailySignInRepository.countByUserIdAndSignDate(userId, today) > 0) {
            throw new BadRequestException("今日已签到");
        }

        int continuousDays = 1;
        Optional<DailySignIn> lastOpt = dailySignInRepository.findTopByUserIdOrderBySignDateDesc(userId);
        if (lastOpt.isPresent()) {
            DailySignIn last = lastOpt.get();
            if (last.getSignDate().plusDays(1).isEqual(today)) {
                continuousDays = last.getContinuousDays() + 1;
            }
        }

        int rewardPoints = Math.min(SIGN_IN_BASE_POINTS + continuousDays * SIGN_IN_CONTINUOUS_BONUS, SIGN_IN_MAX_POINTS);
        int rewardExp = Math.min(SIGN_IN_BASE_EXP + continuousDays, SIGN_IN_MAX_EXP);

        DailySignIn signIn = new DailySignIn();
        signIn.setUserId(userId);
        signIn.setSignDate(today);
        signIn.setContinuousDays(continuousDays);
        signIn.setRewardPoints(rewardPoints);
        dailySignInRepository.save(signIn);

        addPoints(userId, "SIGN_IN", rewardPoints, "连续签到 " + continuousDays + " 天奖励");
        addExperience(userId, "SIGN_IN", rewardExp);

        if (continuousDays >= 7) {
            checkAchievements(userId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("signDate", today);
        result.put("continuousDays", continuousDays);
        result.put("rewardPoints", rewardPoints);
        result.put("rewardExperience", rewardExp);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSignInStatus(Long userId) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        boolean signedInToday = dailySignInRepository.countByUserIdAndSignDate(userId, today) > 0;

        int continuousDays = 0;
        Optional<DailySignIn> lastOpt = dailySignInRepository.findTopByUserIdOrderBySignDateDesc(userId);
        if (lastOpt.isPresent()) {
            DailySignIn last = lastOpt.get();
            if (last.getSignDate().isEqual(today) || last.getSignDate().plusDays(1).isEqual(today)) {
                continuousDays = last.getContinuousDays();
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("signedInToday", signedInToday);
        result.put("continuousDays", continuousDays);
        return result;
    }

    @Transactional
    public UserPoint addPoints(Long userId, String type, int amount, String description) {
        if (amount == 0) {
            return getOrCreateUserPoint(userId);
        }

        UserPoint userPoint = getOrCreateUserPoint(userId);
        userPoint.setPoints(userPoint.getPoints() + amount);
        if (amount > 0) {
            userPoint.setTotalPoints(userPoint.getTotalPoints() + amount);
        }

        PointLog pointLog = new PointLog();
        pointLog.setUserId(userId);
        pointLog.setType(type);
        pointLog.setAmount(amount);
        pointLog.setBalance(userPoint.getPoints());
        pointLog.setDescription(description);
        pointLogRepository.save(pointLog);

        userPointRepository.save(userPoint);
        return userPoint;
    }

    @Transactional
    public UserPoint getUserPoints(Long userId) {
        return getOrCreateUserPoint(userId);
    }

    @Transactional(readOnly = true)
    public List<PointLog> getPointLogs(Long userId, int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return pointLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).getContent();
    }

    @Transactional(readOnly = true)
    public List<UserAchievement> getUserAchievements(Long userId) {
        return userAchievementRepository.findByUserIdOrderByObtainedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAchievementsWithStatus(Long userId) {
        List<Achievement> all = achievementRepository.findAll();
        Set<Long> obtainedIds = userAchievementRepository.findAchievementIdsByUserId(userId);

        return all.stream().map(achievement -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", achievement.getId());
            map.put("code", achievement.getCode());
            map.put("name", achievement.getName());
            map.put("description", achievement.getDescription());
            map.put("icon", achievement.getIcon());
            map.put("rewardPoints", achievement.getRewardPoints());
            map.put("requiredCondition", achievement.getRequiredCondition());
            map.put("obtained", obtainedIds.contains(achievement.getId()));
            return map;
        }).collect(Collectors.toList());
    }

    private UserLevel getOrCreateUserLevel(Long userId) {
        return userLevelRepository.findByUserId(userId).orElseGet(() -> {
            UserLevel level = new UserLevel();
            level.setUserId(userId);
            level.setLevel(1);
            level.setExperience(0);
            return userLevelRepository.save(level);
        });
    }

    private UserPoint getOrCreateUserPoint(Long userId) {
        return userPointRepository.findByUserId(userId).orElseGet(() -> {
            UserPoint point = new UserPoint();
            point.setUserId(userId);
            point.setPoints(0);
            point.setTotalPoints(0);
            return userPointRepository.save(point);
        });
    }

    private int calculateLevel(int experience) {
        int level = 1;
        while (experience >= BASE_EXP_PER_LEVEL * level * (level + 1) / 2) {
            level++;
        }
        return level;
    }

    private int experienceForNextLevel(int currentLevel) {
        return BASE_EXP_PER_LEVEL * currentLevel * (currentLevel + 1) / 2;
    }

    private boolean isAchievementMet(String code, Long userId) {
        return switch (code) {
            case "FIRST_POST" -> articleRepository.countByAuthorId(userId) >= 1;
            case "COMMENT_MASTER" -> commentRepository.countByUserId(userId) >= 10;
            case "COLLECTION_EXPERT" ->
                    Optional.ofNullable(articleRepository.sumFavoriteCountByAuthorId(userId)).orElse(0) >= 10;
            case "LIKES_100" -> Optional.ofNullable(articleRepository.sumLikeCountByAuthorId(userId)).orElse(0) >= 100;
            case "VIEWS_1000" -> Optional.ofNullable(articleRepository.sumViewCountByAuthorId(userId)).orElse(0) >= 1000;
            case "SIGN_IN_7_DAYS" -> {
                Optional<DailySignIn> last = dailySignInRepository.findTopByUserIdOrderBySignDateDesc(userId);
                yield last.isPresent() && last.get().getContinuousDays() >= 7;
            }
            default -> false;
        };
    }

    private void grantAchievement(Long userId, Achievement achievement) {
        UserAchievement userAchievement = new UserAchievement();
        userAchievement.setUserId(userId);
        userAchievement.setAchievement(achievement);
        userAchievementRepository.save(userAchievement);

        if (achievement.getRewardPoints() != null && achievement.getRewardPoints() > 0) {
            addPoints(userId, "ACHIEVEMENT", achievement.getRewardPoints(),
                    "获得成就「" + achievement.getName() + "」奖励");
        }
    }
}
