package model;

import java.io.Serializable;
import java.time.LocalDate; // Java 8 이상: DATE 타입 처리를 위해 사용

/**
 * 사용자 정보를 담는 데이터 모델 (DTO/Model)
 * 세션에 저장하기 위해 Serializable 인터페이스를 구현합니다.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // DB: users 테이블의 모든 필드
    private int userId;              // user_id (PK)
    private String loginId;          // loginID
    private String password;         // password
    private String name;             // name
    private LocalDate birth;         // birth
    private String email;            // email
    private boolean emailVerified;   // email_verified
    private String nickname;         // nickname

    // 기본 생성자
    public User() {}

    // 필드를 포함한 전체 생성자 (DB에서 로딩 시 사용)
    public User(int userId, String loginId, String password, String name, LocalDate birth, String email, boolean emailVerified, String nickname) {
        this.userId = userId;
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.birth = birth;
        this.email = email;
        this.emailVerified = emailVerified;
        this.nickname = nickname;
    }
    
    // Getter 및 Setter 메서드
    public int getUserId() {
        return userId;
    }
    public void setUserId(int userId) {
        this.userId = userId;
    }
    // (이하 나머지 필드에 대한 Getter/Setter는 가독성을 위해 생략하지만, 실제 코드에는 포함해야 합니다.)
    
    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getBirth() { return birth; }
    public void setBirth(LocalDate birth) { this.birth = birth; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}