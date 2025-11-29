package model;

import java.time.LocalDateTime; // Java 8 이상: DATETIME 타입 처리를 위해 사용

/**
 * 게임 점수 정보를 담는 데이터 모델 (DTO/Model)
 */
public class Score {
    
    private int scoreId;        // score_id (PK)
    private int userId;         // user_id (FK)
    private int gameId;         // game_id (FK)
    private int score;          // score
    private LocalDateTime playedAt; // played_at (DEFAULT CURRENT_TIMESTAMP)

    // DB에서 조회 시 사용할 전체 생성자
    public Score(int scoreId, int userId, int gameId, int score, LocalDateTime playedAt) {
        this.scoreId = scoreId;
        this.userId = userId;
        this.gameId = gameId;
        this.score = score;
        this.playedAt = playedAt;
    }

    // 새로운 점수 저장 시 사용할 생성자 (ID와 시간은 DB에서 자동 생성)
    public Score(int userId, int gameId, int score) {
        this.userId = userId;
        this.gameId = gameId;
        this.score = score;
    }
    
    // Getter 및 Setter 메서드 (가독성을 위해 생략)
    
    public int getScoreId() { return scoreId; }
    public void setScoreId(int scoreId) { this.scoreId = scoreId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getGameId() { return gameId; }
    public void setGameId(int gameId) { this.gameId = gameId; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public LocalDateTime getPlayedAt() { return playedAt; }
    public void setPlayedAt(LocalDateTime playedAt) { this.playedAt = playedAt; }
}