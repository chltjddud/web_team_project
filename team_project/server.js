// team_project/server.js (Vercel 배포 최적화 버전)

// 1. 필요한 모듈 불러오기 및 환경 변수 로드
require('dotenv').config(); 
const express = require('express');
const session = require('express-session');
const { createClient } = require('@supabase/supabase-js');
const nodemailer = require('nodemailer');
// Vercel은 정적 파일을 직접 호스팅하므로, path 모듈과 express.static은 제거합니다.
// const path = require('path'); 

const app = express();
const PORT = 3000; // 로컬 테스트용 포트

// ------------------------------------------------------------------
// 2. 환경 설정 및 초기화
// ------------------------------------------------------------------
const supabaseUrl = process.env.SUPABASE_URL;
const supabaseServiceKey = process.env.SUPABASE_SERVICE_KEY; 
const emailUser = process.env.EMAIL_USER;
const emailPass = process.env.EMAIL_PASS;

// Supabase 클라이언트 초기화
const supabase = (supabaseUrl && supabaseServiceKey) 
    ? createClient(supabaseUrl, supabaseServiceKey) 
    : { from: () => ({ select: () => ({ eq: () => ({ maybeSingle: () => ({ data: null, error: new Error("Supabase is not configured.") }), count: () => ({ count: 0, error: null }) }) }), upsert: () => ({ error: null }), update: () => ({ error: null }) }) };

// Nodemailer Transporter 초기화
const transporter = nodemailer.createTransport({
    service: 'gmail', 
    auth: { user: emailUser, pass: emailPass },
});

// ------------------------------------------------------------------
// 3. 미들웨어 설정
// ------------------------------------------------------------------

app.use(express.json()); 
app.use(express.urlencoded({ extended: true })); 

// 세션 미들웨어 설정
app.use(session({
    secret: 'a_very_secret_key_for_nodejs_session', 
    resave: false,
    saveUninitialized: false,
    // Vercel(Production) 환경에서는 secure: true 설정 권장
    cookie: { secure: process.env.NODE_ENV === 'production', maxAge: 1000 * 60 * 60 * 24 } 
}));


// ------------------------------------------------------------------
// 4. 라우트 핸들러 구현 (API 경로만 처리)
// ------------------------------------------------------------------

/**
 * 4-1. 중복 확인 라우트 (경로: /checkDuplicate)
 */
app.post('/checkDuplicate', async (req, res) => {
    const { type, value } = req.body; 
    res.type('text/plain;charset=UTF-8');
    
    if (!type || !value || value.trim() === '') {
        return res.status(400).send("error: type 또는 value가 누락되었습니다.");
    }
    let columnName = type === 'id' ? 'loginID' : (type === 'nickname' ? 'nickname' : (type === 'email' ? 'email' : null));
    if (!columnName) {
        return res.status(400).send("error: 유효하지 않은 type입니다.");
    }

    try {
        const { count, error } = await supabase
            .from('users')
            .select('user_id', { count: 'exact', head: true }) 
            .eq(columnName, value); 
        
        if (error) throw error;
        return res.send(count > 0 ? "duplicate" : "available");
    } catch (e) {
        console.error("DB 오류: 중복 확인 중 예외 발생", e);
        // Supabase 설정이 안 되어있다면 (로컬 테스트 목적)
        if (e.message.includes("Supabase is not configured")) return res.status(500).send("fatal_error: Supabase 설정이 필요합니다.");
        return res.status(500).send("fatal_error: 서버 DB 처리 중 오류 발생.");
    }
});


/**
 * 4-2. 이메일 인증번호 전송 라우트 (경로: /sendCode)
 */
