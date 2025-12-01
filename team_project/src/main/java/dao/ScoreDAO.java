package dao; 

import DB.DBUtil; 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class ScoreDAO {

    private static final String MEMORY_GAME_NAME = "memory_game"; 
    
    // -------------------------------------------------------------
    // 1. 사용자 ID 조회
    // -------------------------------------------------------------
    public int getUserIdByLoginID(String loginID) {
        String sql = "SELECT user_id FROM users WHERE loginID = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int userId = 0;
        try {
            conn = DBUtil.getConnection(); 
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, loginID);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                userId = rs.getInt("user_id");
            }
        } catch (SQLException e) {
            System.err.println("--- 🚨 ScoreDAO.getUserIdByLoginID 오류: " + sql + " ---");
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return userId;
    }
    
    // -------------------------------------------------------------
    // 2. 게임 ID 조회
    // -------------------------------------------------------------
    public int getGameId() {
        String sql = "SELECT game_id FROM games WHERE game_name = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int gameId = 0;
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, MEMORY_GAME_NAME);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                gameId = rs.getInt("game_id");
            }
        } catch (SQLException e) {
            System.err.println("--- 🚨 ScoreDAO.getGameId 오류: " + sql + " ---");
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return gameId;
    }

    // -------------------------------------------------------------
    // 3. 모든 기록 저장 (scores 테이블)
    // -------------------------------------------------------------
    public boolean saveScore(int userId, int gameId, int score) {
        String sql = "INSERT INTO scores (user_id, game_id, score, played_at) VALUES (?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        try {
            conn = DBUtil.getConnection(); 
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.setInt(2, gameId);
            pstmt.setInt(3, score);           
            pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                success = true;
            }
        } catch (SQLException e) {
            System.err.println("--- 🚨 ScoreDAO.saveScore DB 저장 오류: " + sql + " (user: " + userId + ", score: " + score + ") ---");
            e.printStackTrace(); 
        } finally {
            DBUtil.close(conn, pstmt); 
        }
        return success;
    }

    // -------------------------------------------------------------
    // 4. 최고 점수 저장 (high_scores 테이블) - 규칙 분기 처리
    // -------------------------------------------------------------
    public boolean saveHighScore(int userId, int gameId, int score, boolean isMemoryGame) {
        // ... (SQL 구성 로직은 이전과 동일)
        String updateCondition;
        if (isMemoryGame) {
            updateCondition = "VALUES(score) < score";
        } else {
            updateCondition = "VALUES(score) > score";
        }

        String sql = "INSERT INTO high_scores (user_id, game_id, score, recorded_at) VALUES (?, ?, ?, NOW()) "
                   + "ON DUPLICATE KEY UPDATE "
                   + "score = CASE WHEN " + updateCondition + " THEN VALUES(score) ELSE score END, "
                   + "recorded_at = CASE WHEN " + updateCondition + " THEN NOW() ELSE recorded_at END";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean updated = false;

        try {
            conn = DBUtil.getConnection(); 
            pstmt = conn.prepareStatement(sql);
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, gameId);
            pstmt.setInt(3, score); 
            
            int rowsAffected = pstmt.executeUpdate();
            
            // rowsAffected가 1(삽입) 또는 2(갱신)인 경우 갱신 성공으로 간주
            if (rowsAffected > 0) {
                updated = true;
            }

        } catch (SQLException e) {
            System.err.println("--- 🚨 ScoreDAO.saveHighScore DB 저장/갱신 오류: " + sql + " (user: " + userId + ", score: " + score + ") ---");
            e.printStackTrace(); 
        } finally {
            DBUtil.close(conn, pstmt); 
        }
        
        return updated;
    }
    
    // -------------------------------------------------------------
    // 5. 최고 점수 조회 (high_scores 테이블)
    // -------------------------------------------------------------
    public Integer getHighScore(int userId, int gameId) {
        String sql = "SELECT score FROM high_scores WHERE user_id = ? AND game_id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Integer highScore = null;

        try {
            conn = DBUtil.getConnection(); 
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.setInt(2, gameId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                highScore = rs.getInt("score");
            }
        } catch (SQLException e) {
            System.err.println("--- 🚨 ScoreDAO.getHighScore DB 조회 오류: " + sql + " ---");
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return highScore;
    }
}