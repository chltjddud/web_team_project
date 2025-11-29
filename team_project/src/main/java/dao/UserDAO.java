package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date; 
import java.time.LocalDate; 
import DB.DBUtil; 
import model.User; 

public class UserDAO { 
    
    // ⭐️ 로그인 인증 및 전체 사용자 정보 조회를 위한 쿼리 ⭐️
    private static final String FIND_USER_BY_CREDENTIALS_SQL = 
        "SELECT user_id, loginID, password, name, birth, email, email_verified, nickname FROM users WHERE loginID = ? AND password = ?";
    
    // ⭐️ 기존 상수 ⭐️
    private static final String LOGIN_CHECK_SQL = 
        "SELECT COUNT(*) FROM users WHERE loginID = ? AND password = ?";
    
    private static final String FIND_PASSWORD_BY_ID_EMAIL_SQL =
        "SELECT password FROM users WHERE loginID = ? AND email = ?";
    
    // email_verified 필드 추가
    private static final String INSERT_USER_SQL = 
        "INSERT INTO users (loginID, password, name, birth, email, nickname, email_verified) VALUES (?, ?, ?, ?, ?, ?, ?)";
    
    private static final String FIND_ID_BY_NAME_EMAIL_SQL = 
        "SELECT loginID FROM users WHERE name = ? AND email = ?";
    
    private static final String CHECK_USER_EXISTS_BY_NAME_EMAIL_SQL = 
        "SELECT COUNT(*) FROM users WHERE name = ? AND email = ?";
        
    private static final String CHECK_USER_EXISTS_BY_ID_EMAIL_SQL =
        "SELECT COUNT(*) FROM users WHERE loginID = ? AND email = ?";
    
    private static final String UPDATE_PASSWORD_SQL =
        "UPDATE users SET password = ? WHERE loginID = ?";


