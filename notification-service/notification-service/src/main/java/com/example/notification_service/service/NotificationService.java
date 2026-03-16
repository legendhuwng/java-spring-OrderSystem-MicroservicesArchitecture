// NotificationService.java
package com.example.notification_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    public void notifyOrderCreated(String orderId) {
        // Thực tế: gửi email "Đơn hàng của bạn đã được tạo"
        log.info("📧 [NOTIFY] Order created - orderId={}", orderId);
    }

    public void notifyInventoryReserved(String orderId) {
        log.info("📧 [NOTIFY] Inventory reserved - orderId={}", orderId);
    }

    public void notifyInventoryFailed(String orderId, String reason) {
        // Thực tế: gửi email "Xin lỗi, sản phẩm hết hàng"
        log.info("📧 [NOTIFY] Inventory failed - orderId={}, reason={}", orderId, reason);
    }

    public void notifyPaymentSuccess(String orderId) {
        // Thực tế: gửi email "Thanh toán thành công"
        log.info("📧 [NOTIFY] Payment success - orderId={}", orderId);
    }

    public void notifyPaymentFailed(String orderId, String reason) {
        // Thực tế: gửi email "Thanh toán thất bại, vui lòng thử lại"
        log.info("📧 [NOTIFY] Payment failed - orderId={}, reason={}", orderId, reason);
    }

    public void notifyShippingCreated(String orderId, String trackingNumber) {
        // Thực tế: gửi email "Đơn hàng đang được giao, tracking: TRACK-XXXX"
        log.info("📧 [NOTIFY] Shipping created - orderId={}, tracking={}", orderId, trackingNumber);
    }
}