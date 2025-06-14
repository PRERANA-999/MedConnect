package com.example.medconnect;

import android.os.AsyncTask;
import android.util.Log;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSender {

    private static final String TAG = "EmailSender";

    // IMPORTANT: REPLACE THESE WITH YOUR ACTUAL EMAIL SERVICE DETAILS
    // For Gmail, you MUST generate an "App password" in your Google Account security settings.
    // DO NOT use your regular Gmail password here directly.
    private static final String SMTP_HOST = "smtp.gmail.com"; // Example for Gmail
    private static final String SMTP_PORT = "587";            // Example for Gmail, TLS
    private static final String SENDER_EMAIL = "punithnaidu05@gmail.com"; // Your actual sending email address
    private static final String SENDER_PASSWORD = "juoy oegj kxgp rpei"; // Your email password or App password

    public static void sendEmail(String recipientEmail, String subject, String body) {
        // Execute email sending on a background thread
        new SendMailTask().execute(recipientEmail, subject, body);
    }

    private static class SendMailTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            String recipientEmail = params[0];
            String subject = params[1];
            String body = params[2];

            Properties props = new Properties();
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true"); // Use TLS encryption

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                }
            });

            try {
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL));
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));
                message.setSubject(subject);
                message.setContent(body, "text/html"); // Set content type for HTML emails

                Transport.send(message);
                Log.d(TAG, "Email sent successfully to " + recipientEmail);
                return true;
            } catch (MessagingException e) {
                Log.e(TAG, "Error sending email (MessagingException): " + e.getMessage(), e);
                // Log specific details if needed for debugging SMTP issues
                if (e.getCause() != null) {
                    Log.e(TAG, "Cause: " + e.getCause().getMessage());
                }
                return false;
            } catch (Exception e) {
                Log.e(TAG, "General error during email sending: " + e.getMessage(), e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            // This method runs on the UI thread after doInBackground completes.
            // You could use a Toast here, but it's generally better for the calling
            // Activity to handle UI feedback based on the email sending status.
            // For now, it just logs success/failure.
        }
    }
}