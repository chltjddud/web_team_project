package controller;

import java.io.IOException;
import java.io.PrintWriter;
// import java.util.Random; // ⭐️ 제거: 임시 비밀번호 생성 로직 불필요 ⭐️

import dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// ⭐️ 매핑 경로: /findPW (수정 없음) ⭐️
// @WebServlet("/findPW") 
public class FindPwServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    private UserDAO userDAO = new UserDAO();

    // ⭐️ generateTempPassword 메서드는 사용자 요청에 따라 제거합니다. ⭐️

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession();

        String loginId = request.getParameter("loginId"); // 클라이언트에서 받은 ID
        String email = request.getParameter("email");     // 클라이언트에서 받은 이메일
        
        // 세션에 저장된 인증 정보
        Boolean isAuthOK = (Boolean) session.getAttribute("isFindPwAuthOK"); 
        String storedId = (String) session.getAttribute("findPwAuthId");
        String storedEmail = (String) session.getAttribute("findPwAuthEmail");
        
        // ----------------------------------------------------
        // 1. 인증 및 정보 유효성 검사 
        // ----------------------------------------------------

        if (isAuthOK == null || !isAuthOK) {
            out.print("error:인증되지 않은 사용자입니다. 이메일 인증을 완료해주세요.");
            return;
        }

        if (loginId == null || email == null || !loginId.equals(storedId) || !email.equals(storedEmail)) {
             // 보안을 위해 세션 속성 제거
            session.removeAttribute("isFindPwAuthOK");
            session.removeAttribute("findPwAuthId");
            session.removeAttribute("findPwAuthEmail");
            out.print("error:인증 정보가 유효하지 않거나 입력 정보가 일치하지 않습니다. 다시 시도해주세요.");
            return;
        }

        try {
            // ⭐️ 핵심 변경 1: DB에서 실제 비밀번호 조회 ⭐️
            String actualPassword = userDAO.findPasswordByLoginIdAndEmail(loginId, email); 
            
            if (actualPassword == null || actualPassword.isEmpty()) {
                out.print("error:일치하는 사용자 정보를 찾을 수 없습니다.");
                return;
            }

            // ⭐️ 핵심 변경 2: 실제 비밀번호를 세션에 저장 (키: foundPassword) ⭐️
            session.setAttribute("foundPassword", actualPassword); 
            session.setAttribute("foundLoginId", loginId); 

            // 3. 사용된 세션 속성 제거
            session.removeAttribute("isFindPwAuthOK");
            session.removeAttribute("findPwAuthEmail");
            session.removeAttribute("findPwAuthId");
            
            // 4. 성공 응답
            out.print("success:비밀번호 조회 완료"); 

        } catch (Exception e) {
            e.printStackTrace();
            out.print("error:서버 내부 오류 발생. 비밀번호 조회에 실패했습니다.");
        }
    }
}