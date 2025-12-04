// server.js

// 1. 모듈 로드 및 초기 설정
const express = require('express');
const bodyParser = require('body-parser');
const session = require('express-session');
const { createClient } = require('@supabase/supabase-js');
const nodemailer = require('nodemailer');
const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '.env') }); // .env 파일 로드

const app = express();
const PORT = 3000;

// 2. 환경 변수 로드 및 Supabase 클라이언트 초기화
const SUPABASE_URL = process.env.SUPABASE_URL;
const SUPABASE_ANON_KEY = process.env.SUPABASE_ANON_KEY;
const SUPABASE_SERVICE_KEY = process.env.SUPABASE_SERVICE_KEY; // 서비스 키를 사용하여 서버에서 인증을 우회할 수 있습니다.
const EMAIL_USER = process.env.EMAIL_USER;
const EMAIL_PASS = process.env.EMAIL_PASS;

if (!SUPABASE_URL || !SUPABASE_ANON_KEY || !EMAIL_USER || !EMAIL_PASS) {
    console.error("❌ 환경 변수가 제대로 설정되지 않았습니다. .env 파일을 확인하세요.");
    process.exit(1);
}

// ⚠️ 서버 측에서 서비스 키를 사용해 권한 높은 작업 수행
const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_KEY);

// 3. 미들웨어 설정
// URL-encoded 데이터를 파싱합니다 (Content-Type: application/x-www-form-urlencoded)
app.use(bodyParser.urlencoded({ extended: true }));
// 정적 파일 제공 (HTML, CSS, JS, 이미지 파일 등)
app.use(express.static(path.join(__dirname, 'public'))); 
app.use(express.static(path.join(__dirname, 'views'))); // views 폴더를 추가 (HTML 파일이 여기에 있다고 가정)

// 세션 설정 (인증 코드와 1단계 데이터를 임시 저장)
app.use(session({
    secret: 'super-secret-key-for-minigame', // 세션 암호화 키
    resave: false,
    saveUninitialized: false,
    cookie: { secure: process.env.NODE_ENV === 'production', maxAge: 1000 * 60 * 10 } // 10분 유지
}));


// 4. Nodemailer 설정 (이메일 발송)
const transporter = nodemailer.createTransport({
    service: 'gmail', // Gmail 서비스 사용
    auth: {
        user: EMAIL_USER,
        pass: EMAIL_PASS,
    }
});

// 5. 서버 라우팅 정의

// 5-1. 이메일 중복 확인 라우트
app.post('/checkDuplicate', async (req, res) => {
    const { type, value } = req.body;
    
    if (type !== 'email' || !value) {
        return res.status(400).send("invalid request");
    }

    try {
        const { data, error } = await supabase
            .from('users') // ⚠️ 데이터베이스 테이블 이름은 'users'로 가정합니다.
            .select('email')
            .eq('email', value)
            .single();

        if (error && error.code !== 'PGRST116') { // PGRST116은 '결과 없음' 오류 코드
            console.error("Supabase Error:", error);
            return res.status(500).send("server error");
        }

        if (data) {
            // 이메일이 이미 존재함
            return res.send("duplicate");
        } else {
            // 사용 가능
            return res.send("available");
        }

    } catch (e) {
        console.error("Unexpected Error:", e);
        return res.status(500).send("server error");
    }
});

// 5-2. 인증번호 전송 라우트
app.post('/sendCode', async (req, res) => {
    const { email } = req.body;
    
    if (!email) {
        return res.status(400).send("이메일 주소를 입력해주세요.");
    }
    
    // 6자리 랜덤 인증번호 생성
    const authCode = Math.floor(100000 + Math.random() * 900000).toString();
    
    // 세션에 인증번호 저장 (3분 유효)
    req.session.auth = {
        email: email,
        code: authCode,
        expires: Date.now() + 180000 // 3분 후 만료 (180,000ms)
    };

    const mailOptions = {
        from: EMAIL_USER,
        to: email,
        subject: '[MINIGAME] 회원가입 인증번호 안내',
        html: `<p>안녕하세요. 미니게임 회원가입을 위한 인증번호입니다.</p>
               <h1 style="color: #d89617;">${authCode}</h1>
               <p>인증번호는 3분간 유효합니다.</p>`
    };

    try {
        await transporter.sendMail(mailOptions);
        res.send("인증번호 전송 완료!");
    } catch (error) {
        console.error('Email Send Error:', error);
        // 세션에 저장된 인증 정보 제거
        delete req.session.auth; 
        res.status(500).send("이메일 전송에 실패했습니다.");
    }
});

// 5-3. 인증번호 확인 라우트
app.post('/verifyCode', (req, res) => {
    const { email, authCode } = req.body;

    if (!email || !authCode) {
        return res.status(400).send("이메일 또는 인증번호를 입력해주세요.");
    }
    
    const sessionAuth = req.session.auth;

    // 1. 세션 정보 존재 및 이메일 일치 확인
    if (!sessionAuth || sessionAuth.email !== email) {
        return res.status(400).send("인증 정보를 찾을 수 없습니다. 인증번호를 다시 요청해주세요.");
    }

    // 2. 만료 시간 확인
    if (Date.now() > sessionAuth.expires) {
        delete req.session.auth; // 만료된 세션 정보 제거
        return res.status(400).send("인증번호가 만료되었습니다. 다시 요청해주세요.");
    }

    // 3. 인증번호 일치 확인
    if (sessionAuth.code === authCode) {
        // 성공 시, 인증 완료 플래그를 세션에 저장하고 인증번호 정보는 삭제
        req.session.isEmailVerified = true;
        delete req.session.auth; 
        return res.send("인증 성공");
    } else {
        return res.status(400).send("인증번호가 일치하지 않습니다.");
    }
});


// 5-4. 1단계 데이터 세션 저장 라우트
app.post('/saveStep1', (req, res) => {
    const { name, birthdate, email } = req.body;
    
    if (!name || !birthdate || !email) {
        return res.status(400).send("필수 데이터(이름, 생년월일, 이메일)가 누락되었습니다.");
    }
    
    // ⚠️ 이메일 인증이 완료되었는지 확인
    if (!req.session.isEmailVerified) {
        return res.status(403).send("이메일 인증이 완료되지 않았습니다.");
    }

    // 세션에 1단계 데이터 저장
    req.session.registerStep1 = { name, birthdate, email };
    
    // 인증 완료 플래그는 사용했으므로 삭제 (선택 사항)
    delete req.session.isEmailVerified; 

    // 성공 응답
    res.send("success: step1 data saved in session");
});


// 6. 서버 시작
app.listen(PORT, () => {
    console.log(`🚀 Server is running on http://localhost:${PORT}`);
});