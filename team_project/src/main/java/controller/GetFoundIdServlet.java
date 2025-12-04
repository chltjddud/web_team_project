package controller;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// ⭐️ 수정 1: 매핑 경로를 /getFoundId로 단순화 ⭐️
// @WebServlet("/getFoundId") 
public class GetFoundIdServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(false); 

        if (session == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); 
            out.print("error:SESSION_NULL: 세션이 존재하지 않거나 만료되었습니다.");
            return;
        }

        // ⭐️ 수정 2: 세션 키를 FindIdServlet과 동일하게 "foundId"로 변경 ⭐️
        String foundId = (String) session.getAttribute("foundId");
        
        if (foundId == null || foundId.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND); 
            out.print("error:ID_MISSING: 세션에 ID 정보(foundId)가 없습니다. (ID 미저장/이미 사용)");
            return;
        }
        
        // 성공 시 세션에서 ID 정보 제거
        session.removeAttribute("foundId"); 
        
        // 200 OK와 함께 ID 반환
        out.print("success:" + foundId);
    }
}