package controller; 

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.Properties; 

// Jakarta Mail API 사용
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

// @WebServlet("/findIDCheck") 
public class FindIdCheckServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    private UserDAO userDAO = new UserDAO(); 
    private static final long AUTH_EXPIRY_TIME_MS = 5 * 60 * 1000; // 5분
    
    // ⭐️ 이메일 설정 상수 (앱 비밀번호로 대체 필수) ⭐️
    private static final String GMAIL_USER = "minigeimweb@gmail.com"; 
    private static final String GMAIL_PASS = "cjrfnbuklhzpmhgz"; 

    private void sendAuthCodeEmail(String toEmail, String code) throws MessagingException, java.io.UnsupportedEncodingException {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true"); 
        props.put("mail.smtp.ssl.protocols", "TLSv1.2"); 
        props.put("mail.smtp.timeout", "5000"); 
        props.put("mail.smtp.connectiontimeout", "5000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(GMAIL_USER, GMAIL_PASS);
            }
        });
        
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(GMAIL_USER, "인증 서비스", "UTF-8")); 
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
        message.setSubject("[ID 찾기] 요청하신 이메일 인증번호입니다.", "UTF-8"); 
        message.setText("인증번호: " + code + "\n\n5분 이내에 입력해주세요.", "UTF-8", "plain"); 

        Transport.send(message);
    }


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // ⭐️ 디버깅 로그: 요청 수신 확인 ⭐️
        System.out.println("DEBUG: [FindIdCheckServlet] 요청 수신 및 처리 시작.");
        
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain;charset=UTF-8"); 
        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession();

        String name = request.getParameter("name");
        String email = request.getParameter("email");

        if (name == null || name.trim().isEmpty() || email == null || email.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); 
            out.print("error:필수 정보(이름, 이메일) 누락");
            return;
        }

        try {
            // DAO 로그 추가: DB 확인 단계까지 도달했는지 확인
            System.out.println("DEBUG: DAO 호출 (이름: " + name + ", 이메일: " + email + ")");
            boolean userExists = userDAO.checkUserExistsByNameAndEmail(name, email);
            System.out.println("DEBUG: DAO 결과 (userExists): " + userExists);
            
            if (!userExists) {
                out.print("error:이름과 이메일이 일치하는 사용자 정보를 찾을 수 없습니다.");
                return;
            }

            // 2. 인증번호 생성
            Random random = new Random();
            int authCodeInt = 100000 + random.nextInt(900000); 
            String authCode = String.valueOf(authCodeInt);
            long expiryTime = System.currentTimeMillis() + AUTH_EXPIRY_TIME_MS;
            
            // 3. 이메일 전송
            try {
                sendAuthCodeEmail(email, authCode);
                
                // ⭐️ 수정 부분: 생성된 인증번호 콘솔에 출력 ⭐️
                System.out.println("DEBUG: [Mail Sent] 대상: " + email + ", 생성된 인증번호: " + authCode);
                
            } catch (MessagingException e) {
                e.printStackTrace();
                // 사용자 요청에 따라 Gmail 오류 언급은 피하고 일반적인 오류 메시지 사용
                out.print("error:이메일 전송에 실패했습니다. 서버의 메일 설정 및 라이브러리를 확인하세요.");
                return;
            }

            // 4. 세션에 인증 정보 저장 
            session.setAttribute("findIdAuthCode", authCode);
            session.setAttribute("findIdAuthEmail", email);
            session.setAttribute("findIdAuthExpiry", expiryTime);
            session.setAttribute("isFindIdAuthOK", false); 
            
            out.print("success:인증번호 전송 완료");

        } catch (Exception e) {
            e.printStackTrace();
            out.print("error:서버 내부 오류 발생. (유형: " + e.getClass().getSimpleName() + ")"); 
        }
    }
}