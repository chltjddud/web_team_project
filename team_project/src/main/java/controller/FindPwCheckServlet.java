package controller; 

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.Properties; 
import java.io.UnsupportedEncodingException; 

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import dao.UserDAO; 
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// ⭐️ 매핑 경로 수정: /findPWCheck ⭐️
@WebServlet("/findPWCheck") 
public class FindPwCheckServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    private UserDAO userDAO = new UserDAO(); 
    private static final long AUTH_EXPIRY_TIME_MS = 5 * 60 * 1000; // 5분
    
    // 이메일 설정 상수 
    private static final String GMAIL_USER = "minigeimweb@gmail.com"; 
    private static final String GMAIL_PASS = "cjrfnbuklhzpmhgz"; 

    private void sendAuthCodeEmail(String toEmail, String authCode) 
            throws MessagingException, java.io.UnsupportedEncodingException { 
        
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2"); 
        
        Session mailSession = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(GMAIL_USER, GMAIL_PASS);
            }
        });
        
        MimeMessage message = new MimeMessage(mailSession);
        message.setFrom(new InternetAddress(GMAIL_USER, "미니게임 웹", "UTF-8"));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
        message.setSubject("비밀번호 찾기 인증번호", "UTF-8");
        message.setText("인증번호는 [" + authCode + "] 입니다. 5분 이내로 입력해주세요.", "UTF-8", "plain");

        Transport.send(message);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession();

        String loginId = request.getParameter("loginId"); 
        String email = request.getParameter("email");
        
        // ----------------------------------------------------
        // 1. ID와 이메일이 모두 일치하는 사용자 검사 
        // ----------------------------------------------------
        try {
            // UserDAO에 checkUserExistsByIdAndEmail(loginId, email)가 정의되어 있어야 합니다.
            boolean userExists = userDAO.checkUserExistsByIdAndEmail(loginId, email); 
            
            if (!userExists) {
                out.print("error:ID와 이메일이 일치하는 사용자 정보를 찾을 수 없습니다.");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("error:DB 조회 중 서버 오류가 발생했습니다.");
            return;
        }


        // 2. 인증번호 생성 및 이메일 전송
        Random random = new Random();
        int authCodeInt = 100000 + random.nextInt(900000); 
        String authCode = String.valueOf(authCodeInt);
        long expiryTime = System.currentTimeMillis() + AUTH_EXPIRY_TIME_MS;
        
        try {
            sendAuthCodeEmail(email, authCode);
            System.out.println("DEBUG: PW 찾기 인증번호 발송 완료 - 코드: " + authCode + " (이메일: " + email + ")");
        } catch (MessagingException e) {
            e.printStackTrace();
            out.print("error:이메일 전송에 실패했습니다. 서버의 메일 설정 및 라이브러리를 확인하세요.");
            return;
        } catch (UnsupportedEncodingException e) { 
            e.printStackTrace();
            out.print("error:메일 인코딩 설정 중 오류가 발생했습니다.");
            return;
        }


        // 3. 세션에 PW 찾기 인증 정보 저장
        session.setAttribute("findPwAuthCode", authCode);
        session.setAttribute("findPwAuthEmail", email);
        session.setAttribute("findPwAuthId", loginId); 
        session.setAttribute("findPwAuthExpiry", expiryTime);
        session.setAttribute("isFindPwAuthOK", false); // 인증 완료 플래그 초기화
        
        out.print("success:인증번호가 전송되었습니다.");
    }
}