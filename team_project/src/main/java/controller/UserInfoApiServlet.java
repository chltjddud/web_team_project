package controller;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import model.User; 

/**
 * AJAX 요청을 받아 세션 사용자 정보를 JSON으로 반환하는 API 서블릿
 * 매핑 경로: /api/userInfo
 */
@WebServlet("/api/userInfo") 
public class UserInfoApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // JSON 응답 설정
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8"); 

        // 세션이 없으면 새로 생성하지 않음
        HttpSession session = request.getSession(false); 
        
        if (session != null && session.getAttribute("loggedInUser") != null) {
            User user = (User) session.getAttribute("loggedInUser");
            
            // 닉네임 또는 이름 중 표시할 이름 결정
            String displayName = user.getNickname();
            if (displayName == null || displayName.isEmpty()) {
                displayName = user.getName();
            }
            
            // JSON 응답 문자열 생성 (userId 포함)
            // ⭐️ 주의: JSON 문자열을 직접 생성하는 대신 Gson 라이브러리 등을 사용하면 좋습니다. 
            // 여기서는 단순성을 위해 String.format을 사용합니다.
            String jsonResponse = String.format(
                "{\"isLoggedIn\": true, \"displayName\": \"%s\", \"userId\": %d}", 
                displayName, 
                user.getUserId() // 스코어 저장에 필요한 핵심 정보
            );
            
            response.getWriter().write(jsonResponse);
            
        } else {
            // 세션 만료 또는 로그인 안됨
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // HTTP 401
            response.getWriter().write("{\"isLoggedIn\": false, \"message\": \"세션이 만료되었습니다.\"}");
        }
    }
}