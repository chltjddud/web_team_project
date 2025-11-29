package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import DB.DBUtil;
import model.Score;

/**
 * 게임 점수(scores 테이블) 관련 데이터베이스 작업을 처리합니다.
 */
public class ScoreDAO {

    // ⭐️ 점수 저장 쿼리 ⭐️
    private static final String INSERT_SCORE_SQL = 
        "INSERT INTO scores (user_id, game_id, score) VALUES (?, ?, ?)";

    /**
     * 새로운 게임 점수를 scores 테이블에 저장합니다.
     * @param score 저장할 Score 객체 (userId, gameId, score 포함)
     * @return 성공 시 true, 실패 시 false
     */
    public boolean saveScore(Score score) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(INSERT_SCORE_SQL);
            
            // Score 객체의 정보를 쿼리에 바인딩
            pstmt.setInt(1, score.getUserId());
            pstmt.setInt(2, score.getGameId());
            pstmt.setInt(3, score.getScore());
            
            // 쿼리 실행 (영향을 받은 행의 수가 1 이상이면 성공)
            if (pstmt.executeUpdate() > 0) {
                success = true;
            }
        } catch (SQLException e) {
            System.err.println("DB 오류: 게임 점수 저장 중 예외 발생");
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt);
        }
        return success;
    }
    
    // 추가로 최고 점수 조회, 랭킹 조회 등의 메서드를 구현할 수 있습니다.
}