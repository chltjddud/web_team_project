package controller;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 사용자 세션을 무효화(invalidate)하고 로그인 페이지로 리다이렉트하는 서블릿입니다.
 * 매핑 경로: /logout
 */
// @WebServlet("/logout") 
public class LogoutServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // GET 방식 또는 POST 방식으로 요청이 오면 모두 로그아웃 처리
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // 1. 현재 세션을 가져옵니다. (세션이 없으면 새로 생성하지 않습니다: false)
        HttpSession session = request.getSession(false); 
        
        if (session != null) {
            // 2. 세션을 무효화합니다. (세션에 저장된 모든 정보가 삭제됩니다.)
            session.invalidate(); 
            System.out.println("사용자 세션이 무효화되었습니다.");
        }
        
        // 3. 로그인 페이지(main_page.html)로 리다이렉트합니다.
        // 이 경로는 LoginServlet에서 로그인 실패 시 이동했던 경로와 동일합니다.
        // /team_project/test/main/main_page.html
        response.sendRedirect(request.getContextPath() + "/test/main/main_page.html");
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        doGet(request, response);
    }
}