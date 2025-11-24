package controller;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// ⭐️ 매핑 경로 수정: /FindPWverifyCode ⭐️
@WebServlet("/FindPWverifyCode") 
public class FindPwVerifyCodeServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession();

        // 1. 클라이언트로부터 입력 값 받기
        String inputCode = request.getParameter("authCode");
        String inputEmail = request.getParameter("email");
        String inputId = request.getParameter("loginId"); 

        // 2. 세션에서 저장된 PW 찾기 인증 정보 불러오기
        String storedCode = (String) session.getAttribute("findPwAuthCode"); 
        String storedEmail = (String) session.getAttribute("findPwAuthEmail"); 
        String storedId = (String) session.getAttribute("findPwAuthId"); 
        Long expiryTime = (Long) session.getAttribute("findPwAuthExpiry"); 

        // ----------------------------------------------------
        // 3. 유효성 검사
        // ----------------------------------------------------
        if (inputCode == null || inputEmail == null || inputId == null || storedCode == null || expiryTime == null) {
            out.print("error:인증 정보가 유효하지 않습니다. 인증번호를 다시 요청해주세요.");
            return;
        }

        // A. ID, 이메일 일치 검사
        if (!storedEmail.equals(inputEmail) || !storedId.equals(inputId)) {
             out.print("error:인증 요청 정보가 일치하지 않습니다. 다시 확인해주세요.");
             return;
        }
        
        // B. 만료 시간 검사
        if (System.currentTimeMillis() > expiryTime) {
            session.removeAttribute("findPwAuthCode");
            session.removeAttribute("findPwAuthExpiry");
            out.print("error:인증번호가 만료되었습니다. 재전송해주세요.");
            return;
        }
        
        // C. 코드 일치 검사
        if (!storedCode.equals(inputCode)) {
            out.print("error:인증번호가 일치하지 않습니다.");
            return;
        }

        // ----------------------------------------------------
        // 4. 인증 성공 처리
        // ----------------------------------------------------
        
        // 최종 PW 조회(NEXT 버튼) 활성화 플래그
        session.setAttribute("isFindPwAuthOK", true); // ⭐️ PW 찾기 인증 완료 플래그 설정 ⭐️
        
        // 보안을 위해 사용된 인증 정보 제거
        session.removeAttribute("findPwAuthCode");
        session.removeAttribute("findPwAuthExpiry");

        out.print("success:인증이 완료되었습니다.");
    }
}