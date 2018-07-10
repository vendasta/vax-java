package com.vendasta.vax;

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


    public static class Builder {
        private Boolean includeToken;
        private Float timeout;

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