    // -------------------------------------------------------------------
    // 1. 중복 확인 범용 메서드
    // -------------------------------------------------------------------
    public boolean isValueDuplicate(String type, String value) {
        String sql = "SELECT COUNT(*) FROM users WHERE " + type + " = ?"; 
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean isDuplicate = false;

        try {
            conn = DBUtil.getConnection(); 
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, value);

            rs = pstmt.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                isDuplicate = true;
            }

        } catch (SQLException e) {
            System.err.println("DB 오류: " + type + " 중복 확인 중 예외 발생");
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        
        return isDuplicate;
    }
    
    public boolean isUserIdDuplicate(String loginId) {
        return isValueDuplicate("loginID", loginId); 
    }
    
    public boolean isNicknameDuplicate(String nickname) {
        return isValueDuplicate("nickname", nickname); 
    }
    
    public boolean isEmailDuplicate(String email) {
        return isValueDuplicate("email", email);
    }
    
    // -------------------------------------------------------------------
    // 2. 사용자 삽입(저장) 메서드
    // -------------------------------------------------------------------

    public boolean insertUser(String loginId, String password, String nickname, String name, String email, String birthdate) {
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtil.getConnection(); 
            pstmt = conn.prepareStatement(INSERT_USER_SQL);
            
            // SQL 쿼리의 ? 순서에 맞게 파라미터 설정
            pstmt.setString(1, loginId);   // loginID
            pstmt.setString(2, password);  // password
            pstmt.setString(3, name);      // name
            pstmt.setString(4, birthdate); // birth (DB DATE 형식에 맞는 문자열)
            pstmt.setString(5, email);     // email
            pstmt.setString(6, nickname);  // nickname
            pstmt.setBoolean(7, false);    // email_verified
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("DB 사용자 등록 오류 (SQL Exception): " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            DBUtil.close(conn, pstmt); 
        }
    }
    
    // -------------------------------------------------------------------
    // 3. ID 찾기 관련 메서드
    // -------------------------------------------------------------------

    public boolean checkUserExistsByNameAndEmail(String name, String email) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean exists = false;

        try {
            conn = DBUtil.getConnection(); 
            pstmt = conn.prepareStatement(CHECK_USER_EXISTS_BY_NAME_EMAIL_SQL);
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            rs = pstmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                exists = true;
            }
        } catch (SQLException e) {
            System.err.println("DB 오류: 이름/이메일로 사용자 존재 확인 중 예외 발생");
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return exists;
    }
    
    public String findIdByNameAndEmail(String name, String email) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String foundId = null;

        try {
            conn = DBUtil.getConnection(); 
            pstmt = conn.prepareStatement(FIND_ID_BY_NAME_EMAIL_SQL);
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                foundId = rs.getString("loginID"); 
            }
        } catch (SQLException e) {
            System.err.println("DB 오류: 이름/이메일로 ID 조회 중 예외 발생");
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return foundId;
    }

    // -------------------------------------------------------------------
    // 4. PW 찾기 관련 메서드
    // -------------------------------------------------------------------
    
    public String findPasswordByLoginIdAndEmail(String loginId, String email) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String actualPassword = null;

        try {
            conn = DBUtil.getConnection(); 
            pstmt = conn.prepareStatement(FIND_PASSWORD_BY_ID_EMAIL_SQL); 
            pstmt.setString(1, loginId);
            pstmt.setString(2, email);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                actualPassword = rs.getString("password"); 
            }
        } catch (SQLException e) {
            System.err.println("DB 오류: ID/이메일로 비밀번호 조회 중 예외 발생");
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return actualPassword;
    }
    
    public boolean checkUserExistsByIdAndEmail(String loginId, String email) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean exists = false;

        try {
            conn = DBUtil.getConnection(); 
            pstmt = conn.prepareStatement(CHECK_USER_EXISTS_BY_ID_EMAIL_SQL);
            pstmt.setString(1, loginId);
            pstmt.setString(2, email);
            rs = pstmt.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {
                exists = true;
            }
        } catch (SQLException e) {
            System.err.println("DB 오류: ID/이메일로 사용자 존재 확인 중 예외 발생");
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return exists;
    }

    public boolean updatePassword(String loginId, String newPassword) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DBUtil.getConnection(); 
            pstmt = conn.prepareStatement(UPDATE_PASSWORD_SQL);
            
            pstmt.setString(1, newPassword); 
            pstmt.setString(2, loginId);      
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                success = true;
            }

        } catch (SQLException e) {
            System.err.println("DB 오류: 비밀번호 업데이트 중 예외 발생");
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt);
        }
        return success;
    }

    // -------------------------------------------------------------------
    // 5. 로그인 인증 및 사용자 정보 조회 메서드 (User 객체 반환) ⭐️추가됨⭐️
    // -------------------------------------------------------------------

    /**
     * ID와 비밀번호가 일치하는 사용자의 정보를 조회합니다.
     * @param loginId 사용자가 입력한 ID
     * @param password 사용자가 입력한 비밀번호
     * @return 일치하는 사용자가 있으면 User 객체를, 없으면 null을 반환
     */
    public User findUserByCredentials(String loginId, String password) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        User user = null;
        
        try {
            conn = DBUtil.getConnection(); 
            // ⭐️ FIND_USER_BY_CREDENTIALS_SQL 사용 ⭐️
            pstmt = conn.prepareStatement(FIND_USER_BY_CREDENTIALS_SQL); 
            pstmt.setString(1, loginId);
            pstmt.setString(2, password);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                // DB의 DATE를 Java 8의 LocalDate로 변환
                Date birthSqlDate = rs.getDate("birth");
                
                user = new User(
                    rs.getInt("user_id"),
                    rs.getString("loginID"), 
                    rs.getString("password"),
                    rs.getString("name"), 
                    birthSqlDate != null ? birthSqlDate.toLocalDate() : null, // DB DATE -> LocalDate
                    rs.getString("email"),
                    rs.getBoolean("email_verified"),
                    rs.getString("nickname")
                );
            }
        } catch (SQLException e) {
            System.err.println("DB 오류: 사용자 정보 조회 중 예외 발생");
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return user;
    }
    
    // -------------------------------------------------------------------
    // 6. 로그인 인증 메서드 (COUNT만 반환) (기존 5번 로직 유지)
    // -------------------------------------------------------------------

    /**
     * ID와 비밀번호가 모두 일치하는 사용자가 있는지 확인하여 로그인 인증을 수행합니다.
     * @param loginId 사용자가 입력한 ID
     * @param password 사용자가 입력한 비밀번호
     * @return 일치하는 사용자가 있으면 true, 없으면 false
     */
    public boolean checkLoginCredentials(String loginId, String password) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean isValid = false;

        try {
            conn = DBUtil.getConnection(); 
            pstmt = conn.prepareStatement(LOGIN_CHECK_SQL); 
            pstmt.setString(1, loginId);
            pstmt.setString(2, password);
            rs = pstmt.executeQuery();
            
            // COUNT(*) 결과가 1 이상이면 인증 성공
            if (rs.next() && rs.getInt(1) > 0) {
                isValid = true;
            }
        } catch (SQLException e) {
            System.err.println("DB 오류: 로그인 인증 중 예외 발생");
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return isValid;
    }
}