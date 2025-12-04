// api/index.js (Vercel Serverless Function)

const express = require('express');
const bodyParser = require('body-parser');
const session = require('express-session');
const { createClient } = require('@supabase/supabase-js');
const nodemailer = require('nodemailer');
const path = require('path');
// Vercel 환경에서는 .env 파일을 직접 로드할 필요가 없으며,
// 환경 변수는 Vercel 설정에서 자동으로 로드됩니다.

const app = express();

// 1. 환경 변수 로드 및 Supabase 클라이언트 초기화 (Vercel 환경 변수 사용)
// ⚠️ Vercel 대시보드에서 환경 변수를 설정해야 합니다.
const SUPABASE_URL = process.env.SUPABASE_URL;
const SUPABASE_SERVICE_KEY = process.env.SUPABASE_SERVICE_KEY;
const EMAIL_USER = process.env.EMAIL_USER;
const EMAIL_PASS = process.env.EMAIL_PASS;

// ⚠️ 서비스 키를 사용하여 Supabase 클라이언트 생성
const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_KEY);

// 2. 미들웨어 설정
app.use(bodyParser.urlencoded({ extended: true }));

// Vercel에서 세션 데이터를 관리하는 것은 복잡하므로,
// 세션 secret을 환경 변수로 설정하는 것이 좋습니다.
// SECRET_SESSION_KEY도 Vercel 환경 변수에 추가해야 합니다.
app.use(session({
    secret: process.env.SECRET_SESSION_KEY || 'default-secret-for-dev', 
    resave: false,
    saveUninitialized: false,
    cookie: { 
        secure: process.env.NODE_ENV === 'production', // Vercel에서는 'production'으로 설정됨
        maxAge: 1000 * 60 * 10 
    }
}));


// 3. Nodemailer 설정 (EMAIL_PASS는 실제로는 '앱 비밀번호'여야 함)
const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
        user: EMAIL_USER,
        pass: EMAIL_PASS, 
    }
});


// 4. 기존 Express 라우팅 로직을 여기에 복사/붙여넣기 합니다.
// (server.js의 5-1 ~ 5-4 섹션의 모든 app.post(...) 코드를 여기에 복사합니다.)
// --- 여기에 /checkDuplicate, /sendCode, /verifyCode, /saveStep1 라우트가 들어갑니다. ---

// 5-1. 이메일 중복 확인 라우트
app.post('/checkDuplicate', async (req, res) => {
    // ... server.js의 동일한 로직 ...
    const { type, value } = req.body;
    
    if (type !== 'email' || !value) {
        return res.status(400).send("invalid request");
    }

    try {
        const { data, error } = await supabase
            .from('users') 
            .select('email')
            .eq('email', value)
            .single();

        if (error && error.code !== 'PGRST116') {
            console.error("Supabase Error:", error);
            return res.status(500).send("server error");
        }

        return res.send(data ? "duplicate" : "available");

    } catch (e) {
        console.error("Unexpected Error:", e);
        return res.status(500).send("server error");
    }
});

// 5-2. 인증번호 전송 라우트
app.post('/sendCode', async (req, res) => {
    // ... server.js의 동일한 로직 ...
    const { email } = req.body;
    
    if (!email) {
        return res.status(400).send("이메일 주소를 입력해주세요.");
    }
    
    const authCode = Math.floor(100000 + Math.random() * 900000).toString();
    
    req.session.auth = {
        email: email,
        code: authCode,
        expires: Date.now() + 180000 
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
        delete req.session.auth; 
        res.status(500).send("이메일 전송에 실패했습니다.");
    }
});

// 5-3. 인증번호 확인 라우트
app.post('/verifyCode', (req, res) => {
    // ... server.js의 동일한 로직 ...
    const { email, authCode } = req.body;

    if (!email || !authCode) {
        return res.status(400).send("이메일 또는 인증번호를 입력해주세요.");
    }
    
    const sessionAuth = req.session.auth;

    if (!sessionAuth || sessionAuth.email !== email) {
        return res.status(400).send("인증 정보를 찾을 수 없습니다. 인증번호를 다시 요청해주세요.");
    }

    if (Date.now() > sessionAuth.expires) {
        delete req.session.auth; 
        return res.status(400).send("인증번호가 만료되었습니다. 다시 요청해주세요.");
    }

    if (sessionAuth.code === authCode) {
        req.session.isEmailVerified = true;
        delete req.session.auth; 
        return res.send("인증 성공");
    } else {
        return res.status(400).send("인증번호가 일치하지 않습니다.");
    }
});


// 5-4. 1단계 데이터 세션 저장 라우트
app.post('/saveStep1', (req, res) => {
    // ... server.js의 동일한 로직 ...
    const { name, birthdate, email } = req.body;
    
    if (!name || !birthdate || !email) {
        return res.status(400).send("필수 데이터(이름, 생년월일, 이메일)가 누락되었습니다.");
    }
    
    if (!req.session.isEmailVerified) {
        return res.status(403).send("이메일 인증이 완료되지 않았습니다.");
    }

    req.session.registerStep1 = { name, birthdate, email };
    delete req.session.isEmailVerified; 

    res.send("success: step1 data saved in session");
});


// 5. 정적 파일 라우팅 (Vercel에서는 이 부분이 필요 없을 수도 있지만, 안전하게 추가)
// Vercel은 'views' 폴더를 정적 에셋으로 제공할 수 있습니다.
app.get('/register_1.html', (req, res) => {
    res.sendFile(path.join(__dirname, '..', 'views', 'register_1.html'));
});

app.get('/register_2.html', (req, res) => {
    res.sendFile(path.join(__dirname, '..', 'views', 'register_2.html'));
});


// 6. Vercel 서버리스 함수로 내보내기
module.exports = app; 

// ⚠️ 참고: app.listen(...) 코드는 삭제해야 합니다.