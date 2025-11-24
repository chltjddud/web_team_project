package controller;

import java.io.IOException;

import dao.UserDAO; 
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/checkDuplicate") 
public class CheckDuplicateServlet extends HttpServlet {
 
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("text/plain;charset=UTF-8");
        
        try {
            request.setCharacterEncoding("UTF-8");
            
            String type = request.getParameter("type"); // id, nickname, 또는 email
            String value = request.getParameter("value");
            
            if (type == null || value == null || value.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("error: type 또는 value가 누락되었습니다.");
                return;
            }
            
            boolean isDuplicate = false;
            UserDAO dao = new UserDAO();
            
            if (type.equals("id")) {
                isDuplicate = dao.isUserIdDuplicate(value); 
            } else if (type.equals("nickname")) {
                isDuplicate = dao.isNicknameDuplicate(value);
            } else if (type.equals("email")) {
                // 이메일 중복 확인 처리
                isDuplicate = dao.isEmailDuplicate(value);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("error: 유효하지 않은 type입니다.");
                return;
            }
            
            if (isDuplicate) {
                response.getWriter().write("duplicate"); 
            } else {
                response.getWriter().write("available");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("fatal_error: 서버 DB 처리 중 오류 발생.");
        }
    }
}