package controller;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import dao.UserDAO; 
import model.User; // ⭐️ User 모델 import 추가 ⭐️

/**
 * ID/Password를 받아 로그인 인증을 처리하는 서블릿
 * 매핑 경로: /login
 */
// @WebServlet("/login") 
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private UserDAO userDAO = new UserDAO(); 

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8"); 
        
        String loginId = request.getParameter("loginId");
        String password = request.getParameter("password");
        
        User loggedInUser = null; // User 객체를 저장할 변수
        
        if (loginId != null && password != null) {
            // ⭐️ 1. UserDAO를 사용하여 인증 및 User 객체 조회 ⭐️
            loggedInUser = userDAO.findUserByCredentials(loginId, password);
        }

        if (loggedInUser != null) {
            // 2. 로그인 성공: 세션에 User 객체 전체 저장
            HttpSession session = request.getSession(); 
            // 나중에 스코어 저장 시 userId를 쉽게 가져올 수 있습니다.
            session.setAttribute("loggedInUser", loggedInUser); 

            // 3. 로그인 후 main_page_2.html 경로로 리다이렉트
            //    -> 이 HTML 파일이 AJAX를 이용해 세션 정보를 요청합니다.
            response.sendRedirect(request.getContextPath() + "/test/main/main_page_2.html"); 
            
        } else {
            // 4. 로그인 실패: main_page.html 경로로 에러 코드와 함께 리다이렉트
            response.sendRedirect(request.getContextPath() + "/test/main/main_page.html?error=invalid"); 
        }
    }
}