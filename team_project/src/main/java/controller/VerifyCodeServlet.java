package controller;

import DB.DBUtil;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 사용자 입력 인증 코드를 DB에 저장된 코드와 비교하여 유효성을 검증하는 서블릿입니다.
 * (HTML 폼에서 'email'과 'authCode'를 받습니다.)
 */
// @WebServlet("/test/resister/verifyCode")
public class VerifyCodeServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=UTF-8");

        // 1. 사용자 입력 파라미터 받기
        // JavaScript에서 formData.append('email', email)로 전송했으므로 email 파라미터를 받습니다.
        String email = request.getParameter("email"); 
        // HTML 폼의 <input name="authCode">에서 전송된 값을 받습니다.
        String inputCode = request.getParameter("authCode"); 

        if (email == null || inputCode == null || email.trim().isEmpty() || inputCode.trim().isEmpty()) {
            response.getWriter().println("인증 실패. 이메일 또는 인증번호가 누락되었습니다.");
            return;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            
            // 2. SQL 쿼리 준비: 일치, 만료 여부, 미사용 코드를 확인
            String sql = "SELECT * FROM email_verification WHERE email = ? AND verification_code = ? AND expires_at > NOW() AND is_verified = 0";
            
            ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, inputCode);
            
            rs = ps.executeQuery();

            if (rs.next()) {
                // 3. 인증 성공 시, DB 상태 업데이트 (코드가 사용되었음을 표시)
                String updateSql = "UPDATE email_verification SET is_verified = 1 WHERE email = ?";
                PreparedStatement psUpdate = conn.prepareStatement(updateSql);
                psUpdate.setString(1, email);
                psUpdate.executeUpdate();
                psUpdate.close();

                System.out.println("[INFO] 인증 성공: " + email);
                response.getWriter().println("인증 성공! 회원가입을 완료할 준비가 되었습니다.");
                
                // 🚨 여기에 회원가입 최종 단계로 포워딩하는 로직을 추가합니다.

            } else {
                // 4. 인증 실패 (코드 불일치, 만료, 이미 사용됨 등)
                response.getWriter().println("인증 실패. 코드가 유효하지 않거나 만료되었습니다. 다시 시도해주세요.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.getWriter().println("데이터베이스 오류: " + e.getMessage());
        } finally {
            // 5. 리소스 해제
            try { if (rs != null) rs.close(); } catch (SQLException ignore) {}
            try { if (ps != null) ps.close(); } catch (SQLException ignore) {}
            try { if (conn != null) conn.close(); } catch (SQLException ignore) {}
        }
    }
}