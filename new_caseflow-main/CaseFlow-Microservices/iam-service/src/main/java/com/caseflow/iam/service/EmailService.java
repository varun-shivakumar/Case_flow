package com.caseflow.iam.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from-name:CaseFlow Admin}")
    private String fromName;

    @Value("${app.mail.from-address:}")
    private String fromAddress;

    @Value("${app.mail.brand.logo-path:email-assets/logo.png}")
    private String logoPath;

    @Value("${app.mail.brand.banner-path:email-assets/banner.png}")
    private String bannerPath;

    /**
     * Send a plain-text email using the configured SMTP server.
     */
    public void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(buildBrandedHtml(subject, body), true);

            String senderAddress = resolveFromAddress();
            if (senderAddress != null && !senderAddress.isBlank()) {
                message.setFrom(senderAddress, fromName);
            }

            // Inline branding assets (CID) for Gmail/Outlook compatibility.
            addInlineIfPresent(message, logoPath, "caseflowLogo");
            addInlineIfPresent(message, bannerPath, "caseflowBanner");

            mailSender.send(mimeMessage);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            // Log the full exception chain so the actual SMTP error is visible in logs
            log.error("Failed to send email to {}", to, e);
            // Preserve original cause so callers can inspect it
            throw new RuntimeException("Failed to send email. Please try again later.", e);
        }
    }

    private void addInlineIfPresent(MimeMessageHelper message, String path, String contentId) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (resource.exists()) {
                message.addInline(contentId, resource);
            } else {
                log.warn("Email asset not found at classpath: {}", path);
            }
        } catch (Exception ex) {
            log.warn("Could not attach inline email asset {}: {}", path, ex.getMessage());
        }
    }

    private String buildBrandedHtml(String subject, String plainBody) {
        String safeSubject = escapeHtml(subject);
        String htmlBody = linkifyAndFormat(escapeHtml(plainBody));

        return """
                <html>
                <body style="margin:0;padding:0;background:#f4f6fa;font-family:Arial,Helvetica,sans-serif;color:#1b2438;">
                  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="padding:24px 12px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="640" cellpadding="0" cellspacing="0" style="max-width:640px;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e8edf6;">
                          <tr>
                            <td style="padding:18px 22px;background:#0f1629;">
                              <img src="cid:caseflowLogo" alt="CaseFlow" style="height:34px;max-width:180px;display:block;" />
                            </td>
                          </tr>
                          <tr>
                            <td>
                              <img src="cid:caseflowBanner" alt="CaseFlow Banner" style="width:100%;display:block;max-height:220px;object-fit:cover;" />
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:24px 22px 20px;">
                              <h2 style="margin:0 0 14px;font-size:20px;color:#0f1629;">__SUBJECT__</h2>
                              <div style="font-size:14px;line-height:1.65;color:#3a475f;">__BODY__</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:0 22px 18px;font-size:12px;color:#8a95aa;">This is an automated email from CaseFlow.</td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """
                .replace("__SUBJECT__", safeSubject)
                .replace("__BODY__", htmlBody);
    }

    private String linkifyAndFormat(String escapedText) {
        String withBreaks = escapedText.replace("\r\n", "\n").replace("\n", "<br/>");
        return withBreaks.replaceAll("(https?://[^\\s<]+)", "<a href=\"$1\" style=\"color:#2b68c2;\">$1</a>");
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String resolveFromAddress() {
        if (fromAddress != null && !fromAddress.isBlank()) {
            return fromAddress.trim();
        }
        if (mailSender instanceof JavaMailSenderImpl senderImpl) {
            String username = senderImpl.getUsername();
            if (username != null && !username.isBlank()) {
                return username.trim();
            }
        }
        return null;
    }
}
