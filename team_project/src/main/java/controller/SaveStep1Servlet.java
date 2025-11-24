package controller;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/saveStep1")
public class SaveStep1Servlet extends HttpServlet {
 
    private static final long serialVersionUID = 1L;

    /**
     * 회원가입 1단계 정보를 HTTP 세션에 저장하는 POST 요청 처리 메서드
     */
    @Override
    // ⭐️ ServletException과 IOException은 메서드 시그니처에서 던져지도록 유지
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // 응답 타입을 미리 설정합니다.
        response.setContentType("text/plain;charset=UTF-8");
        
        try {
            // 1. 인코딩 설정
            request.setCharacterEncoding("UTF-8");
            
            // 2. 클라이언트로부터 전송된 파라미터(데이터) 받기
            String name = request.getParameter("name");
            String birthdate = request.getParameter("birthdate");
            String email = request.getParameter("email");
            
            // 3. 서버 측 유효성 검사 및 필수 데이터 누락 확인
            if (name == null || name.trim().isEmpty() || 
                birthdate == null || birthdate.trim().isEmpty() || 
                email == null || email.trim().isEmpty()) {
                
                // 오류 진단 강화: 누락된 필드를 명시적으로 출력
                StringBuilder errorMsg = new StringBuilder("error: missing required data. Check fields: ");
                if (name == null || name.trim().isEmpty()) errorMsg.append("[name] ");
                if (birthdate == null || birthdate.trim().isEmpty()) errorMsg.append("[birthdate] ");
                if (email == null || email.trim().isEmpty()) errorMsg.append("[email]");
                
                System.err.println("ERROR: 필수 데이터 누락. " + errorMsg.toString());
                
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400 Bad Request
                response.getWriter().write(errorMsg.toString());
                return;
            }

            // 4. HTTP 세션 가져오기 또는 새로 생성하기
            HttpSession session = request.getSession();

            // 5. 세션에 1단계 정보 임시 저장
            session.setAttribute("reg_name", name);
            session.setAttribute("reg_birthdate", birthdate);
            session.setAttribute("reg_email", email);
            
            // 6. 성공 응답
            response.getWriter().write("success: Step 1 data saved. Proceed to next step.");

            System.out.println("DEBUG: Step 1 data saved to session for email: " + email);
            
        } catch (IOException e) {
            // ⭐️ IOException 처리 (try 블록 내에서 request/response I/O 문제 발생 시)
            System.err.println("ERROR: IOException during request/response handling.");
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500 Internal Server Error
            response.getWriter().write("fatal_error: IO error during communication.");
            
        } catch (Exception e) {
            // ⭐️ 기타 예측하지 못한 모든 런타임 오류 처리 (예: NullPointerException 등)
            System.err.println("ERROR: Unexpected runtime exception.");
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500 Internal Server Error
            response.getWriter().write("fatal_error: An unexpected error occurred: " + e.getClass().getName());
        }
        
        // 🚨 ServletException은 메서드 시그니처에 정의되어 WAS에 의해 처리되므로 별도의 catch 블록이 필요 없습니다.
    }
}