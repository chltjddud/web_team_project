package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import DB.DBUtil; 

public class UserDAO { 
    
    // ⭐️ 신규 추가: ID와 이메일로 비밀번호 조회 (요청 사항 반영) ⭐️
    private static final String FIND_PASSWORD_BY_ID_EMAIL_SQL =
        "SELECT password FROM users WHERE loginID = ? AND email = ?";
    
    // ⭐️ 기존 상수 ⭐️
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
    // 1. 중복 확인 범용 메서드 (변경 없음)
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
    // 2. 사용자 삽입(저장) 메서드 (변경 없음)
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
            pstmt.setString(4, birthdate); // birth
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
    // 3. ID 찾기 관련 메서드 (변경 없음)
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
    // 4. PW 찾기 관련 메서드 (변경 및 추가)
    // -------------------------------------------------------------------
    
    // ⭐️ 신규 메서드: 실제 비밀번호 조회 ⭐️
    /**
     * PW 찾기 시, ID와 이메일이 모두 일치하는 사용자의 비밀번호를 조회합니다.
     * (경고: 배포 시에는 임시 비밀번호를 생성/업데이트하는 방식으로 변경해야 합니다.)
     * @param loginId 사용자가 입력한 ID
     * @param email 사용자가 입력한 이메일
     * @return 일치하는 사용자의 실제 비밀번호, 없으면 null
     */
    public String findPasswordByLoginIdAndEmail(String loginId, String email) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String actualPassword = null;

        try {
            conn = DBUtil.getConnection(); 
            // ⭐️ 추가된 SQL 상수 사용 ⭐️
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
    
    // 기존 메서드 (변경 없음)
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
            
            // 0보다 큰 값(사용자 존재)이 반환되면 true
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

    // 기존 메서드 (변경 없음 - 임시 비밀번호 방식에서는 사용되지만, 여기서는 그대로 유지)
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
}