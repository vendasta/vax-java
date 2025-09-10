package com.vendasta.vax;

public class EnvironmentConfig {
    private final String url;
    private final String host;
    private final boolean secure;

    // Private constructor used by Builder
    private EnvironmentConfig(Builder builder) {
        this.host = builder.host;
        this.url = builder.url;
        this.secure = builder.secure;
    }

    public boolean isSecure() {
        return secure;
    }

    public String getUrl() {
        return url;
    }

    public String getHost() {
        return host;
    }

    public static class Builder {
        private String host;
        private String url;
        private boolean secure = true; // Default to secure

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder secure(boolean secure) {
            this.secure = secure;
            return this;
        }

        public EnvironmentConfig build() {
            if (host == null || host.trim().isEmpty()) {
                throw new IllegalArgumentException("Host cannot be null or empty");
            }
            if (url == null || url.trim().isEmpty()) {
                throw new IllegalArgumentException("URL cannot be null or empty");
            }
            return new EnvironmentConfig(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}