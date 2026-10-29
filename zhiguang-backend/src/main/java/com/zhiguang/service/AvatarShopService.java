package com.zhiguang.service;

import com.zhiguang.dto.UserUpdateDto;
import com.zhiguang.entity.User;
import com.zhiguang.entity.UserAvatarUnlock;
import com.zhiguang.entity.UserPoint;
import com.zhiguang.exception.BadRequestException;
import com.zhiguang.repository.UserAvatarUnlockRepository;
import com.zhiguang.repository.UserPointRepository;
import com.zhiguang.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 头像商城：使用签到积分兑换头像
 */
@Service
@RequiredArgsConstructor
public class AvatarShopService {

    public static final int AVATAR_PRICE = 1000;

    private static final List<AvatarItem> CATALOG = List.of(
            new AvatarItem("avatar-01", "初樱", "粉樱双马尾", "/avatars/avatar-01.jpg"),
            new AvatarItem("avatar-02", "蓝调", "蓝调耳机", "/avatars/avatar-02.jpg"),
            new AvatarItem("avatar-03", "蔷薇", "蔷薇长发", "/avatars/avatar-03.jpg"),
            new AvatarItem("avatar-04", "蝶结", "蝶结编发", "/avatars/avatar-04.jpg"),
            new AvatarItem("avatar-05", "雾蓝", "晨雾卫衣", "/avatars/avatar-05.jpg"),
            new AvatarItem("avatar-06", "紫帽", "贝雷帽少女", "/avatars/avatar-06.jpg"),
            new AvatarItem("avatar-07", "绒雪", "绒粉短发", "/avatars/avatar-07.jpg"),
            new AvatarItem("avatar-08", "晚风", "侧马尾", "/avatars/avatar-08.jpg"),
            new AvatarItem("avatar-09", "蜜橙", "蜜橙晴空", "/avatars/avatar-09.jpg"),
            new AvatarItem("avatar-10", "星镜", "星蓝眼镜", "/avatars/avatar-10.jpg"),
            new AvatarItem("avatar-11", "跃动", "活力马尾", "/avatars/avatar-11.jpg"),
            new AvatarItem("avatar-12", "星河", "星河微卷", "/avatars/avatar-12.jpg"),
            new AvatarItem("avatar-13", "紫夜", "紫调少年", "/avatars/avatar-13.jpg"),
            new AvatarItem("avatar-14", "微风", "浅紫短发", "/avatars/avatar-14.jpg"),
            new AvatarItem("avatar-15", "星环", "颈间星环", "/avatars/avatar-15.jpg"),
            new AvatarItem("avatar-16", "粉蓝", "粉蓝夹克", "/avatars/avatar-16.jpg"),
            new AvatarItem("avatar-17", "墨镜", "镜片少年", "/avatars/avatar-17.jpg"),
            new AvatarItem("avatar-18", "青衫", "青蓝卫衣", "/avatars/avatar-18.jpg"),
            new AvatarItem("avatar-19", "晴日", "晴日少年", "/avatars/avatar-19.jpg"),
            new AvatarItem("avatar-20", "暮色", "暮色长发", "/avatars/avatar-20.jpg"),
            new AvatarItem("avatar-21", "海盐", "海盐少年", "/avatars/avatar-21.jpg"),
            new AvatarItem("avatar-22", "电波", "电波耳机", "/avatars/avatar-22.jpg"),
            new AvatarItem("avatar-23", "晚樱", "晚樱少年", "/avatars/avatar-23.jpg")
    );

    private static final Map<String, AvatarItem> CATALOG_BY_ID = CATALOG.stream()
            .collect(Collectors.toUnmodifiableMap(AvatarItem::id, item -> item));

    private final UserRepository userRepository;
    private final UserAvatarUnlockRepository unlockRepository;
    private final UserPointRepository userPointRepository;
    private final UserGrowthService userGrowthService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Map<String, Object> getShop(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("用户不存在"));
        int points = userPointRepository.findByUserId(userId)
                .map(UserPoint::getPoints)
                .orElse(0);
        Set<String> ownedIds = unlockRepository.findByUserId(userId).stream()
                .map(UserAvatarUnlock::getAvatarId)
                .collect(Collectors.toSet());

        List<Map<String, Object>> avatars = CATALOG.stream().map(item -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", item.id());
            map.put("name", item.name());
            map.put("tagline", item.tagline());
            map.put("imageUrl", item.imageUrl());
            map.put("price", AVATAR_PRICE);
            map.put("owned", ownedIds.contains(item.id()));
            map.put("equipped", item.imageUrl().equals(user.getAvatar()));
            return map;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("points", points);
        result.put("price", AVATAR_PRICE);
        result.put("ownedCount", ownedIds.size());
        result.put("avatars", avatars);
        return result;
    }

    @Transactional
    public Map<String, Object> purchase(Long userId, String avatarId) {
        AvatarItem item = findItem(avatarId);
        if (unlockRepository.existsByUserIdAndAvatarId(userId, avatarId)) {
            throw new BadRequestException("你已经拥有这个头像了");
        }

        // 先确保积分账户存在，再锁定该行，避免并发重复扣减
        userGrowthService.getUserPoints(userId);
        UserPoint point = userPointRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new BadRequestException("积分账户异常"));
        if (point.getPoints() < AVATAR_PRICE) {
            throw new BadRequestException("积分不足，还差 " + (AVATAR_PRICE - point.getPoints()) + " 积分");
        }

        userGrowthService.addPoints(userId, "AVATAR_PURCHASE", -AVATAR_PRICE, "兑换头像：" + item.name());

        UserAvatarUnlock unlock = new UserAvatarUnlock();
        unlock.setUserId(userId);
        unlock.setAvatarId(avatarId);
        try {
            unlockRepository.saveAndFlush(unlock);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("你已经拥有这个头像了");
        }

        UserUpdateDto update = new UserUpdateDto();
        update.setAvatar(item.imageUrl());
        userService.updateUser(userId, update);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("avatarId", item.id());
        result.put("name", item.name());
        result.put("tagline", item.tagline());
        result.put("avatar", item.imageUrl());
        result.put("points", point.getPoints());
        return result;
    }

    @Transactional
    public Map<String, Object> equip(Long userId, String avatarId) {
        AvatarItem item = findItem(avatarId);
        if (!unlockRepository.existsByUserIdAndAvatarId(userId, avatarId)) {
            throw new BadRequestException("请先兑换这个头像");
        }

        UserUpdateDto update = new UserUpdateDto();
        update.setAvatar(item.imageUrl());
        userService.updateUser(userId, update);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("avatarId", item.id());
        result.put("avatar", item.imageUrl());
        return result;
    }

    private AvatarItem findItem(String avatarId) {
        AvatarItem item = CATALOG_BY_ID.get(avatarId);
        if (item == null) {
            throw new BadRequestException("头像不存在");
        }
        return item;
    }

    private record AvatarItem(String id, String name, String tagline, String imageUrl) {
    }
}