app.post('/sendCode', async (req, res) => {
    const { email } = req.body;
    res.type('text/plain;charset=UTF-8');

    if (!email) return res.status(400).send("error: 이메일이 누락되었습니다.");

    const verificationCode = String(Math.floor(100000 + Math.random() * 900000));
    
    try {
        const now = new Date();
        const expiresAt = new Date(now.getTime() + 5 * 60000); 

        // 1. Supabase에 인증 코드 저장/업데이트
        const { error: dbError } = await supabase
            .from('email_verification')
            .upsert({ email, verification_code: verificationCode, created_at: now.toISOString(), expires_at: expiresAt.toISOString(), is_verified: false }, { onConflict: 'email', ignoreDuplicates: false });
        
        if (dbError) throw dbError;
        
        // 2. 이메일 전송
        const mailOptions = {
            from: emailUser, 
            to: email, 
            subject: 'MINIGAME 회원가입 인증번호입니다.',
            html: `<h1>인증번호: ${verificationCode}</h1><p>5분 이내에 입력해주세요.</p>`
        };

        transporter.sendMail(mailOptions, (error, info) => {
            if (error) {
                console.error("메일 전송 실패:", error);
                return res.send("메일 전송 실패: 서버 로그를 확인하세요.");
            }
            return res.send("전송 완료");
        });

    } catch (e) {
        console.error("서버 DB 또는 메일 전송 중 오류 발생:", e);
        return res.status(500).send(`메일 전송 또는 DB 저장 실패: ${e.message}. 로그를 확인하세요.`);
    }
});


/**
 * 4-3. 이메일 인증번호 확인 라우트 (VerifyCodeServlet 대체)
 * 경로: /verifyCode
 */
app.post('/verifyCode', async (req, res) => {
    const { email, authCode } = req.body;
    res.type('text/plain;charset=UTF-8');

    if (!email || !authCode) return res.status(400).send("error: 이메일 또는 인증번호가 누락되었습니다.");

    try {
        const now = new Date().toISOString();
        
        // 1. DB에서 유효한 인증 정보 조회
        const { data, error } = await supabase
            .from('email_verification')
            .select('*')
            .eq('email', email)
            .eq('verification_code', authCode)
            .eq('is_verified', false)
            .gt('expires_at', now) // 만료되지 않은 코드만
            .maybeSingle();

        if (error) throw error;

        if (data) {
            // 2. 인증 성공: is_verified 상태를 true로 업데이트
            const { error: updateError } = await supabase
                .from('email_verification')
                .update({ is_verified: true })
                .eq('email', email);

            if (updateError) throw updateError;
            return res.send("인증 성공");
        } else {
            return res.send("⚠️ 인증번호가 일치하지 않거나 만료되었습니다.");
        }
    } catch (e) {
        console.error("서버 DB 처리 중 오류 발생 (인증 코드 확인):", e);
        return res.status(500).send("fatal_error: 서버 DB 처리 중 오류 발생.");
    }
});


/**
 * 4-4. 1단계 데이터 세션 저장 라우트 (경로: /saveStep1)
 */
app.post('/saveStep1', (req, res) => {
    const { name, birthdate, email } = req.body;
    res.type('text/plain;charset=UTF-8');

    if (!name || !birthdate || !email) {
        return res.status(400).send("error: 회원가입 필수 정보(이름, 생년월일, 이메일)가 누락되었습니다.");
    }
    
    // Express 세션에 데이터 임시 저장
    req.session.reg_name = name;
    req.session.reg_birthdate = birthdate;
    req.session.reg_email = email;
    
    res.send("success: Step 1 data saved. Proceed to next step.");
});


// ------------------------------------------------------------------
// 5. Vercel 배포를 위한 export 및 로컬 실행
// ------------------------------------------------------------------

// ⭐️ Vercel이 서버리스 함수로 사용하도록 Express 앱 객체를 내보냅니다. ⭐️
module.exports = app;

// 로컬 테스트 환경(NODE_ENV가 'production'이나 'test'가 아닐 때)에서만 서버를 실행합니다.
if (process.env.NODE_ENV !== 'production' && process.env.NODE_ENV !== 'test') {
    app.listen(PORT, () => {
        console.log(`✅ Node.js 서버가 http://localhost:${PORT} 에서 실행 중입니다. (로컬 테스트용)`);
        // 로컬 테스트용 HTML 접근 주소: http://localhost:3000/test/resist/resist_1.html
    });
}