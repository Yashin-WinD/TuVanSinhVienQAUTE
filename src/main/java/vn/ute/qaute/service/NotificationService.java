package vn.ute.qaute.service;

import vn.ute.qaute.entity.Notification;
import vn.ute.qaute.entity.User;

import java.util.List;

public interface NotificationService {
    Notification sendNotification(User recipient, String title, String content, String targetUrl);
    List<Notification> getNotificationsForUser(Long userId);
    void markAsRead(Long notificationId);
    long getUnreadCount(Long userId);
}
