package controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import DB.DBUtil;

// @WebServlet("/api/saveScore")
public class SaveScoreServlet extends HttpServlet {
    private static final long serialVersionUID = 1L; // 🐞 직렬화 경고 해결
    private static final int MEMORY_GAME_ID = 1; // ⚠️ 메모리 게임의 실제 game_id로 변경하세요.

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            // 로그인되지 않은 경우
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"success\": false, \"message\": \"로그인이 필요합니다.\"}");
            return;
        }
        
        // ⚠️ 세션에서 user_id 가져오기 (실제 세션 키로 변경)
        int userId = (Integer) session.getAttribute("user_id"); 
        
        // ⚠️ 점수 파라미터 가져오기 (JSON 요청 본문을 파싱했다고 가정)
        int currentScore; 
        try {
            // JavaScript에서 JSON.stringify({ score: score })로 보냈다고 가정하고, 
            // 요청 본문을 읽어 파싱합니다. (간단하게 구현)
            String body = request.getReader().lines().reduce("", (accumulator, line) -> accumulator + line);
            // JSON 파싱 (예: {"score": 100} -> 100 추출)
            currentScore = Integer.parseInt(body.replaceAll("[^0-9]", "")); 
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\": false, \"message\": \"잘못된 점수 형식입니다.\"}");
            return;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            int existingScore = -1; // -1은 기록이 없음을 의미

            // 1. 기존 최고 기록 조회
            String selectSql = "SELECT score FROM high_scores WHERE user_id = ? AND game_id = ?";
            pstmt = conn.prepareStatement(selectSql);
            pstmt.setInt(1, userId);
            pstmt.setInt(2, MEMORY_GAME_ID);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                existingScore = rs.getInt("score");
            }
            DBUtil.close(null, pstmt, rs); // 첫 번째 pstmt와 rs 닫기

            // 2. 점수 비교 및 저장/업데이트 로직
            boolean updated = false;
            
            // ⭐️ 횟수(score)가 기존 기록보다 적을 때 (currentScore < existingScore) 또는 최초 기록일 때 저장/업데이트 ⭐️
            if (existingScore == -1 || currentScore < existingScore) {
                
                String upsertSql;
                if (existingScore == -1) {
                    // 삽입 (최초 기록)
                    upsertSql = "INSERT INTO high_scores (user_id, game_id, score) VALUES (?, ?, ?)";
                    pstmt = conn.prepareStatement(upsertSql);
                    pstmt.setInt(1, userId);
                    pstmt.setInt(2, MEMORY_GAME_ID);
                    pstmt.setInt(3, currentScore);
                } else {
                    // 업데이트 (기록 갱신: 횟수가 더 적음)
                    upsertSql = "UPDATE high_scores SET score = ?, recorded_at = CURRENT_TIMESTAMP WHERE user_id = ? AND game_id = ?";
                    pstmt = conn.prepareStatement(upsertSql);
                    pstmt.setInt(1, currentScore);
                    pstmt.setInt(2, userId);
                    pstmt.setInt(3, MEMORY_GAME_ID);
                    updated = true;
                }
                
                pstmt.executeUpdate();
                DBUtil.close(null, pstmt); // pstmt 닫기
            }
            
            // 3. 최종 최고 기록 다시 조회하여 클라이언트에 반환
            int finalHighScore = currentScore;
            String finalSelectSql = "SELECT score FROM high_scores WHERE user_id = ? AND game_id = ?";
            pstmt = conn.prepareStatement(finalSelectSql);
            pstmt.setInt(1, userId);
            pstmt.setInt(2, MEMORY_GAME_ID);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                finalHighScore = rs.getInt("score");
            }
            
            // JSON 응답
            out.print(String.format("{\"success\": true, \"highScore\": %d, \"currentScore\": %d, \"updated\": %b}", 
                finalHighScore, currentScore, updated));


        } catch (Exception e) {
            System.err.println("점수 저장 오류: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\": false, \"message\": \"서버 내부 오류 발생.\"}");
        } finally {
            // 🐞 DBUtil.close 순서 오류 해결: (Connection, PreparedStatement, ResultSet) 순서
            DBUtil.close(conn, pstmt, rs);
        }
    }
}