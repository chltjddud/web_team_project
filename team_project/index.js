// index.js

// 환경 변수 로드 (dotenv 설치 필수)
require('dotenv').config(); 

const { createClient } = require('@supabase/supabase-js');

// 1. 환경 변수 로드
const supabaseUrl = process.env.SUPABASE_URL;
const supabaseAnonKey = process.env.SUPABASE_ANON_KEY;

// 2. Supabase 클라이언트 초기화
const supabase = createClient(supabaseUrl, supabaseAnonKey);

/**
 * Supabase의 'users' 테이블을 사용하여 연결 및 쿼리를 테스트합니다.
 */
async function testSupabaseConnection() {
    console.log(`Connecting to Supabase URL: ${supabaseUrl}`);

    try {
        // 'users' 테이블에서 loginID, name, email 컬럼을 1개 가져오는 쿼리를 실행합니다.
        const { data, error } = await supabase
            .from('users') 
            .select('loginid, name, email') // ⭐️ users 테이블의 컬럼 이름으로 수정
            .limit(1);

        if (error) {
            // 네트워크 연결은 되었으나, 인증 또는 쿼리 권한 오류가 발생한 경우
            console.error('❌ Supabase 쿼리 오류 발생:', error.message);
            // 에러 코드와 상태를 확인해 보세요 (예: RLS 정책 위반 등)
            return;
        }

        if (data && data.length > 0) {
            console.log('✅ Supabase 연결 성공! 데이터베이스에 접근할 수 있으며 데이터를 가져왔습니다.');
            console.log('--- 첫 번째 users 레코드 예시 ---');
            console.log(`로그인 ID: ${data[0].loginID}`);
            console.log(`이름: ${data[0].name}`);
            console.log(`이메일: ${data[0].email}`);
        } else {
            console.log("✅ Supabase 연결 성공! 하지만 'users' 테이블에 데이터가 없습니다. (빈 배열)");
        }

    } catch (e) {
        // DNS, 방화벽 등 네트워크 문제가 발생한 경우
        console.error('❌ 네트워크 연결 실패 (UnknownHost나 Timeout 가능성):', e.message);
        console.error('--- 해결 방법: DNS 설정 또는 로컬/Supabase 방화벽을 확인하세요. ---');
    }
}

testSupabaseConnection();