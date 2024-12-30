package com.mqc.config;

import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;


public class AllExceptUrlsStartedWith implements RequestMatcher {
    private static final String[] ALLOWED_METHODS =
            new String[]{"GET", "HEAD", "TRACE", "OPTIONS", "POST"};

    private final String[] allowedUrls;

    public AllExceptUrlsStartedWith(String... allowedUrls) {
        this.allowedUrls = allowedUrls;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        // replicate default behavior (see CsrfFilter.DefaultRequiresCsrfMatcher class)
        String method = request.getMethod();
        for (String allowedMethod : ALLOWED_METHODS) {
            if (allowedMethod.equals(method)) {
                return false;
            }
        }

        // apply our own exceptions
        String uri = request.getRequestURI();
        for (String allowedUrl : allowedUrls) {
            if (uri.startsWith(allowedUrl)) {
                return false;
            }
        }

        return true;
    }
}
