package com.vendasta.vax;

public class EnvironmentConfig {
    private final String url;
    private final String scope;
    private final String host;
    private final boolean secure;

    public EnvironmentConfig(String host, String scope, String url, boolean secure) {
        this.host = host;
        this.scope = scope;
        this.url = url;
        this.secure = secure;
    }

    public boolean isSecure() {
        return secure;
    }

    public String getUrl() {
        return url;
    }

    public String getScope() {
        return scope;
    }

    public String getHost() {
        return host;
    }
}