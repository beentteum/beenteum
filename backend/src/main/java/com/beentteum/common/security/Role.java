package com.beentteum.common.security;

/**
 * 서비스 권한. JWT 의 role claim 과 Spring Security 권한(ROLE_xxx)에 사용한다.
 * TODO(DB/ERD): 사용자 테이블의 권한 컬럼 값이 확정되면 이 enum 과 값을 맞춘다.
 */
public enum Role {
    USER,
    ADMIN;

    /** Spring Security hasRole("USER") 검사에 맞는 권한 문자열 */
    public String authority() {
        return "ROLE_" + name();
    }
}
