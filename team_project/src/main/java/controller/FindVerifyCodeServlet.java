package controller;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// 경로 정상
@WebServlet("/FindverifyCode") 
public class FindVerifyCodeServlet extends HttpServlet {
    // ... (내부 로직은 이전과 동일) ...
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession();

        String inputCode = request.getParameter("authCode");
        String inputEmail = request.getParameter("email");
        
        String storedCode = (String) session.getAttribute("findIdAuthCode");
        String storedEmail = (String) session.getAttribute("findIdAuthEmail"); 
        Long expiryTime = (Long) session.getAttribute("findIdAuthExpiry");

        if (inputCode == null || inputEmail == null || storedCode == null || expiryTime == null) {
            out.print("error:인증 정보가 유효하지 않습니다. 인증번호를 다시 요청해주세요.");
            return;
        }

        if (!storedEmail.equals(inputEmail)) {
             out.print("error:인증 요청 정보가 일치하지 않습니다. (이메일 불일치)");
             return;
        }
        
        if (System.currentTimeMillis() > expiryTime) {
            session.removeAttribute("findIdAuthCode");
            session.removeAttribute("findIdAuthExpiry");
            out.print("error:인증번호가 만료되었습니다. 재전송해주세요.");
            return;
        }
        
        if (!storedCode.equals(inputCode)) {
            out.print("error:인증번호가 일치하지 않습니다.");
            return;
        }

        session.setAttribute("isFindIdAuthOK", true); 
        
        session.removeAttribute("findIdAuthCode"); 
        session.removeAttribute("findIdAuthExpiry");
        
        out.print("success:인증 성공");
    }
}