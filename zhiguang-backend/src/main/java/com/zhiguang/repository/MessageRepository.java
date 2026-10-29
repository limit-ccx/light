package com.zhiguang.repository;

import com.zhiguang.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * 幂等查询：clientMessageId 必须限定发送者属主。
     * 此前为全局唯一查找——带上他人的 clientMessageId 调发送接口会直接返回他人的消息（IDOR，M-1）。
     */
    Optional<Message> findByClientMessageIdAndSenderId(String clientMessageId, Long senderId);

    /**
     * 发送方向的历史消息总数（忽略删除标记，防止"删了再发"绕过陌生人单条限制）。
     */
    long countBySenderIdAndReceiverId(Long senderId, Long receiverId);

    /**
     * "对该用户可见"的过滤条件：消息未被发送者删除（或发送者不是该用户）、未被接收者删除（或接收者不是该用户）。
     * deletedFor* 为原始 boolean 非空列，旧行 DDL 迁移后为 false，无需判 NULL。
     */
    String VISIBLE_FOR = """
            ((m.sender.id = :userId AND m.deletedForSender = false) OR (m.receiver.id = :userId AND m.deletedForReceiver = false))
            """;

    @EntityGraph(attributePaths = {"sender", "receiver"})
    @Query("SELECT m FROM Message m WHERE " +
           "((m.sender.id = :user1 AND m.receiver.id = :user2) OR (m.sender.id = :user2 AND m.receiver.id = :user1)) " +
           "AND ((m.sender.id = :me AND m.deletedForSender = false) OR (m.receiver.id = :me AND m.deletedForReceiver = false))")
    Page<Message> findConversation(@Param("user1") Long user1, @Param("user2") Long user2,
                                   @Param("me") Long me, Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver.id = :userId AND m.isRead = false AND m.deletedForReceiver = false")
    long countUnreadByReceiverId(@Param("userId") Long userId);

    /** 按发送者分组的未读数（会话列表红点，M-4），返回 [senderId, count] */
    @Query("SELECT m.sender.id, COUNT(m) FROM Message m " +
           "WHERE m.receiver.id = :userId AND m.isRead = false AND m.deletedForReceiver = false " +
           "GROUP BY m.sender.id")
    List<Object[]> countUnreadGroupBySender(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.sender.id = :otherId AND m.receiver.id = :userId AND m.isRead = false")
    void markConversationAsRead(@Param("userId") Long userId, @Param("otherId") Long otherId);

    /** 每个伙伴的最新一条"对我可见"消息（会话列表，M-7：EntityGraph 消除懒加载 N+1） */
    @EntityGraph(attributePaths = {"sender", "receiver"})
    @Query("SELECT m FROM Message m WHERE m.id IN (" +
           "  SELECT MAX(sub.id) FROM Message sub " +
           "  WHERE (sub.sender.id = :userId OR sub.receiver.id = :userId) " +
           "  AND ((sub.sender.id = :userId AND sub.deletedForSender = false) OR (sub.receiver.id = :userId AND sub.deletedForReceiver = false)) " +
           "  GROUP BY CASE WHEN sub.sender.id = :userId THEN sub.receiver.id ELSE sub.sender.id END" +
           ") ORDER BY m.createdAt DESC")
    List<Message> findLatestMessagesByUserId(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"sender", "receiver"})
    @Query("SELECT m FROM Message m WHERE " + VISIBLE_FOR + " AND m.content LIKE %:keyword%")
    Page<Message> searchMessages(@Param("userId") Long userId, @Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"sender", "receiver"})
    @Query("SELECT m FROM Message m WHERE ((m.sender.id = :userId AND m.receiver.id = :partnerId) " +
           "OR (m.sender.id = :partnerId AND m.receiver.id = :userId)) " +
           "AND ((m.sender.id = :userId AND m.deletedForSender = false) OR (m.receiver.id = :userId AND m.deletedForReceiver = false)) " +
           "AND m.content LIKE %:keyword%")
    Page<Message> searchConversationMessages(@Param("userId") Long userId, @Param("partnerId") Long partnerId,
                                             @Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"sender", "receiver"})
    @Query("SELECT m FROM Message m WHERE ((m.sender.id = :userId AND m.receiver.id = :senderId) " +
           "OR (m.sender.id = :senderId AND m.receiver.id = :userId)) " +
           "AND ((m.sender.id = :userId AND m.deletedForSender = false) OR (m.receiver.id = :userId AND m.deletedForReceiver = false))")
    Page<Message> searchMessagesBySender(@Param("userId") Long userId, @Param("senderId") Long senderId, Pageable pageable);

    /** 向上翻页（M-8：由 native 改 JPQL，可配 EntityGraph，避免每行懒加载） */
    @EntityGraph(attributePaths = {"sender", "receiver"})
    @Query("SELECT m FROM Message m WHERE ((m.sender.id = :userId AND m.receiver.id = :partnerId) " +
           "OR (m.sender.id = :partnerId AND m.receiver.id = :userId)) " +
           "AND ((m.sender.id = :userId AND m.deletedForSender = false) OR (m.receiver.id = :userId AND m.deletedForReceiver = false)) " +
           "AND m.createdAt < :before")
    Page<Message> findMessagesBefore(@Param("userId") Long userId, @Param("partnerId") Long partnerId,
                                     @Param("before") LocalDateTime before, Pageable pageable);
}
