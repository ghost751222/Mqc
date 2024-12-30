package com.mqc.utils;

import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class HttpClientUtils {

    public static HttpClient getHttpClient() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

        SSLContextBuilder sslBuilder = new SSLContextBuilder();
        sslBuilder.loadTrustMaterial(null, (TrustStrategy) (x509Certificates, s) -> true);
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslBuilder.build(), (s, sslSession) -> true);
        return HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).build();

        //return HttpClientBuilder.create().setSslcontext(new SSLContextBuilder().loadTrustMaterial(null, (TrustStrategy) (x509Certificates, s) -> true).build()).build();
    }

    public static HttpGet getHttpGetMethod(String uri) {
        return new HttpGet(uri);
    }

    public static HttpPost getHttpPostMethod(String uri) {
        return new HttpPost(uri);
    }

    public static void addHeaderContentJson(AbstractHttpMessage httpMessage) {
        httpMessage.addHeader(new BasicHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()));
    }

    public static class HttpGet extends HttpEntityEnclosingRequestBase {

        public static final String METHOD_NAME = "GET";

        public HttpGet(String uri) {
            setURI(URI.create(uri));
            addHeader(new BasicHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()));
        }

        @Override
        public String getMethod() {
            return METHOD_NAME;
        }

    }
}
