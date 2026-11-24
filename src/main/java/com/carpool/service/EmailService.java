package com.carpool.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.carpool.model.Booking;
import com.carpool.model.Ride;


@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ✅ Keep this for text mails (forgot password etc.)
    public void sendSimpleEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    // ✅ NEW: Professional HTML welcome email
    @Async
    public void sendWelcomeEmail(String to, String name, boolean needsApproval) throws MessagingException {

        String loginLink = "http://localhost:5173/login"; // change when deployed
        String subject = "Welcome to VeloCity 🚗";

        String approvalText = needsApproval
                ? "<p style='color:#e67e22;'><b>Note:</b> Your admin account requires approval before login.</p>"
                : "<p>You can log in immediately and start using the platform.</p>";

        String html = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Welcome to VeloCity</title>
        </head>
        <body style="margin:0; padding:0; background-color:#f4f6f8; font-family:Arial, sans-serif;">
            <div style="max-width:600px; margin:40px auto; background:#ffffff; border-radius:10px; overflow:hidden; box-shadow:0 0 10px rgba(0,0,0,0.08);">
                
                <div style="background:#0d6efd; color:white; padding:20px; text-align:center;">
                    <h1 style="margin:0;">🚗 VeloCity</h1>
                    <p style="margin:5px 0 0;">Smart Carpool Platform</p>
                </div>

                <div style="padding:30px; color:#333;">
                    <h2>Hi %s 👋</h2>
                    
                    <p>Your VeloCity account has been created successfully 🎉</p>
                    %s

                    <div style="text-align:center; margin:30px 0;">
                        <a href="%s" 
                           style="background:#0d6efd; color:white; padding:14px 28px; 
                                  text-decoration:none; border-radius:6px; font-size:16px; display:inline-block;">
                            Login to VeloCity
                        </a>
                    </div>

                    <p style="font-size:14px; color:#666;">
                        If you did not create this account, please contact our support immediately.
                    </p>

                    <p>Welcome aboard,<br><b>Team VeloCity</b></p>
                </div>

                <div style="background:#f1f3f5; text-align:center; padding:12px; font-size:12px; color:#777;">
                    © 2026 VeloCity. All rights reserved.
                </div>

            </div>
        </body>
        </html>
        """.formatted(name, approvalText, loginLink);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true); // true = HTML

        mailSender.send(message);
    }
    @Async
    public void sendRideConfirmedEmail(Booking booking, Ride ride) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(booking.getRequesterEmail());
            helper.setSubject("✅ Ride Confirmed – Your trip is booked!");

            String html = """
        <!DOCTYPE html>
        <html>
        <head>
          <style>
            body { font-family: Arial, sans-serif; background:#f4f6f8; padding:20px; }
            .card { max-width:600px; margin:auto; background:white; border-radius:10px; overflow:hidden; box-shadow:0 4px 10px rgba(0,0,0,0.08); }
            .header { background:#0d6efd; color:white; padding:20px; text-align:center; }
            .content { padding:20px; color:#333; }
            .ride-box { background:#f1f5f9; padding:15px; border-radius:8px; margin:15px 0; }
            .btn { display:inline-block; padding:12px 20px; background:#0d6efd; color:white; text-decoration:none; border-radius:6px; margin-top:15px; }
            .footer { text-align:center; font-size:12px; color:#777; padding:15px; }
          </style>
        </head>
        <body>
          <div class="card">
            <div class="header">
              <h2>🚗 Ride Confirmed!</h2>
              <p>Your booking is successfully confirmed</p>
            </div>

            <div class="content">
              <p>Hi <b>%s</b>,</p>
              <p>Your seat has been successfully booked. Here are your ride details:</p>

              <div class="ride-box">
                <p><b>Ride ID:</b> %d</p>
                <p><b>Seats Booked:</b> %d</p>
                <p><b>Status:</b> CONFIRMED</p>
                <p><b>Confirmed At:</b> %s</p>
              </div>

              <p>Please be on time and carry any required identification.</p>

              <a class="btn" href="http://localhost:3000/my-rides">View My Bookings</a>
            </div>

            <div class="footer">
              <p>Thank you for choosing our carpool service.</p>
              <p>Safe travels 🚀</p>
            </div>
          </div>
        </body>
        </html>
        """.formatted(
                    booking.getRequesterName(),
                    booking.getRideId(),
                    booking.getSeatsRequested(),
                    booking.getConfirmedAt()
            );

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            System.err.println("Ride confirmation email failed: " + e.getMessage());
        }
    }
    @Async
    public void sendInvoiceEmail(Booking booking, Ride ride) {
        try {
            System.out.println("✅ INVOICE EMAIL TRIGGERED for " + booking.getRequesterEmail());
            int seats = booking.getSeatsRequested();

            double baseFare = ride.getPrice() * seats;
            double gst = baseFare * 0.02;
            double platformFee = 20.0 * seats;
            double total = baseFare + gst + platformFee;

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(booking.getRequesterEmail());
            helper.setSubject("🧾 VeloCity Invoice – Payment Successful");

            String html = """
<!DOCTYPE html>
<html>
<body style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,sans-serif;">
<div style="max-width:650px;margin:30px auto;background:#ffffff;border-radius:10px;overflow:hidden;box-shadow:0 4px 10px rgba(0,0,0,0.08);">

  <div style="background:#0d6efd;color:white;padding:22px;text-align:center;">
    <h2 style="margin:0;">🧾 Payment Invoice</h2>
    <p style="margin:6px 0 0;">VeloCity Carpool Platform</p>
  </div>

  <div style="padding:25px;color:#333;">
    <p><b>Invoice To:</b> %s</p>
    <p><b>Email:</b> %s</p>
    <p><b>Transaction ID:</b> %s</p>
    <p><b>Booking ID:</b> %d</p>

    <hr style="margin:20px 0;">

    <h3>Fare Breakdown</h3>

    <table style="width:100%%;border-collapse:collapse;">
      <tr>
        <td>Base Fare (%d seat%s)</td>
        <td style="text-align:right;">₹%.2f</td>
      </tr>
      <tr>
        <td>GST (2%%)</td>
        <td style="text-align:right;">₹%.2f</td>
      </tr>
      <tr>
        <td>Platform Fee</td>
        <td style="text-align:right;">₹%.2f</td>
      </tr>
      <tr>
        <td colspan="2"><hr></td>
      </tr>
      <tr style="font-size:18px;">
        <td><b>Total Paid</b></td>
        <td style="text-align:right;"><b>₹%.2f</b></td>
      </tr>
    </table>

    <p style="margin-top:20px;">
      Payment Status: <b style="color:green;">SUCCESS</b><br>
      Confirmed At: %s
    </p>

    <div style="text-align:center;margin:30px 0;">
      <a href="http://localhost:3000/my-rides"
         style="background:#0d6efd;color:white;padding:12px 26px;text-decoration:none;border-radius:6px;">
        View My Bookings
      </a>
    </div>

    <p style="font-size:13px;color:#777;">
      This is a system generated invoice. For any support, contact VeloCity helpdesk.
    </p>
  </div>

  <div style="background:#f1f3f5;text-align:center;padding:12px;font-size:12px;color:#777;">
    © 2026 VeloCity
  </div>

</div>
</body>
</html>
""".formatted(
                    booking.getRequesterName(),
                    booking.getRequesterEmail(),
                    booking.getTransactionId(),
                    booking.getId(),
                    seats, seats > 1 ? "s" : "",
                    baseFare,
                    gst,
                    platformFee,
                    total,
                    booking.getConfirmedAt()
            );

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            System.err.println("Invoice email failed: " + e.getMessage());
        }
    }
    @Async
    public void sendBookingAcceptedEmail(Booking booking, Ride ride) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(booking.getRequesterEmail());
            helper.setSubject("✅ Ride Request Accepted – VeloCity");

            String html = """
<!DOCTYPE html>
<html>
<body style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,sans-serif;">
<div style="max-width:600px;margin:30px auto;background:#ffffff;border-radius:10px;overflow:hidden;box-shadow:0 4px 10px rgba(0,0,0,0.08);">

  <div style="background:#16a34a;color:white;padding:20px;text-align:center;">
    <h2 style="margin:0;">✅ Ride Accepted</h2>
    <p>Your request has been approved by the driver</p>
  </div>

  <div style="padding:24px;color:#333;">
    <p>Hi <b>%s</b>,</p>

    <p>Good news 🎉 Your ride request has been <b>accepted</b>.</p>

    <div style="background:#f1f5f9;padding:14px;border-radius:8px;">
      <p><b>Ride ID:</b> %d</p>
      <p><b>Seats:</b> %d</p>
      <p><b>Status:</b> ACCEPTED</p>
    </div>

    <div style="text-align:center;margin:25px 0;">
      <a href="http://localhost:3000/my-rides"
         style="background:#0d6efd;color:white;padding:12px 24px;text-decoration:none;border-radius:6px;">
        View My Rides
      </a>
    </div>

    <p>We wish you a safe and pleasant journey 🚗</p>
    <p><b>– Team VeloCity</b></p>
  </div>

</div>
</body>
</html>
""".formatted(
                    booking.getRequesterName(),
                    booking.getRideId(),
                    booking.getSeatsRequested()
            );

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            System.err.println("Accepted email failed");
            e.printStackTrace();
        }
    }
    @Async
    public void sendBookingRejectedEmail(Booking booking, Ride ride) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(booking.getRequesterEmail());
            helper.setSubject("❌ Ride Request Rejected – VeloCity");

            String html = """
<!DOCTYPE html>
<html>
<body style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,sans-serif;">
<div style="max-width:600px;margin:30px auto;background:#ffffff;border-radius:10px;overflow:hidden;box-shadow:0 4px 10px rgba(0,0,0,0.08);">

  <div style="background:#dc2626;color:white;padding:20px;text-align:center;">
    <h2 style="margin:0;">❌ Ride Rejected</h2>
    <p>Your ride request was not approved</p>
  </div>

  <div style="padding:24px;color:#333;">
    <p>Hi <b>%s</b>,</p>

    <p>Unfortunately, the driver has <b>rejected</b> your ride request.</p>

    <div style="background:#f1f5f9;padding:14px;border-radius:8px;">
      <p><b>Ride ID:</b> %d</p>
      <p><b>Seats:</b> %d</p>
      <p><b>Status:</b> REJECTED</p>
    </div>

    <p style="margin-top:18px;">
      Don’t worry — you can explore other rides and book again anytime.
    </p>

    <div style="text-align:center;margin:25px 0;">
      <a href="http://localhost:3000/rides"
         style="background:#0d6efd;color:white;padding:12px 24px;text-decoration:none;border-radius:6px;">
        Find Other Rides
      </a>
    </div>

    <p><b>– Team VeloCity</b></p>
  </div>

</div>
</body>
</html>
""".formatted(
                    booking.getRequesterName(),
                    booking.getRideId(),
                    booking.getSeatsRequested()
            );

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            System.err.println("Rejected email failed");
            e.printStackTrace();
        }
    }



}
