package com.vendasta.vax;

/**
 * Configuration options for individual API requests.
 * 
 * <p>This class allows customization of request behavior including
 * timeouts and authentication settings.
 */
public class RequestOptions {
    private Boolean includeToken = true;
    private Float timeout = 10000f;

    RequestOptions(Builder builder) {
        if (builder.includeToken != null) {
            this.includeToken = builder.includeToken;
        }
        if (builder.timeout != null) {
            this.timeout = builder.timeout;
        }
    }

    Boolean getIncludeToken() {
        return this.includeToken;
    }

    Float getTimeout() {
        return this.timeout;
    }


    /**
     * Builder for configuring RequestOptions.
     * 
     * <p>Provides a fluent interface for setting request-specific options
     * including timeout and authentication settings.
     */
    public static class Builder {
        private Boolean includeToken;
        private Float timeout;
        
        /**
         * Creates a new builder instance.
         */
        public Builder() {}

        Builder setTimeout(float timeout) {
            this.timeout = timeout;
            return this;
        }

        Builder setIncludeToken(boolean includeToken) {
            this.includeToken = includeToken;
            return this;
        }

        RequestOptions build() {
            return new RequestOptions(this);
        }

        void fromOptions(Builder options) {
            if (options.includeToken != null) {
                this.includeToken = options.includeToken;
            }
            if (options.timeout != null) {
                this.timeout = options.timeout;
            }
        }
    }
}