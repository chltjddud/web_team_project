package controller;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// PW 찾기 결과 조회 서블릿
@WebServlet("/getFoundPw")
public class GetFoundPwServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {

		response.setContentType("text/plain;charset=UTF-8");
		PrintWriter out = response.getWriter();

		// 세션이 존재하는지 확인 (없으면 새로 생성하지 않음)
		HttpSession session = request.getSession(false);

		if (session == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			out.print("error:SESSION_NULL: 세션이 존재하지 않거나 만료되었습니다.");
			return;
		}

		// ⭐️ 세션에서 실제 DB 비밀번호 정보 가져오기 ⭐️
		String foundPassword = (String) session.getAttribute("foundPassword");
        // 세션에서 ID 정보 가져오기
        String foundLoginId = (String) session.getAttribute("foundLoginId");
        
		// 비밀번호 정보가 없으면 오류 처리
		if (foundPassword == null || foundPassword.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			out.print("error:PW_MISSING: 세션에 비밀번호 정보(foundPassword)가 없습니다. (PW 미저장/이미 사용)");
			return;
		}

		// ⭐️ 성공 시 세션에서 PW 및 ID 정보 제거 (일회성 사용은 유지) ⭐️
        // (정보가 노출된 후 다른 사람이 세션을 통해 재사용하는 것을 방지하기 위해 제거합니다.)
		session.removeAttribute("foundPassword");
        session.removeAttribute("foundLoginId");

        // 200 OK와 함께 [success:ID:DB_PASSWORD] 형태로 반환
        // 클라이언트(findPW2.html)는 이 값을 그대로 사용자에게 보여줍니다.
        out.print("success:" + foundLoginId + ":" + foundPassword);
    }
}