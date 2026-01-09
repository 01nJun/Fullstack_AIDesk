package com.desk.security.filter;

import com.desk.service.FaceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

public class FaceAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private final FaceService faceService;
    private final StandardServletMultipartResolver multipartResolver;

    public FaceAuthenticationFilter(String defaultFilterProcessesUrl, FaceService faceService) {
        super(defaultFilterProcessesUrl);
        this.faceService = faceService;
        this.multipartResolver = new StandardServletMultipartResolver();
    }

    @Override
    @SuppressWarnings("null")
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        // 1. 멀티파트 요청인지 확인
        if (!multipartResolver.isMultipart(request)) {
            throw new BadCredentialsException("인증 형식이 올바르지 않습니다. (Multipart 아님)");
        }

        try {
            // 2. 에러가 발생했던 강제 형변환 대신, 리졸버를 통해 멀티파트 요청으로 변환
            MultipartHttpServletRequest multipartRequest = multipartResolver.resolveMultipart(request);

            // 3. 파일 추출 (프론트에서 보낸 이름 "faceFile" 확인)
            var file = multipartRequest.getFile("faceFile");

            if (file == null || file.isEmpty()) {
                throw new BadCredentialsException("전송된 얼굴 이미지가 없습니다.");
            }

            // 4. FaceService를 통해 사용자 식별
            String identifiedEmail = faceService.findFace(file);

            if (identifiedEmail == null) {
                throw new BadCredentialsException("FACE_NOT_RECOGNIZED");
            }

            // 5. 토큰 생성 및 인증 매니저에게 전달
            // 성공 시 발급되는 JWT에 인증수단(amr)을 표기할 수 있도록 요청에 힌트 부착
            request.setAttribute("amr", "face");
            FaceAuthenticationToken authRequest = new FaceAuthenticationToken(identifiedEmail);
            return this.getAuthenticationManager().authenticate(authRequest);

        } catch (Exception e) {
            // 상세 에러 로그 출력
            logger.error("얼굴 인식 인증 중 오류 발생", e);
            throw new BadCredentialsException(e.getMessage());
        }
    }
}