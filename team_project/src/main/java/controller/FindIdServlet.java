package controller;

import java.io.IOException;
import java.io.PrintWriter;

import dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// 경로 정상
@WebServlet("/findID") 
public class FindIdServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    private UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession();

        String name = request.getParameter("name");
        String email = request.getParameter("email");
        
        Boolean isAuthOK = (Boolean) session.getAttribute("isFindIdAuthOK");
        
        if (isAuthOK == null || !isAuthOK) {
            out.print("error:인증되지 않은 사용자입니다. 이메일 인증을 완료해주세요.");
            return;
        }

        try {
            String foundId = userDAO.findIdByNameAndEmail(name, email);
            
            if (foundId == null || foundId.isEmpty()) {
                out.print("error:입력하신 정보와 일치하는 ID를 찾을 수 없습니다.");
                return;
            }

            session.setAttribute("foundId", foundId);
            
            session.removeAttribute("isFindIdAuthOK"); 
            
            out.print("success:ID 조회 완료");

        } catch (Exception e) {
            e.printStackTrace();
            out.print("error:서버 내부 오류 발생. ID 조회에 실패했습니다.");
        }
    }
}