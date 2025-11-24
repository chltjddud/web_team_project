package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 데이터베이스 연결을 관리하는 싱글톤 클래스입니다.
 * JDBC 드라이버 로드 및 Connection 객체를 반환하는 역할을 합니다.
 */
public class DBManager {

    // 🔑 데이터베이스 연결 정보 (반드시 본인의 정보로 수정하세요!)
    private static final String DB_URL = "jdbc:mysql://localhost:3306/mini_game?serverTimezone=Asia/Seoul";
    private static final String DB_USER = "your_db_username"; // DB 사용자 이름
    private static final String DB_PASS = "your_db_password"; // DB 비밀번호

    // MySQL 드라이버 클래스 이름
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    private static DBManager instance;

    /**
     * 싱글톤 패턴을 위한 private 생성자입니다.
     * 생성자에서 JDBC 드라이버를 로드합니다.
     */
    private DBManager() {
        try {
            // JDBC 드라이버 로드
            Class.forName(DRIVER);
            System.out.println("✅ JDBC Driver Loaded Successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Error: MySQL JDBC Driver not found.");
            e.printStackTrace();
            // 애플리케이션 시작 불가
            throw new RuntimeException("JDBC Driver not found", e); 
        }
    }

    /**
     * DBManager의 싱글톤 인스턴스를 반환합니다.
     * @return DBManager 인스턴스
     */
    public static DBManager getInstance() {
        if (instance == null) {
            synchronized (DBManager.class) {
                if (instance == null) {
                    instance = new DBManager();
                }
            }
        }
        return instance;
    }

    /**
     * 새로운 데이터베이스 연결(Connection)을 가져옵니다.
     * @return 데이터베이스 Connection 객체
     * @throws SQLException 연결 실패 시 예외 발생
     */
    public Connection getConnection() throws SQLException {
        // 연결 풀(Connection Pool)을 사용해야 성능이 더 좋지만, 기본 예제에서는 단순 연결을 사용합니다.
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    /**
     * JDBC 자원(Connection, Statement, ResultSet)을 닫는 유틸리티 메소드입니다.
     */
    public void close(AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                } catch (Exception e) {
                    System.err.println("❌ Error closing JDBC resource: " + e.getMessage());
                }
            }
        }
    }
}