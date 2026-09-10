package com.project.airbnb.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.project.airbnb.entity.DatCho;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    /**
     * Gửi OTP qua email thật. Trả false nếu tắt mail hoặc SMTP lỗi (OTP vẫn có thể đã lưu DB).
     *
     * @return
     */
    @SuppressWarnings("null")
    public boolean sendOtpEmail(String to, String otpCode) {
        try {
            if (mailSender == null) {
                log.warn("Mail sender not configured. OTP email for {} would be: {}", to, otpCode);
                return false;
            }
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Mã xác nhận email của bạn - Airbnb Homestay");

            String htmlContent = buildOtpEmailContent(otpCode);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("OTP email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Error sending OTP email to {}: {}", to, e.getMessage());
            log.warn("Email sending failed, but continuing. OTP code: {}", otpCode);
        }
        return false;
    }

    @SuppressWarnings("null")
    public void sendWelcomeEmail(String to, String hoTen) {
        try {
            if (mailSender == null) {
                log.warn("Mail sender not configured. Welcome email for {} would be sent.", to);
                return;
            }
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Chào mừng bạn đến với Airbnb Homestay!");

            String htmlContent = buildWelcomeEmailContent(hoTen);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Error sending welcome email to {}: {}", to, e.getMessage());
            log.warn("Email sending failed, but continuing.");
        }
    }

    @SuppressWarnings("null")
    public void sendVerificationEmail(String to, String verificationToken) {
        try {
            if (mailSender == null) {
                log.warn("Mail sender not configured. Verification email for {} would be sent with token: {}", to,
                        verificationToken);
                return;
            }
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Xác nhận email của bạn - Airbnb Homestay");

            String verificationUrl = frontendUrl + "/verify-email?token=" + verificationToken;
            String htmlContent = buildVerificationEmailContent(verificationUrl);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Verification email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Error sending verification email to {}: {}", to, e.getMessage());
            log.warn("Email sending failed, but continuing.");
        }
    }

    @SuppressWarnings("null")
    public void sendAccountLockedEmail(String to, String hoTen, String reason) {
        try {
            if (mailSender == null) return;
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Tài khoản của bạn đã bị khóa - Airbnb Homestay");

            String htmlContent = buildAccountLockedEmailContent(hoTen, reason);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Account locked email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Error sending account locked email to {}: {}", to, e.getMessage());
        }
    }

    @SuppressWarnings("null")
    public void sendAccountUnlockedEmail(String to, String hoTen) {
        try {
            if (mailSender == null) return;
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Tài khoản của bạn đã được mở khóa - Airbnb Homestay");

            String htmlContent = buildAccountUnlockedEmailContent(hoTen);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Account unlocked email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Error sending account unlocked email to {}: {}", to, e.getMessage());
        }
    }

    @SuppressWarnings("null")
    public void sendRoleChangedEmail(String to, String hoTen, boolean isHost) {
        try {
            if (mailSender == null) return;
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Vai trò của bạn đã được cập nhật - Airbnb Homestay");

            String htmlContent = buildRoleChangedEmailContent(hoTen, isHost);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Role changed email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Error sending role changed email to {}: {}", to, e.getMessage());
        }
    }

    @SuppressWarnings("null")
    public void sendKycVerifiedEmail(String to, String hoTen) {
        try {
            if (mailSender == null) return;
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Xác minh danh tính thành công - Airbnb Homestay");

            String htmlContent = buildKycVerifiedEmailContent(hoTen);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("KYC verified email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Error sending KYC verified email to {}: {}", to, e.getMessage());
        }
    }

    @SuppressWarnings("null")
    public void sendKycRevokedEmail(String to, String hoTen) {
        try {
            if (mailSender == null) return;
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Cập nhật xác minh danh tính - Airbnb Homestay");

            String htmlContent = buildKycRevokedEmailContent(hoTen);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("KYC revoked email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Error sending KYC revoked email to {}: {}", to, e.getMessage());
        }
    }

    @SuppressWarnings("null")
    public void sendHostApprovedEmail(String to, String hoTen) {
        try {
            if (mailSender == null) return;
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Chúc mừng bạn đã trở thành Chủ nhà - Airbnb Homestay");

            String htmlContent = buildHostApprovedEmailContent(hoTen);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Host approved email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Error sending host approved email to {}: {}", to, e.getMessage());
        }
    }

    @SuppressWarnings("null")
    public void sendHostRejectedEmail(String to, String hoTen) {
        try {
            if (mailSender == null) return;
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Yêu cầu trở thành Chủ nhà đã bị từ chối - Airbnb Homestay");

            String htmlContent = buildHostRejectedEmailContent(hoTen);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Host rejected email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Error sending host rejected email to {}: {}", to, e.getMessage());
        }
    }

    @SuppressWarnings("null")
    public void sendHostRevokedEmail(String to, String hoTen) {
        try {
            if (mailSender == null) return;
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Cập nhật vai trò tài khoản - Airbnb Homestay");

            String htmlContent = buildHostRevokedEmailContent(hoTen);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Host revoked email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Error sending host revoked email to {}: {}", to, e.getMessage());
        }
    }

    private String buildOtpEmailContent(String otpCode) {
        return "<!DOCTYPE html>" +
                "<html lang='vi'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Helvetica Neue\", Arial, sans-serif; background-color: #f7f7f7;'>"
                +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background-color: #f7f7f7; padding: 40px 20px;'>"
                +
                "<tr>" +
                "<td align='center'>" +
                "<table width='600' cellpadding='0' cellspacing='0' style='background-color: #ffffff; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);'>"
                +
                "<!-- Header -->" +
                "<tr>" +
                "<td style='padding: 40px 40px 20px; text-align: center; border-bottom: 1px solid #ebebeb;'>" +
                "<div style='width: 48px; height: 48px; background: linear-gradient(135deg, #FF385C 0%, #BD1E59 100%); border-radius: 50%; margin: 0 auto 16px; display: flex; align-items: center; justify-content: center;'>"
                +
                "<span style='color: #ffffff; font-size: 20px; font-weight: bold;'>A</span>" +
                "</div>" +
                "<h1 style='margin: 0; color: #222222; font-size: 28px; font-weight: 600;'>Mã xác nhận</h1>" +
                "</td>" +
                "</tr>" +
                "<!-- Content -->" +
                "<tr>" +
                "<td style='padding: 40px; text-align: center;'>" +
                "<p style='margin: 0 0 24px; color: #717171; font-size: 16px; line-height: 1.5;'>Vui lòng sử dụng mã sau để xác nhận tài khoản của bạn:</p>"
                +
                "<div style='background: linear-gradient(135deg, #FF385C 0%, #BD1E59 100%); border-radius: 12px; padding: 32px; margin: 32px 0; box-shadow: 0 4px 12px rgba(255, 56, 92, 0.2);'>"
                +
                "<div style='color: #ffffff; font-size: 48px; font-weight: 700; letter-spacing: 8px; font-family: \"Courier New\", monospace;'>"
                +
                otpCode +
                "</div>" +
                "</div>" +
                "<p style='margin: 24px 0 0; color: #717171; font-size: 14px; line-height: 1.5;'>Mã này sẽ hết hạn sau <strong style='color: #222222;'>10 phút</strong>.</p>"
                +
                "</td>" +
                "</tr>" +
                "<!-- Footer -->" +
                "<tr>" +
                "<td style='padding: 32px 40px; background-color: #f7f7f7; border-top: 1px solid #ebebeb; border-radius: 0 0 12px 12px;'>"
                +
                "<p style='margin: 0 0 16px; color: #717171; font-size: 13px; line-height: 1.5; text-align: center;'>Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email này.</p>"
                +
                "<p style='margin: 0; color: #717171; font-size: 13px; line-height: 1.5; text-align: center;'>Đây là email tự động, vui lòng không trả lời email này.</p>"
                +
                "</td>" +
                "</tr>" +
                "</table>" +
                "<p style='margin: 24px 0 0; color: #717171; font-size: 12px; text-align: center;'>© 2026 Airbnb Homestay, Inc. Tất cả các quyền được bảo lưu.</p>"
                +
                "</td>" +
                "</tr>" +
                "</table>" +
                "</body>" +
                "</html>";
    }

    private String buildWelcomeEmailContent(String hoTen) {
        String ten = hoTen != null ? hoTen.split(" ")[hoTen.split(" ").length - 1] : "bạn";

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #222;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h1 style='color: #FF385C;'>Chào mừng đến với Airbnb Homestay!</h1>" +
                "<p>Xin chào " + ten + ",</p>" +
                "<p>Chúng tôi rất vui được chào đón bạn đến với Airbnb Homestay!</p>" +
                "<p>Bây giờ bạn có thể khám phá những nơi lưu trú độc đáo, trải nghiệm thú vị và dịch vụ đặc biệt trên toàn thế giới.</p>"
                +
                "<div style='margin: 30px 0;'>" +
                "<a href='" + frontendUrl
                + "' style='background: #FF385C; color: white; padding: 15px 30px; text-decoration: none; border-radius: 8px; display: inline-block; font-weight: bold;'>Bắt đầu khám phá</a>"
                +
                "</div>" +
                "<p>Nếu bạn có bất kỳ câu hỏi nào, chúng tôi luôn sẵn sàng hỗ trợ!</p>" +
                "<p>Trân trọng,<br>Đội ngũ Airbnb Homestay</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String buildVerificationEmailContent(String verificationUrl) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #222;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h1 style='color: #FF385C;'>Xác nhận email của bạn</h1>" +
                "<p>Vui lòng click vào nút bên dưới để xác nhận địa chỉ email của bạn:</p>" +
                "<div style='margin: 30px 0;'>" +
                "<a href='" + verificationUrl
                + "' style='background: #FF385C; color: white; padding: 15px 30px; text-decoration: none; border-radius: 8px; display: inline-block; font-weight: bold;'>Xác nhận email</a>"
                +
                "</div>" +
                "<p>Hoặc copy và paste link này vào trình duyệt:</p>" +
                "<p style='color: #666; word-break: break-all;'>" + verificationUrl + "</p>" +
                "<p>Link này sẽ hết hạn sau 24 giờ.</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String buildAccountLockedEmailContent(String hoTen, String reason) {
        String ten = hoTen != null ? hoTen.split(" ")[hoTen.split(" ").length - 1] : "bạn";
        return "<!DOCTYPE html>" +
                "<html lang='vi'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Helvetica Neue\", Arial, sans-serif; background-color: #f7f7f7;'>"
                +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background-color: #f7f7f7; padding: 40px 20px;'>"
                +
                "<tr><td align='center'>" +
                "<table width='600' cellpadding='0' cellspacing='0' style='background-color: #ffffff; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);'>"
                +
                "<!-- Header -->" +
                "<tr><td style='padding: 40px 40px 20px; text-align: center; border-bottom: 1px solid #ebebeb;'>" +
                "<div style='width: 48px; height: 48px; background: linear-gradient(135deg, #FF385C 0%, #BD1E59 100%); border-radius: 50%; margin: 0 auto 16px; display: flex; align-items: center; justify-content: center;'>"
                +
                "<span style='color: #ffffff; font-size: 20px; font-weight: bold;'>A</span>" +
                "</div>" +
                "<h1 style='margin: 0; color: #222222; font-size: 28px; font-weight: 600;'>Tài khoản đã bị khóa</h1>" +
                "</td></tr>" +
                "<!-- Content -->" +
                "<tr><td style='padding: 40px;'>" +
                "<p style='margin: 0 0 16px; color: #222; font-size: 16px; line-height: 1.5;'>Xin chào <strong>" + ten + "</strong>,</p>"
                +
                "<p style='margin: 0 0 24px; color: #717171; font-size: 16px; line-height: 1.5;'>Tài khoản Airbnb Homestay của bạn đã bị khóa bởi quản trị viên vì vi phạm điều khoản dịch vụ.</p>"
                +
                "<div style='background: #FEF2F2; border: 1px solid #FECACA; border-radius: 12px; padding: 24px; margin: 24px 0;'>"
                +
                "<div style='width: 40px; height: 40px; background: #FEE2E2; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin-bottom: 12px;'>"
                +
                "<svg width='20' height='20' viewBox='0 0 24 24' fill='none' stroke='#DC2626' stroke-width='2'><circle cx='12' cy='12' r='10'/><line x1='15' y1='9' x2='9' y2='15'/><line x1='9' y1='9' x2='15' y2='15'/></svg>"
                +
                "</div>" +
                "<p style='margin: 0 0 8px; font-weight: 600; color: #991B1B; font-size: 14px;'>Lý do khóa tài khoản:</p>"
                +
                "<p style='margin: 0; color: #7F1D1D; font-size: 15px; line-height: 1.6;'>" + reason + "</p>" +
                "</div>" +
                "<p style='margin: 0 0 24px; color: #717171; font-size: 14px; line-height: 1.5;'>Nếu bạn cho rằng đây là nhầm lẫn hoặc cần hỗ trợ thêm, vui lòng liên hệ với đội ngũ hỗ trợ của chúng tôi.</p>"
                +
                "<div style='background: #f7f7f7; border-radius: 8px; padding: 16px; margin-top: 24px;'>" +
                "<p style='margin: 0; color: #717171; font-size: 13px; line-height: 1.5; text-align: center;'>Đây là email tự động, vui lòng không trả lời email này.</p>"
                +
                "</div>" +
                "</td></tr>" +
                "<!-- Footer -->" +
                "<tr><td style='padding: 32px 40px; background-color: #f7f7f7; border-top: 1px solid #ebebeb; border-radius: 0 0 12px 12px; text-align: center;'>"
                +
                "<p style='margin: 0 0 4px; color: #FF385C; font-size: 14px; font-weight: 600;'>Airbnb Homestay</p>"
                +
                "<p style='margin: 0; color: #717171; font-size: 12px;'>© 2026 Airbnb Homestay, Inc. Tất cả các quyền được bảo lưu.</p>"
                +
                "</td></tr>" +
                "</table>" +
                "</td></tr>" +
                "</table>" +
                "</body>" +
                "</html>";
    }    private String buildAccountUnlockedEmailContent(String hoTen) {
        String ten = hoTen != null ? hoTen.split(" ")[hoTen.split(" ").length - 1] : "bạn";
        return "<!DOCTYPE html>" +
                "<html lang='vi'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Helvetica Neue\", Arial, sans-serif; background-color: #f7f7f7;'>"
                +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background-color: #f7f7f7; padding: 40px 20px;'>"
                +
                "<tr><td align='center'>" +
                "<table width='600' cellpadding='0' cellspacing='0' style='background-color: #ffffff; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);'>"
                +
                "<!-- Header -->" +
                "<tr><td style='padding: 40px 40px 20px; text-align: center; border-bottom: 1px solid #ebebeb;'>" +
                "<div style='width: 48px; height: 48px; background: linear-gradient(135deg, #10B981 0%, #059669 100%); border-radius: 50%; margin: 0 auto 16px; display: flex; align-items: center; justify-content: center;'>"
                +
                "<svg width='24' height='24' viewBox='0 0 24 24' fill='none' stroke='#ffffff' stroke-width='2.5'><polyline points='20 6 9 17 4 12'/></svg>"
                +
                "</div>" +
                "<h1 style='margin: 0; color: #222222; font-size: 28px; font-weight: 600;'>Tài khoản đã được mở khóa</h1>" +
                "</td></tr>" +
                "<!-- Content -->" +
                "<tr><td style='padding: 40px;'>" +
                "<p style='margin: 0 0 16px; color: #222; font-size: 16px; line-height: 1.5;'>Xin chào <strong>" + ten + "</strong>,</p>"
                +
                "<p style='margin: 0 0 24px; color: #717171; font-size: 16px; line-height: 1.5;'>Tài khoản Airbnb Homestay của bạn đã được mở khóa. Bạn có thể đăng nhập và sử dụng dịch vụ bình thường.</p>"
                +
                "<div style='background: #F0FDF4; border: 1px solid #BBF7D0; border-radius: 12px; padding: 24px; margin: 24px 0; text-align: center;'>"
                +
                "<p style='margin: 0 0 16px; color: #166534; font-size: 14px; font-weight: 600;'>Truy cập ngay để tiếp tục trải nghiệm</p>"
                +
                "<a href='" + frontendUrl
                + "' style='display: inline-block; background: #10B981; color: #ffffff; padding: 14px 32px; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 15px;'>Đăng nhập ngay</a>"
                +
                "</div>" +
                "<p style='margin: 24px 0 0; color: #717171; font-size: 14px; line-height: 1.5;'>Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi. Chúng tôi rất mong được chào đón bạn trở lại!</p>"
                +
                "</td></tr>" +
                "<!-- Footer -->" +
                "<tr><td style='padding: 32px 40px; background-color: #f7f7f7; border-top: 1px solid #ebebeb; border-radius: 0 0 12px 12px; text-align: center;'>"
                +
                "<p style='margin: 0 0 4px; color: #10B981; font-size: 14px; font-weight: 600;'>Airbnb Homestay</p>"
                +
                "<p style='margin: 0; color: #717171; font-size: 12px;'>© 2026 Airbnb Homestay, Inc. Tất cả các quyền được bảo lưu.</p>"
                +
                "</td></tr>" +
                "</table>" +
                "</td></tr>" +
                "</table>" +
                "</body>" +
                "</html>";
    }

    @SuppressWarnings("null")
    public void sendBookingPendingEmail(String to, String guestName, DatCho booking, boolean isWalletPayment,
            BigDecimal newBalance) {
        try {
            if (mailSender == null)
                return;
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Yêu cầu đặt phòng của bạn đã được gửi - Airbnb Homestay");

            String htmlContent = buildBookingPendingEmailContent(guestName, booking, isWalletPayment, newBalance);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Booking pending email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Error sending booking pending email to {}: {}", to, e.getMessage());
        }
    }

    @SuppressWarnings("null")
    public void sendBookingConfirmedEmail(String to, String guestName, DatCho booking) {
        try {
            if (mailSender == null)
                return;
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Đặt phòng đã được xác nhận! Mã check-in của bạn - Airbnb Homestay");

            String htmlContent = buildBookingConfirmedEmailContent(guestName, booking);
            helper.setText(htmlContent, true);

            // Generate and attach QR Code
            // QR Content: Detailed Text Note with room pass
            String passPhong = booking.getPhong().getPassPhong() != null ? booking.getPhong().getPassPhong() : "";
            String qrContent = String.format(
                    "THÔNG TIN ĐẶT PHÒNG / BOOKING INFO\n" +
                            "--------------------------------\n" +
                            "Homestay: %s\n" +
                            "Khách/Guest: %s\n" +
                            "Mã/Code: %d\n" +
                            "Ngày/Date: %s - %s\n" +
                            "Số khách/Guests: %d\n" +
                            "Đ/c: %s, %s\n" +
                            "--------------------------------\n" +
                            "PASS PHÒNG / ROOM PASS: %s",
                    booking.getPhong().getTieuDe(),
                    guestName,
                    booking.getMaDatCho(),
                    booking.getNgayNhanPhong(),
                    booking.getNgayTraPhong(),
                    booking.getSoLuongKhach(),
                    booking.getPhong().getDiaChiDayDu(),
                    booking.getPhong().getThanhPho(),
                    passPhong);

            byte[] qrImage = generateQRCodeImage(qrContent);
            if (qrImage != null) {
                helper.addInline("qrCodeImage", new ByteArrayResource(qrImage), "image/png");
            }

            mailSender.send(message);
            log.info("Booking confirmed email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Error sending booking confirmed email to {}: {}", to, e.getMessage());
        }
    }

    private String buildRoleChangedEmailContent(String hoTen, boolean isHost) {
        String ten = hoTen != null ? hoTen.split(" ")[hoTen.split(" ").length - 1] : "bạn";
        String roleText = isHost ? "Chủ nhà (Host)" : "Khách (Guest)";
        String roleDesc = isHost
                ? "Bạn có thể tạo và quản lý các listing cho thuê, nhận đặt chỗ và kiếm thu nhập từ việc cho thuê chỗ ở."
                : "Bạn có thể đặt phòng, trải nghiệm dịch vụ và khám phá những nơi lưu trú tuyệt vời trên Airbnb Homestay.";

        String iconSvg = isHost
                ? "<svg width='24' height='24' viewBox='0 0 24 24' fill='none' stroke='#ffffff' stroke-width='2'><path d='M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z'/><polyline points='9 22 9 12 15 12 15 22'/></svg>"
                : "<svg width='24' height='24' viewBox='0 0 24 24' fill='none' stroke='#ffffff' stroke-width='2'><path d='M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2'/><circle cx='12' cy='7' r='4'/></svg>";

        String gradientColor = isHost ? "#F59E0B, #D97706" : "#FF385C, #BD1E59";

        return "<!DOCTYPE html>" +
                "<html lang='vi'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Helvetica Neue\", Arial, sans-serif; background-color: #f7f7f7;'>"
                +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background-color: #f7f7f7; padding: 40px 20px;'>"
                +
                "<tr><td align='center'>" +
                "<table width='600' cellpadding='0' cellspacing='0' style='background-color: #ffffff; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);'>"
                +
                "<!-- Header -->" +
                "<tr><td style='padding: 40px 40px 20px; text-align: center; border-bottom: 1px solid #ebebeb;'>" +
                "<div style='width: 48px; height: 48px; background: linear-gradient(135deg, " + gradientColor + "); border-radius: 50%; margin: 0 auto 16px; display: flex; align-items: center; justify-content: center;'>"
                +
                iconSvg +
                "</div>" +
                "<h1 style='margin: 0; color: #222222; font-size: 28px; font-weight: 600;'>Vai trò đã được cập nhật</h1>"
                +
                "</td></tr>" +
                "<!-- Content -->" +
                "<tr><td style='padding: 40px;'>" +
                "<p style='margin: 0 0 16px; color: #222; font-size: 16px; line-height: 1.5;'>Xin chào <strong>" + ten + "</strong>,</p>"
                +
                "<p style='margin: 0 0 24px; color: #717171; font-size: 16px; line-height: 1.5;'>Vai trò của bạn trên Airbnb Homestay đã được quản trị viên cập nhật.</p>"
                +
                "<div style='background: #F7F7F7; border: 1px solid #EBEBEB; border-radius: 12px; padding: 24px; margin: 24px 0; text-align: center;'>"
                +
                "<div style='display: inline-block; padding: 8px 24px; border-radius: 20px; font-size: 18px; font-weight: 700; color: #ffffff; background: linear-gradient(135deg, " + gradientColor + "); margin-bottom: 16px;'>"
                +
                roleText +
                "</div>" +
                "<p style='margin: 0; color: #717171; font-size: 15px; line-height: 1.6;'>" + roleDesc + "</p>" +
                "</div>" +
                "<p style='margin: 0 0 24px; color: #717171; font-size: 14px; line-height: 1.5;'>Để biết thêm chi tiết, vui lòng đăng nhập vào tài khoản của bạn và kiểm tra thông tin.</p>"
                +
                "<a href='" + frontendUrl
                + "' style='display: block; text-align: center; background: #222222; color: #ffffff; padding: 14px 32px; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 15px;'>Đăng nhập tài khoản</a>"
                +
                "</td></tr>" +
                "<!-- Footer -->" +
                "<tr><td style='padding: 32px 40px; background-color: #f7f7f7; border-top: 1px solid #ebebeb; border-radius: 0 0 12px 12px; text-align: center;'>"
                +
                "<p style='margin: 0 0 4px; color: #222222; font-size: 14px; font-weight: 600;'>Airbnb Homestay</p>"
                +
                "<p style='margin: 0; color: #717171; font-size: 12px;'>© 2026 Airbnb Homestay, Inc. Tất cả các quyền được bảo lưu.</p>"
                +
                "</td></tr>" +
                "</table>" +
                "</td></tr>" +
                "</table>" +
                "</body>" +
                "</html>";
    }

    private String buildKycVerifiedEmailContent(String hoTen) {
        String ten = hoTen != null ? hoTen.split(" ")[hoTen.split(" ").length - 1] : "bạn";
        return "<!DOCTYPE html>" +
                "<html lang='vi'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Helvetica Neue\", Arial, sans-serif; background-color: #f7f7f7;'>"
                +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background-color: #f7f7f7; padding: 40px 20px;'>"
                +
                "<tr><td align='center'>" +
                "<table width='600' cellpadding='0' cellspacing='0' style='background-color: #ffffff; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);'>"
                +
                "<!-- Header -->" +
                "<tr><td style='padding: 40px 40px 20px; text-align: center; border-bottom: 1px solid #ebebeb;'>" +
                "<div style='width: 56px; height: 56px; background: linear-gradient(135deg, #3B82F6 0%, #1D4ED8 100%); border-radius: 50%; margin: 0 auto 16px; display: flex; align-items: center; justify-content: center; box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);'>"
                +
                "<svg width='28' height='28' viewBox='0 0 24 24' fill='none' stroke='#ffffff' stroke-width='2.5' stroke-linecap='round' stroke-linejoin='round'><path d='M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z'/><polyline points='9 12 11 14 15 10'/></svg>"
                +
                "</div>" +
                "<h1 style='margin: 0; color: #222222; font-size: 28px; font-weight: 600;'>Xác minh danh tính thành công</h1>"
                +
                "</td></tr>" +
                "<!-- Content -->" +
                "<tr><td style='padding: 40px;'>" +
                "<p style='margin: 0 0 16px; color: #222; font-size: 16px; line-height: 1.5;'>Xin chào <strong>" + ten + "</strong>,</p>"
                +
                "<p style='margin: 0 0 24px; color: #717171; font-size: 16px; line-height: 1.5;'>Tài khoản của bạn đã được xác minh danh tính (KYC) thành công bởi quản trị viên.</p>"
                +
                "<div style='background: #EFF6FF; border: 1px solid #BFDBFE; border-radius: 12px; padding: 24px; margin: 24px 0; text-align: center;'>"
                +
                "<div style='display: inline-flex; align-items: center; gap: 8px; padding: 8px 20px; background: #3B82F6; border-radius: 20px; margin-bottom: 12px;'>"
                +
                "<svg width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='#ffffff' stroke-width='2.5'><polyline points='20 6 9 17 4 12'/></svg>"
                +
                "<span style='color: #ffffff; font-size: 14px; font-weight: 700;'>ĐÃ XÁC MINH</span>"
                +
                "</div>" +
                "<p style='margin: 0; color: #1E40AF; font-size: 15px; line-height: 1.6;'>Tài khoản của bạn hiện đã được kích hoạt <strong>tích xanh</strong> xác minh danh tính. Điều này giúp tăng độ tin cậy và uy tín khi bạn đặt phòng hoặc cho thuê chỗ ở trên Airbnb Homestay.</p>"
                +
                "</div>" +
                "<div style='background: #F7F7F7; border-radius: 8px; padding: 16px; text-align: center;'>" +
                "<p style='margin: 0; color: #717171; font-size: 13px; line-height: 1.5;'>Đây là email tự động, vui lòng không trả lời email này.</p>"
                +
                "</div>" +
                "</td></tr>" +
                "<!-- Footer -->" +
                "<tr><td style='padding: 32px 40px; background-color: #f7f7f7; border-top: 1px solid #ebebeb; border-radius: 0 0 12px 12px; text-align: center;'>"
                +
                "<p style='margin: 0 0 4px; color: #3B82F6; font-size: 14px; font-weight: 600;'>Airbnb Homestay</p>"
                +
                "<p style='margin: 0; color: #717171; font-size: 12px;'>© 2026 Airbnb Homestay, Inc. Tất cả các quyền được bảo lưu.</p>"
                +
                "</td></tr>" +
                "</table>" +
                "</td></tr>" +
                "</table>" +
                "</body>" +
                "</html>";
    }

    private String buildKycRevokedEmailContent(String hoTen) {
        String ten = hoTen != null ? hoTen.split(" ")[hoTen.split(" ").length - 1] : "bạn";
        return "<!DOCTYPE html>" +
                "<html lang='vi'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Helvetica Neue\", Arial, sans-serif; background-color: #f7f7f7;'>"
                +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background-color: #f7f7f7; padding: 40px 20px;'>"
                +
                "<tr><td align='center'>" +
                "<table width='600' cellpadding='0' cellspacing='0' style='background-color: #ffffff; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);'>"
                +
                "<!-- Header -->" +
                "<tr><td style='padding: 40px 40px 20px; text-align: center; border-bottom: 1px solid #ebebeb;'>" +
                "<div style='width: 56px; height: 56px; background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%); border-radius: 50%; margin: 0 auto 16px; display: flex; align-items: center; justify-content: center; box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);'>"
                +
                "<svg width='28' height='28' viewBox='0 0 24 24' fill='none' stroke='#ffffff' stroke-width='2.5' stroke-linecap='round' stroke-linejoin='round'><path d='M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z'/><line x1='12' y1='9' x2='12' y2='13'/><line x1='12' y1='17' x2='12.01' y2='17'/></svg>"
                +
                "</div>" +
                "<h1 style='margin: 0; color: #222222; font-size: 28px; font-weight: 600;'>Cập nhật xác minh danh tính</h1>"
                +
                "</td></tr>" +
                "<!-- Content -->" +
                "<tr><td style='padding: 40px;'>" +
                "<p style='margin: 0 0 16px; color: #222; font-size: 16px; line-height: 1.5;'>Xin chào <strong>" + ten + "</strong>,</p>"
                +
                "<p style='margin: 0 0 24px; color: #717171; font-size: 16px; line-height: 1.5;'>Trạng thái xác minh danh tính (KYC) của tài khoản bạn đã được quản trị viên cập nhật.</p>"
                +
                "<div style='background: #FFFBEB; border: 1px solid #FDE68A; border-radius: 12px; padding: 24px; margin: 24px 0; text-align: center;'>"
                +
                "<div style='display: inline-flex; align-items: center; gap: 8px; padding: 8px 20px; background: #F59E0B; border-radius: 20px; margin-bottom: 12px;'>"
                +
                "<span style='color: #ffffff; font-size: 14px; font-weight: 700;'>ĐÃ HỦY XÁC MINH</span>"
                +
                "</div>" +
                "<p style='margin: 0; color: #92400E; font-size: 15px; line-height: 1.6;'>Xác minh danh tính của tài khoản đã bị hủy. Tích xanh xác minh sẽ không còn hiển thị trên hồ sơ của bạn. Nếu bạn cần hỗ trợ hoặc cho rằng có nhầm lẫn, vui lòng liên hệ với đội ngũ hỗ trợ của chúng tôi.</p>"
                +
                "</div>" +
                "<div style='background: #F7F7F7; border-radius: 8px; padding: 16px; text-align: center;'>" +
                "<p style='margin: 0; color: #717171; font-size: 13px; line-height: 1.5;'>Đây là email tự động, vui lòng không trả lời email này.</p>"
                +
                "</div>" +
                "</td></tr>" +
                "<!-- Footer -->" +
                "<tr><td style='padding: 32px 40px; background-color: #f7f7f7; border-top: 1px solid #ebebeb; border-radius: 0 0 12px 12px; text-align: center;'>"
                +
                "<p style='margin: 0 0 4px; color: #F59E0B; font-size: 14px; font-weight: 600;'>Airbnb Homestay</p>"
                +
                "<p style='margin: 0; color: #717171; font-size: 12px;'>© 2026 Airbnb Homestay, Inc. Tất cả các quyền được bảo lưu.</p>"
                +
                "</td></tr>" +
                "</table>" +
                "</td></tr>" +
                "</table>" +
                "</body>" +
                "</html>";
    }

    private String buildHostApprovedEmailContent(String hoTen) {
        String ten = hoTen != null ? hoTen.split(" ")[hoTen.split(" ").length - 1] : "bạn";
        return "<!DOCTYPE html>" +
                "<html lang='vi'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Helvetica Neue\", Arial, sans-serif; background-color: #f7f7f7;'>"
                +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background-color: #f7f7f7; padding: 40px 20px;'>"
                +
                "<tr><td align='center'>" +
                "<table width='600' cellpadding='0' cellspacing='0' style='background-color: #ffffff; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);'>"
                +
                "<!-- Header -->" +
                "<tr><td style='padding: 40px 40px 20px; text-align: center; border-bottom: 1px solid #ebebeb;'>" +
                "<div style='width: 56px; height: 56px; background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%); border-radius: 50%; margin: 0 auto 16px; display: flex; align-items: center; justify-content: center; box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);'>"
                +
                "<svg width='28' height='28' viewBox='0 0 24 24' fill='none' stroke='#ffffff' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><path d='M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z'/><polyline points='9 22 9 12 15 12 15 22'/></svg>"
                +
                "</div>" +
                "<h1 style='margin: 0; color: #222222; font-size: 28px; font-weight: 600;'>Chào mừng bạn trở thành Chủ nhà!</h1>"
                +
                "</td></tr>" +
                "<!-- Content -->" +
                "<tr><td style='padding: 40px;'>" +
                "<p style='margin: 0 0 16px; color: #222; font-size: 16px; line-height: 1.5;'>Xin chào <strong>" + ten + "</strong>,</p>"
                +
                "<p style='margin: 0 0 24px; color: #717171; font-size: 16px; line-height: 1.5;'>Chúc mừng! Tài khoản của bạn đã được quản trị viên phê duyệt với vai trò <strong>Chủ nhà (Host)</strong>.</p>"
                +
                "<div style='background: #FFFBEB; border: 1px solid #FDE68A; border-radius: 12px; padding: 24px; margin: 24px 0;'>"
                +
                "<p style='margin: 0 0 16px; color: #92400E; font-size: 15px; font-weight: 600;'>🎉 Quyền lợi của bạn với tư cách Chủ nhà:</p>"
                +
                "<table style='width: 100%; border-collapse: collapse;'>" +
                "<tr><td style='padding: 8px 0; color: #717171; font-size: 14px;'><span style='color: #F59E0B; margin-right: 8px;'>✓</span> Tạo và quản lý danh sách cho thuê</td></tr>"
                +
                "<tr><td style='padding: 8px 0; color: #717171; font-size: 14px;'><span style='color: #F59E0B; margin-right: 8px;'>✓</span> Nhận đặt chỗ và kiếm thu nhập</td></tr>"
                +
                "<tr><td style='padding: 8px 0; color: #717171; font-size: 14px;'><span style='color: #F59E0B; margin-right: 8px;'>✓</span> Quản lý lịch và giá cho thuê linh hoạt</td></tr>"
                +
                "<tr><td style='padding: 8px 0; color: #717171; font-size: 14px;'><span style='color: #F59E0B; margin-right: 8px;'>✓</span> Trò chuyện trực tiếp với khách qua hệ thống</td></tr>"
                +
                "</table>" +
                "</div>" +
                "<a href='" + frontendUrl + "/hosting/listings"
                + "' style='display: block; text-align: center; background: #F59E0B; color: #ffffff; padding: 14px 32px; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 15px;'>Bắt đầu cho thuê ngay</a>"
                +
                "</td></tr>" +
                "<!-- Footer -->" +
                "<tr><td style='padding: 32px 40px; background-color: #f7f7f7; border-top: 1px solid #ebebeb; border-radius: 0 0 12px 12px; text-align: center;'>"
                +
                "<p style='margin: 0 0 4px; color: #F59E0B; font-size: 14px; font-weight: 600;'>Airbnb Homestay</p>"
                +
                "<p style='margin: 0; color: #717171; font-size: 12px;'>© 2026 Airbnb Homestay, Inc. Tất cả các quyền được bảo lưu.</p>"
                +
                "</td></tr>" +
                "</table>" +
                "</td></tr>" +
                "</table>" +
                "</body>" +
                "</html>";
    }

    private String buildHostRejectedEmailContent(String hoTen) {
        String ten = hoTen != null ? hoTen.split(" ")[hoTen.split(" ").length - 1] : "bạn";
        return "<!DOCTYPE html>" +
                "<html lang='vi'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Helvetica Neue\", Arial, sans-serif; background-color: #f7f7f7;'>"
                +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background-color: #f7f7f7; padding: 40px 20px;'>" +
                "<tr><td align='center'>" +
                "<table width='600' cellpadding='0' cellspacing='0' style='background-color: #ffffff; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);'>"
                +
                "<!-- Header -->" +
                "<tr><td style='padding: 40px 40px 20px; text-align: center; border-bottom: 1px solid #ebebeb;'>" +
                "<div style='width: 56px; height: 56px; background: linear-gradient(135deg, #EF4444 0%, #DC2626 100%); border-radius: 50%; margin: 0 auto 16px; display: flex; align-items: center; justify-content: center; box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);'>"
                +
                "<svg width='28' height='28' viewBox='0 0 24 24' fill='none' stroke='#ffffff' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><circle cx='12' cy='12' r='10'/><line x1='15' y1='9' x2='9' y2='15'/><line x1='9' y1='9' x2='15' y2='15'/></svg>"
                +
                "</div>" +
                "<h1 style='margin: 0; color: #222222; font-size: 28px; font-weight: 600;'>Yêu cầu đã bị từ chối</h1>"
                +
                "</td></tr>" +
                "<!-- Content -->" +
                "<tr><td style='padding: 40px;'>" +
                "<p style='margin: 0 0 16px; color: #222; font-size: 16px; line-height: 1.5;'>Xin chào <strong>" + ten + "</strong>,</p>"
                +
                "<p style='margin: 0 0 24px; color: #717171; font-size: 16px; line-height: 1.5;'>Rất tiếc, yêu cầu trở thành <strong>Chủ nhà (Host)</strong> của bạn đã không được chấp nhận lần này.</p>"
                +
                "<div style='background: #FEF2F2; border: 1px solid #FECACA; border-radius: 12px; padding: 24px; margin: 24px 0;'>"
                +
                "<p style='margin: 0 0 12px; color: #991B1B; font-size: 15px; font-weight: 600;'>Bạn có thể thử lại sau với các gợi ý:</p>"
                +
                "<table style='width: 100%; border-collapse: collapse;'>" +
                "<tr><td style='padding: 6px 0; color: #7F1D1D; font-size: 14px;'><span style='color: #EF4444; margin-right: 8px;'>•</span> Đảm bảo thông tin hồ sơ đầy đủ và chính xác</td></tr>"
                +
                "<tr><td style='padding: 6px 0; color: #7F1D1D; font-size: 14px;'><span style='color: #EF4444; margin-right: 8px;'>•</span> Chia sẻ lý do cụ thể hơn khi đăng ký</td></tr>"
                +
                "<tr><td style='padding: 6px 0; color: #7F1D1D; font-size: 14px;'><span style='color: #EF4444; margin-right: 8px;'>•</span> Liên hệ hỗ trợ nếu bạn cần thêm thông tin</td></tr>"
                +
                "</table>" +
                "</div>" +
                "<p style='margin: 0 0 24px; color: #717171; font-size: 14px; line-height: 1.5;'>Bạn vẫn có thể gửi lại yêu cầu bất cứ lúc nào. Đừng bỏ lỡ cơ hội trở thành Chủ nhà trên Airbnb Homestay!</p>"
                +
                "<a href='" + frontendUrl
                + "' style='display: block; text-align: center; background: #FF385C; color: #ffffff; padding: 14px 32px; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 15px;'>Gửi lại yêu cầu</a>"
                +
                "</td></tr>" +
                "<!-- Footer -->" +
                "<tr><td style='padding: 32px 40px; background-color: #f7f7f7; border-top: 1px solid #ebebeb; border-radius: 0 0 12px 12px; text-align: center;'>"
                +
                "<p style='margin: 0 0 4px; color: #FF385C; font-size: 14px; font-weight: 600;'>Airbnb Homestay</p>"
                +
                "<p style='margin: 0; color: #717171; font-size: 12px;'>© 2026 Airbnb Homestay, Inc. Tất cả các quyền được bảo lưu.</p>"
                +
                "</td></tr>" +
                "</table>" +
                "</td></tr>" +
                "</table>" +
                "</body>" +
                "</html>";
    }

    private String buildHostRevokedEmailContent(String hoTen) {
        String ten = hoTen != null ? hoTen.split(" ")[hoTen.split(" ").length - 1] : "bạn";
        return "<!DOCTYPE html>" +
                "<html lang='vi'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Helvetica Neue\", Arial, sans-serif; background-color: #f7f7f7;'>"
                +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background-color: #f7f7f7; padding: 40px 20px;'>"
                +
                "<tr><td align='center'>" +
                "<table width='600' cellpadding='0' cellspacing='0' style='background-color: #ffffff; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);'>"
                +
                "<!-- Header -->" +
                "<tr><td style='padding: 40px 40px 20px; text-align: center; border-bottom: 1px solid #ebebeb;'>" +
                "<div style='width: 56px; height: 56px; background: linear-gradient(135deg, #6B7280 0%, #4B5563 100%); border-radius: 50%; margin: 0 auto 16px; display: flex; align-items: center; justify-content: center; box-shadow: 0 4px 12px rgba(107, 114, 128, 0.3);'>"
                +
                "<svg width='28' height='28' viewBox='0 0 24 24' fill='none' stroke='#ffffff' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><path d='M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2'/><circle cx='12' cy='7' r='4'/></svg>"
                +
                "</div>" +
                "<h1 style='margin: 0; color: #222222; font-size: 28px; font-weight: 600;'>Cập nhật vai trò tài khoản</h1>"
                +
                "</td></tr>" +
                "<!-- Content -->" +
                "<tr><td style='padding: 40px;'>" +
                "<p style='margin: 0 0 16px; color: #222; font-size: 16px; line-height: 1.5;'>Xin chào <strong>" + ten + "</strong>,</p>"
                +
                "<p style='margin: 0 0 24px; color: #717171; font-size: 16px; line-height: 1.5;'>Vai trò <strong>Chủ nhà (Host)</strong> của bạn trên Airbnb Homestay đã được quản trị viên cập nhật.</p>"
                +
                "<div style='background: #F3F4F6; border: 1px solid #E5E7EB; border-radius: 12px; padding: 24px; margin: 24px 0; text-align: center;'>"
                +
                "<div style='display: inline-flex; align-items: center; gap: 8px; padding: 8px 20px; background: #6B7280; border-radius: 20px; margin-bottom: 12px;'>"
                +
                "<span style='color: #ffffff; font-size: 14px; font-weight: 700;'>KHÁCH (GUEST)</span>"
                +
                "</div>" +
                "<p style='margin: 0; color: #4B5563; font-size: 15px; line-height: 1.6;'>Tài khoản của bạn hiện đang ở chế độ <strong>Khách (Guest)</strong>. Bạn vẫn có thể đặt phòng, trải nghiệm dịch vụ và khám phá những nơi lưu trú trên Airbnb Homestay. Mọi danh sách cho thuê hiện có sẽ không còn hoạt động.</p>"
                +
                "</div>" +
                "<p style='margin: 24px 0 0; color: #717171; font-size: 14px; line-height: 1.5;'>Nếu bạn cần hỗ trợ hoặc cho rằng có nhầm lẫn, vui lòng liên hệ với đội ngũ hỗ trợ của chúng tôi.</p>"
                +
                "</td></tr>" +
                "<!-- Footer -->" +
                "<tr><td style='padding: 32px 40px; background-color: #f7f7f7; border-top: 1px solid #ebebeb; border-radius: 0 0 12px 12px; text-align: center;'>"
                +
                "<p style='margin: 0 0 4px; color: #6B7280; font-size: 14px; font-weight: 600;'>Airbnb Homestay</p>"
                +
                "<p style='margin: 0; color: #717171; font-size: 12px;'>© 2026 Airbnb Homestay, Inc. Tất cả các quyền được bảo lưu.</p>"
                +
                "</td></tr>" +
                "</table>" +
                "</td></tr>" +
                "</table>" +
                "</body>" +
                "</html>";
    }

    private byte[] generateQRCodeImage(String text) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 250, 250);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        } catch (Exception e) {
            log.error("Could not generate QR Code", e);
            return null;
        }
    }

    private String buildBookingPendingEmailContent(String guestName, DatCho booking, boolean isWalletPayment,
            BigDecimal newBalance) {
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        String balanceInfo = "";
        if (isWalletPayment && newBalance != null) {
            balanceInfo = "<div style='background-color: #f7f7f7; padding: 15px; border-radius: 8px; margin-top: 20px;'>"
                    +
                    "<p style='margin: 0; color: #717171; font-size: 14px;'>Thanh toán qua Ví thành công.</p>" +
                    "<p style='margin: 5px 0 0; color: #222; font-weight: bold;'>Số dư hiện tại: "
                    + currencyFormatter.format(newBalance) + "</p>" +
                    "</div>";
        }

        return "<!DOCTYPE html>" +
                "<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #222;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h1 style='color: #FF385C;'>Yêu cầu đặt phòng đã được gửi!</h1>" +
                "<p>Xin chào " + guestName + ",</p>" +
                "<p>Chúng tôi đã nhận được thanh toán của bạn cho chuyến đi tới <strong>"
                + booking.getPhong().getTieuDe() + "</strong>.</p>" +
                "<p>Trạng thái hiện tại: <strong style='color: #F59E0B;'>Chờ xác nhận</strong></p>" +
                "<p>Admin sẽ xem xét và xác nhận đặt phòng của bạn trong thời gian sớm nhất. Bạn sẽ nhận được email thông báo khi hoàn tất.</p>"
                +

                "<div style='border-top: 1px solid #eee; border-bottom: 1px solid #eee; padding: 20px 0; margin: 20px 0;'>"
                +
                "<p><strong>Mã đặt phòng:</strong> #" + booking.getMaDatCho() + "</p>" +
                "<p><strong>Nhận phòng:</strong> " + booking.getNgayNhanPhong().format(dateFormatter) + "</p>" +
                "<p><strong>Trả phòng:</strong> " + booking.getNgayTraPhong().format(dateFormatter) + "</p>" +
                "<p><strong>Tổng tiền:</strong> " + currencyFormatter.format(booking.getTongTien()) + "</p>" +
                "</div>" +

                balanceInfo +

                "<p style='margin-top: 30px;'>Cảm ơn bạn đã chọn Airbnb Homestay!</p>" +
                "</div></body></html>";
    }

    private String buildBookingConfirmedEmailContent(String guestName, DatCho booking) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        return "<!DOCTYPE html>" +
                "<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #222;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h1 style='color: #FF385C;'>Đặt phòng đã được xác nhận!</h1>" +
                "<p>Xin chào " + guestName + ",</p>" +
                "<p>Chúc mừng! Chuyến đi của bạn tới <strong>" + booking.getPhong().getTieuDe()
                + "</strong> đã chính thức được xác nhận.</p>" +

                "<div style='background-color: #f0fdf4; border: 1px solid #bbf7d0; padding: 20px; border-radius: 12px; margin: 20px 0; text-align: center;'>"
                +
                "<p style='margin: 0 0 10px; font-weight: bold; color: #166534;'>MÃ CHECK-IN CỦA BẠN</p>" +
                "<img src='cid:qrCodeImage' alt='QR Code' style='width: 200px; height: 200px;' />" +
                (booking.getPhong().getPassPhong() != null && !booking.getPhong().getPassPhong().isEmpty()
                    ? "<p style='margin: 10px 0 0; font-size: 16px; font-weight: bold; color: #166534;'>PASS PHÒNG: " + booking.getPhong().getPassPhong() + "</p>"
                    : "") +
                "<p style='margin: 10px 0 0; font-size: 13px; color: #166534;'>Quét mã QR hoặc nhập pass phòng khi check-in. Không cần lễ tân.</p>"
                +
                "</div>" +

                "<div style='margin-bottom: 20px;'>" +
                "<p><strong>Địa chỉ:</strong> " + booking.getPhong().getDiaChiDayDu() + ", "
                + booking.getPhong().getThanhPho() + "</p>" +
                "<p><strong>Thời gian:</strong> " + booking.getNgayNhanPhong().format(dateFormatter) + " - "
                + booking.getNgayTraPhong().format(dateFormatter) + "</p>" +
                "<p><strong>Số khách:</strong> " + booking.getSoLuongKhach() + " khách</p>" +
                "</div>" +

                "<p>Chúc bạn có một chuyến đi tuyệt vời!</p>" +
                "<p>Trân trọng,<br>Đội ngũ Airbnb Homestay</p>" +
                "</div></body></html>";
    }
}
