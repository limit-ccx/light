package com.zhiguang.service;

import com.zhiguang.dto.SignInCalendarDto;
import com.zhiguang.dto.SignInDayDto;
import com.zhiguang.dto.SignInResultDto;
import com.zhiguang.entity.*;
import com.zhiguang.exception.BadRequestException;
import com.zhiguang.repository.DailySignInRepository;
import com.zhiguang.repository.SignInSupplementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * 签到服务
 * 实现签到、补签、连续签到奖励、积分经验发放
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignInService {

    private static final int SIGN_IN_BASE_POINTS = 10;
    private static final int SIGN_IN_CONTINUOUS_BONUS = 2;
    private static final int SIGN_IN_MAX_POINTS = 30;
    private static final int SIGN_IN_BASE_EXP = 5;
    private static final int SIGN_IN_MAX_EXP = 20;
    private static final int SUPPLEMENT_MAX_PER_MONTH = 3;
    private static final int SUPPLEMENT_BASE_POINTS = 10;
    private static final int SUPPLEMENT_BASE_EXP = 5;

    private final DailySignInRepository dailySignInRepository;
    private final SignInSupplementRepository supplementRepository;
    private final UserGrowthService userGrowthService;

    /**
     * 今日签到
     */
    @Transactional
    public SignInResultDto signIn(Long userId) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        return performSignIn(userId, today, false);
    }

    /**
     * 补签
     */
    @Transactional
    public SignInResultDto supplementSignIn(Long userId, LocalDate date) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));

        // 补签限制：只能补签当月内的日期
        if (date == null || !date.isBefore(today) || !isSameMonth(date, today)) {
            throw new BadRequestException("只能补签本月内未签到的日期");
        }

        // 检查是否已签到
        if (dailySignInRepository.findByUserIdAndSignDate(userId, date).isPresent()) {
            throw new BadRequestException("该日期已签到");
        }

        // 检查补签次数
        SignInSupplement supplement = getOrCreateSupplement(userId, today);
        if (supplement.getRemainingCount() <= 0) {
            throw new BadRequestException("本月补签次数已用完");
        }

        // 执行补签
        SignInResultDto result = performSignIn(userId, date, true);

        // 扣减补签次数
        supplement.setUsedCount(supplement.getUsedCount() + 1);
        supplementRepository.save(supplement);

        log.info("用户 {} 补签成功: {}, 本月剩余补签次数: {}", userId, date, supplement.getRemainingCount());
        return result;
    }

    /**
     * 执行签到核心逻辑
     */
    private SignInResultDto performSignIn(Long userId, LocalDate signDate, boolean isSupplement) {
        // 幂等性检查
        if (dailySignInRepository.findByUserIdAndSignDate(userId, signDate).isPresent()) {
            throw new BadRequestException("该日期已签到");
        }

        // 计算连续签到天数
        int continuousDays = calculateContinuousDays(userId, signDate);

        // 计算奖励
        int rewardPoints = isSupplement
                ? SUPPLEMENT_BASE_POINTS
                : Math.min(SIGN_IN_BASE_POINTS + continuousDays * SIGN_IN_CONTINUOUS_BONUS, SIGN_IN_MAX_POINTS);
        int rewardExp = isSupplement
                ? SUPPLEMENT_BASE_EXP
                : Math.min(SIGN_IN_BASE_EXP + continuousDays, SIGN_IN_MAX_EXP);

        // 保存签到记录
        DailySignIn signIn = new DailySignIn();
        signIn.setUserId(userId);
        signIn.setSignDate(signDate);
        signIn.setContinuousDays(continuousDays);
        signIn.setRewardPoints(rewardPoints);
        signIn.setRewardExperience(rewardExp);
        signIn.setSupplement(isSupplement);
        dailySignInRepository.save(signIn);

        // 发放积分和经验
        String description = isSupplement
                ? "补签 " + signDate + " 奖励"
                : "连续签到 " + continuousDays + " 天奖励";
        UserPoint userPoint = userGrowthService.addPoints(userId, isSupplement ? "SUPPLEMENT_SIGN_IN" : "SIGN_IN", rewardPoints, description);
        UserLevel userLevel = userGrowthService.addExperience(userId, isSupplement ? "SUPPLEMENT_SIGN_IN" : "SIGN_IN", rewardExp);

        // 检查成就
        if (continuousDays >= 7) {
            userGrowthService.checkAchievements(userId);
        }

        SignInResultDto result = new SignInResultDto();
        result.setSignDate(signDate);
        result.setContinuousDays(continuousDays);
        result.setRewardPoints(rewardPoints);
        result.setRewardExperience(rewardExp);
        result.setSupplement(isSupplement);
        result.setTotalPoints(userPoint.getPoints());
        result.setTotalExperience(userLevel.getExperience());
        result.setLevel(userLevel.getLevel());

        log.info("用户 {} {}成功: {}, 连续 {} 天, 积分 +{}, 经验 +{}",
                userId, isSupplement ? "补签" : "签到", signDate, continuousDays, rewardPoints, rewardExp);
        return result;
    }

    /**
     * 获取签到日历
     */
    @Transactional
    public SignInCalendarDto getCalendar(Long userId, int year, int month) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
        LocalDate lastDayOfMonth = firstDayOfMonth.with(TemporalAdjusters.lastDayOfMonth());

        // 查询当月签到记录
        List<DailySignIn> signIns = dailySignInRepository.findByUserIdAndSignDateBetweenOrderBySignDateAsc(
                userId, firstDayOfMonth, lastDayOfMonth);
        Set<LocalDate> signedDates = new HashSet<>();
        Map<LocalDate, DailySignIn> signInMap = new HashMap<>();
        for (DailySignIn signIn : signIns) {
            signedDates.add(signIn.getSignDate());
            signInMap.put(signIn.getSignDate(), signIn);
        }

        // 构建每日状态
        List<SignInDayDto> days = new ArrayList<>();
        LocalDate date = firstDayOfMonth;
        while (!date.isAfter(lastDayOfMonth)) {
            SignInDayDto day = new SignInDayDto();
            day.setDate(date);
            day.setDayOfMonth(date.getDayOfMonth());
            day.setSigned(signedDates.contains(date));

            if (day.getSigned()) {
                DailySignIn signIn = signInMap.get(date);
                day.setSupplement(signIn.getSupplement());
                day.setRewardPoints(signIn.getRewardPoints());
                day.setRewardExperience(signIn.getRewardExperience());
                day.setCanSign(false);
                day.setCanSupplement(false);
            } else {
                day.setSupplement(false);
                day.setCanSign(date.isEqual(today));
                day.setCanSupplement(date.isBefore(today) && isSameMonth(date, today));
            }

            days.add(day);
            date = date.plusDays(1);
        }

        // 计算连续签到天数
        int continuousDays = calculateCurrentContinuousDays(userId, today);

        // 当月签到天数
        int signedDays = signIns.size();

        // 补签次数
        SignInSupplement supplement = getOrCreateSupplement(userId, today);
        int supplementRemaining = supplement.getRemainingCount();

        // 今日是否已签到
        boolean signedToday = dailySignInRepository.countByUserIdAndSignDate(userId, today) > 0;

        SignInCalendarDto calendar = new SignInCalendarDto();
        calendar.setYear(year);
        calendar.setMonth(month);
        calendar.setDays(days);
        calendar.setContinuousDays(continuousDays);
        calendar.setSignedDays(signedDays);
        calendar.setSupplementRemaining(supplementRemaining);
        calendar.setSignedToday(signedToday);

        return calendar;
    }

    /**
     * 获取签到状态
     */
    @Transactional
    public Map<String, Object> getSignInStatus(Long userId) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        boolean signedInToday = dailySignInRepository.countByUserIdAndSignDate(userId, today) > 0;
        int continuousDays = calculateCurrentContinuousDays(userId, today);

        SignInSupplement supplement = getOrCreateSupplement(userId, today);

        Map<String, Object> result = new HashMap<>();
        result.put("signedInToday", signedInToday);
        result.put("continuousDays", continuousDays);
        result.put("supplementRemaining", supplement.getRemainingCount());
        result.put("supplementUsed", supplement.getUsedCount());
        result.put("supplementMax", supplement.getMaxCount());
        return result;
    }

    /**
     * 计算到指定日期的连续签到天数
     */
    private int calculateContinuousDays(Long userId, LocalDate signDate) {
        // 查询前一天记录
        Optional<DailySignIn> prevOpt = dailySignInRepository.findByUserIdAndSignDate(userId, signDate.minusDays(1));
        if (prevOpt.isPresent()) {
            return prevOpt.get().getContinuousDays() + 1;
        }
        return 1;
    }

    /**
     * 计算当前连续签到天数
     */
    private int calculateCurrentContinuousDays(Long userId, LocalDate today) {
        Optional<DailySignIn> lastOpt = dailySignInRepository.findTopByUserIdOrderBySignDateDesc(userId);
        if (lastOpt.isEmpty()) {
            return 0;
        }
        DailySignIn last = lastOpt.get();
        if (last.getSignDate().isEqual(today) || last.getSignDate().plusDays(1).isEqual(today)) {
            return last.getContinuousDays();
        }
        return 0;
    }

    /**
     * 判断两个日期是否同月
     */
    private boolean isSameMonth(LocalDate date1, LocalDate date2) {
        return date1.getYear() == date2.getYear() && date1.getMonthValue() == date2.getMonthValue();
    }

    /**
     * 获取或创建补签记录
     */
    private SignInSupplement getOrCreateSupplement(Long userId, LocalDate date) {
        String monthKey = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return supplementRepository.findByUserIdAndMonthKey(userId, monthKey).orElseGet(() -> {
            SignInSupplement s = new SignInSupplement();
            s.setUserId(userId);
            s.setMonthKey(monthKey);
            s.setUsedCount(0);
            s.setMaxCount(SUPPLEMENT_MAX_PER_MONTH);
            return supplementRepository.save(s);
        });
    }
}
