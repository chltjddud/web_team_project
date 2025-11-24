// src/dao/ScoreDAO.java
package dao;

import DB.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ScoreDAO {

    // 점수 저장
    public boolean insertScore(int userId, int gameId, int score) {
        String sql = "INSERT INTO scores (user_id, game_id, score) VALUES (?, ?, ?)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, gameId);
            pstmt.setInt(3, score);

            int result = pstmt.executeUpdate();
            return result == 1;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 특정 게임의 TOP 10 랭킹 (username, best_score)
    public List<String> getTopRanking(int gameId) {
        String sql = 
            "SELECT u.username, MAX(s.score) AS best_score " +
            "FROM scores s " +
            "JOIN users u ON s.user_id = u.user_id " +
            "WHERE s.game_id = ? " +
            "GROUP BY s.user_id " +
            "ORDER BY best_score DESC " +
            "LIMIT 10";

        List<String> ranking = new ArrayList<>();

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, gameId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String row = rs.getString("username") + " - " + rs.getInt("best_score");
                    ranking.add(row);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ranking;
    }
}
