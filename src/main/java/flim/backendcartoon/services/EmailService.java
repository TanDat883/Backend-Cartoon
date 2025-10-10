package flim.backendcartoon.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender emailSender;

    @Autowired
    public EmailService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    /** Gửi email HTML cơ bản */
    public void sendMessage(String from, String to, String subject, String html) {
        MimeMessage message = emailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from != null && !from.isBlank() ? from : "Cartoon Support <no-reply@cartoon.app>");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true); // HTML
            emailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Send mail error: " + e.getMessage(), e);
        }
    }

    /** Tiện ích: gửi mail xác nhận thanh toán thành công */
    public void sendPaymentSuccess(String toEmail,
                                   String userName,
                                   String orderCode,
                                   String packageName,
                                   long durationDays,
                                   long amount,
                                   String startDateIso,
                                   String endDateIso) {

        String subject = "Xác nhận thanh toán thành công #" + orderCode;

        // HTML đơn giản, không phụ thuộc Thymeleaf
        String html = """
            <div style="font-family:Segoe UI,Arial,sans-serif;max-width:600px;margin:auto">
              <h2>🎉 Thanh toán thành công</h2>
              <p>Chào %s,</p>
              <p>Bạn đã thanh toán thành công đơn hàng <b>#%s</b>.</p>

              <table style="width:100%%;border-collapse:collapse">
                <tr>
                  <td style="border:1px solid #eee;padding:8px">Gói</td>
                  <td style="border:1px solid #eee;padding:8px"><b>%s</b></td>
                </tr>
                <tr>
                  <td style="border:1px solid #eee;padding:8px">Thời hạn</td>
                  <td style="border:1px solid #eee;padding:8px">%d ngày</td>
                </tr>
                <tr>
                  <td style="border:1px solid #eee;padding:8px">Ngày bắt đầu</td>
                  <td style="border:1px solid #eee;padding:8px">%s</td>
                </tr>
                <tr>
                  <td style="border:1px solid #eee;padding:8px">Ngày kết thúc</td>
                  <td style="border:1px solid #eee;padding:8px">%s</td>
                </tr>
                <tr>
                  <td style="border:1px solid #eee;padding:8px">Số tiền</td>
                  <td style="border:1px solid #eee;padding:8px"><b>%s ₫</b></td>
                </tr>
              </table>

              <p>Nếu bạn không thực hiện giao dịch này, vui lòng phản hồi lại email.</p>
              <p>— Cartoon Team</p>
            </div>
            """.formatted(
                safe(userName),
                orderCode,
                safe(packageName),
                durationDays,
                safe(startDateIso),
                safe(endDateIso),
                formatVnd(amount)
        );

        sendMessage(null, toEmail, subject, html);
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static String formatVnd(long amount) {
        // 1_000_000 -> 1,000,000 (đơn giản)
        return String.format("%,d", amount).replace(',', '.');
    }
}
