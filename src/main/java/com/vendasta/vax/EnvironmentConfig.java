package com.vendasta.vax;

/**
 * Configuration for VAX environment settings.
 * 
 * <p>This class encapsulates the connection details for a specific
 * VAX environment including host, URL, and security settings.
 */
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

    /**
     * Returns whether this environment uses secure connections.
     * 
     * @return true if HTTPS/secure gRPC should be used
     */
    public boolean isSecure() {
        return secure;
    }

    /**
     * Returns the base URL for this environment.
     * 
     * @return the base URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the hostname for this environment.
     * 
     * @return the hostname
     */
    public String getHost() {
        return host;
    }

    /**
     * Builder for configuring EnvironmentConfig instances.
     */
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

    /**
     * Creates a new builder for EnvironmentConfig.
     * 
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}