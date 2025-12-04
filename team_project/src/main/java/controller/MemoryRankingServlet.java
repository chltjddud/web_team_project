package controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import DB.DBUtil; // ⚠️ DBUtil 클래스가 있는 패키지 경로로 변경해야 합니다. 

// 🐞 오류 1 해결: 이 클래스는 서블릿 파일 맨 위에 위치해야 합니다.
class RankingEntry {
    private String nickname;
    private int score;
    private int rank;

    public RankingEntry(String nickname, int score, int rank) {
        this.nickname = nickname;
        this.score = score;
        this.rank = rank;
    }
    
    // JSON 변환을 위한 Getter
    public String getNickname() { return nickname; }
    public int getScore() { return score; }
    public int getRank() { return rank; }
}

// @WebServlet("/api/memoryRanking")
public class MemoryRankingServlet extends HttpServlet {
    private static final long serialVersionUID = 1L; // 🐞 직렬화 경고 해결
    private static final int MEMORY_GAME_ID = 3; 

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException {

        // ⭐️ 디버그 시작 로그 ⭐️
        System.out.println("--- [MemoryRankingServlet Debug Start] ---");
        
        List<RankingEntry> rankingList = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        String sql = 
            "SELECT u.nickname, h.score " +
            "FROM high_scores h JOIN users u ON h.user_id = u.user_id " +
            "WHERE h.game_id = ? " + 
            "ORDER BY h.score ASC, h.recorded_at ASC " + 
            "LIMIT 10"; 
        
        System.out.println("DEBUG: 사용할 SQL 쿼리: " + sql.trim().replaceAll("\\s+", " "));
        System.out.println("DEBUG: MEMORY_GAME_ID 값: " + MEMORY_GAME_ID);

        try {
            // ⭐️ DB 연결 시도 로그 ⭐️
            System.out.println("DEBUG: DB 연결 시도 중...");
            conn = DBUtil.getConnection(); 
            System.out.println("DEBUG: ✅ DB 연결 성공.");
            
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, MEMORY_GAME_ID); 
            
            // ⭐️ 쿼리 실행 로그 ⭐️
            System.out.println("DEBUG: 쿼리 실행 중...");
            rs = pstmt.executeQuery();
            System.out.println("DEBUG: 쿼리 실행 완료.");

            int rank = 1;
            int loadedCount = 0;
            
            while (rs.next()) {
                String nickname = rs.getString("nickname");
                int score = rs.getInt("score");
                
                // ⭐️ 데이터 로드 확인 로그 (주석 처리 가능) ⭐️
                // System.out.println("DEBUG: 랭킹 데이터 로드됨 -> 순위: " + rank + ", 닉네임: " + nickname + ", 점수: " + score);
                
                rankingList.add(new RankingEntry(nickname, score, rank++));
                loadedCount++;
            }
            
            // ⭐️ 최종 데이터 개수 로그 ⭐️
            System.out.println("DEBUG: 총 로드된 랭킹 데이터 개수: " + loadedCount + "개");
            if (loadedCount == 0) {
                 System.out.println("DEBUG: ⚠️ DB에서 해당 game_id (" + MEMORY_GAME_ID + ")에 대한 데이터가 조회되지 않았습니다.");
            }

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            // JSON 변환 및 출력
            String jsonOutput = convertRankingListToJson(rankingList);
            PrintWriter out = response.getWriter();
            out.print(jsonOutput);
            out.flush();

        } catch (Exception e) {
            // ⭐️ 예외 발생 시 상세 로그 ⭐️
            System.err.println("DEBUG: ❌ DB 접근 중 치명적인 예외 발생: " + e.getMessage());
            e.printStackTrace(); 
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "랭킹 데이터를 불러오는 데 실패했습니다.");
        } finally {
            // 🐞 DBUtil.close 순서 오류 해결: (Connection, PreparedStatement, ResultSet) 순서
            DBUtil.close(conn, pstmt, rs); 
            System.out.println("DEBUG: DB 자원 해제 완료.");
            System.out.println("--- [MemoryRankingServlet Debug End] ---");
        }
    }
    
    /**
     * RankingEntry 리스트를 JSON 배열 문자열로 변환합니다. (JSON 라이브러리 미사용)
     */
    private String convertRankingListToJson(List<RankingEntry> rankingList) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        
        for (int i = 0; i < rankingList.size(); i++) {
            RankingEntry entry = rankingList.get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{");
            sb.append("\"rank\":").append(entry.getRank()).append(",");
            sb.append("\"nickname\":\"").append(entry.getNickname()).append("\",");
            sb.append("\"score\":").append(entry.getScore());
            sb.append("}");
        }
        
        sb.append("]");
        return sb.toString();
    }
}