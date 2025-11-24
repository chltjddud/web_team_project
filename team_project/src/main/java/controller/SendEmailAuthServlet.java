package controller;


import DB.DBUtil; // DB 유틸리티 클래스 import
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.Random;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

/**
 * 이메일로 6자리 인증번호를 전송하고 DB에 코드를 저장하는 서블릿입니다.
 */
@WebServlet("/test/resister/sendCode")
public class SendEmailAuthServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * 6자리 랜덤 인증번호를 생성합니다.
     */
    private String generateCode() {
        Random rand = new Random();
        // 100000 ~ 999999 사이의 6자리 숫자 생성
        return String.valueOf(rand.nextInt(900000) + 100000); 
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=UTF-8");

        String to = request.getParameter("email");
        String code = generateCode();

        // Gmail 계정 정보 (⚠️ 반드시 실제 앱 비밀번호로 변경해야 합니다.)
        final String user = "minigeimweb@gmail.com";
        final String pass = "cjrfnbuklhzpmhgz"; 

        // SMTP (Simple Mail Transfer Protocol) 설정
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true"); 

        // 인증정보를 포함한 메일 세션 생성
        Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });
        
        // JDBC 관련 변수
        Connection conn = null;
        PreparedStatement ps = null;
        
        // 시간 설정 (DB에 저장할 만료 시간 계산)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(5); // 5분 만료
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try {
            // 1. 이메일 메시지 작성 및 전송
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(user, "인증 서비스"));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject("이메일 인증번호입니다.");
            message.setText("인증번호: " + code);

            Transport.send(message); // 메일 전송 실행

            System.out.println("[INFO] 메일 전송 완료 → " + to + ", code=" + code);
            
            // 2. 데이터베이스 저장 로직
            conn = DBUtil.getConnection(); 
            
            // 쿼리: 인증 코드를 저장. 이메일이 이미 존재하면 코드를 업데이트하고 인증 상태를 0으로 리셋.
            String sql = "INSERT INTO email_verification (email, verification_code, created_at, expires_at, is_verified) " +
                         "VALUES (?, ?, ?, ?, 0) " +
                         "ON DUPLICATE KEY UPDATE verification_code=?, created_at=?, expires_at=?, is_verified=0";
            
            ps = conn.prepareStatement(sql);
            
            // INSERT 파라미터
            ps.setString(1, to);
            ps.setString(2, code);
            ps.setString(3, now.format(formatter));
            ps.setString(4, expiresAt.format(formatter));
            
            // UPDATE 파라미터
            ps.setString(5, code);
            ps.setString(6, now.format(formatter));
            ps.setString(7, expiresAt.format(formatter));

            ps.executeUpdate();
            
            System.out.println("[INFO] DB에 인증 코드 저장 완료: " + to);
            
            // 3. 클라이언트에게 성공 메시지 전송
            response.getWriter().println("전송 완료");

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().println("메일 전송 또는 DB 저장 실패: " + e.getMessage() + ". 로그를 확인하세요.");
        } finally {
            // 4. 리소스 해제
            try { if (ps != null) ps.close(); } catch (SQLException ignore) {}
            try { if (conn != null) conn.close(); } catch (SQLException ignore) {}
        }
    }
}