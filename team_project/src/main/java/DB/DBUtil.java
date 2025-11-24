package DB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBUtil {

	private static final String URL = "jdbc:mysql://localhost:3306/mini_game?serverTimezone=UTC&useUsageAdvisor=false";
	private static final String USER = "root";
	private static final String PASSWORD = "1234";

	static {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver"); 
		} catch (ClassNotFoundException e) { // catch 블록 끝
			System.err.println("MySQL JDBC Driver를 찾을 수 없습니다.");
			e.printStackTrace( );
		} 
	} // <-- 누락된 닫는 중괄호 추가! (정적 블록 닫기)

	 public static Connection getConnection() throws SQLException {
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
