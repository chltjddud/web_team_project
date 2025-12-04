package controller;

import java.io.IOException;

import dao.UserDAO; 
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// @WebServlet("/registerUser") 
public class RegisterUserServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        // ⭐️ 응답 타입을 JSON으로 설정합니다. ⭐️
        response.setContentType("application/json;charset=UTF-8"); 
        
        HttpSession session = request.getSession(false); 

        if (session == null) {
             response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
             response.getWriter().write("{\"status\":\"error\", \"message\":\"세션이 만료되었거나 1단계 정보가 누락되었습니다.\"}");
             return;
        }
        
        try {
            // 1. 폼 데이터 획득
            String loginId = request.getParameter("user-id"); 
            String password = request.getParameter("password");
            String nickname = request.getParameter("nickname");
            
            // 2. 세션 데이터 획득
            String name = (String) session.getAttribute("reg_name"); 
            String birthdate = (String) session.getAttribute("reg_birthdate"); 
            String email = (String) session.getAttribute("reg_email"); 
            
            // 3. 필수 데이터 검증
            if (loginId == null || password == null || nickname == null || 
                name == null || email == null || birthdate == null) {
                
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"status\":\"error\", \"message\":\"회원가입 필수 정보가 누락되었습니다.\"}");
                return;
            }

            // 4. DB 저장 로직 실행
            UserDAO dao = new UserDAO();
            boolean success = dao.insertUser(loginId, password, nickname, name, email, birthdate);
            
            if (success) {
                // 5. 성공 시 세션 정리 후 JSON 응답
                
                session.removeAttribute("reg_name");
                session.removeAttribute("reg_birthdate");
                session.removeAttribute("reg_email");
                
                // ⭐️ JSON 응답에 리다이렉트 경로 포함 (main_page.html 경로 반영) ⭐️
                String redirectUrl = request.getContextPath() + "/test/main/main_page.html";
                
                response.getWriter().write("{\"status\":\"success\", \"message\":\"회원가입 성공\", \"redirectUrl\":\"" + redirectUrl + "\"}");

            } else {
                // DB 저장 실패 시 500 응답
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"status\":\"error\", \"message\":\"사용자 정보 DB 저장 중 오류가 발생했습니다.\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"error\", \"message\":\"회원가입 처리 중 치명적인 서버 오류가 발생했습니다.\"}");
        }
    }
}