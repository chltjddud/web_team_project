package DB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBUtil {

    // ⚠️ [필수] 아래 정보를 실제 Supabase 연결 정보로 변경해야 합니다.
    private static final String DB_HOST = "db.wtemonhgawgmgpiqqfob.supabase.co"; // 예: 'db.abcdefghijklmn.supabase.co'
    private static final String DB_PORT = "5432";
    private static final String DB_NAME = "postgres"; // 기본 DB 이름
    
    private static final String URL = "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
    
    private static final String USER = "postgres"; // Supabase의 기본 사용자
    private static final String PASSWORD = "iJPDxjDcfGf7ybMe"; // ⚠️ 실제 DB 비밀번호로 변경

    static {
        try {
            // MySQL 드라이버 대신 PostgreSQL 드라이버를 로드합니다.
            Class.forName("org.postgresql.Driver"); 
            System.out.println("PostgreSQL JDBC Driver 로드 성공.");
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL JDBC Driver를 찾을 수 없습니다. 라이브러리(.jar)를 확인하세요.");
            e.printStackTrace();
        }
    }

    /**
     * 데이터베이스 연결을 반환합니다.
     * @return Connection 객체
     * @throws SQLException 연결 실패 시 발생
     */
    public static Connection getConnection() throws SQLException {
        // Supabase DB에 PostgreSQL JDBC를 사용하여 연결을 시도합니다.
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    
    /**
     * JDBC 자원을 안전하게 닫는 메서드 (Connection, PreparedStatement, ResultSet)
     */
    public static void close(Connection conn, PreparedStatement pstmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            System.err.println("DB 자원 해제 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * ResultSet이 없는 경우의 오버로딩 (INSERT, UPDATE 등에 사용)
     */
    public static void close(Connection conn, PreparedStatement pstmt) {
        close(conn, pstmt, null);
    }
}