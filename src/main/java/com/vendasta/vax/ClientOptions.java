package com.vendasta.vax;

public class ClientOptions {
    private Boolean useHttp = false;

    ClientOptions(Builder builder) {
        if (builder.useHttp != null) {
            this.useHttp = builder.useHttp;
        }
    }

    Boolean getUseHttp() {
        return this.useHttp;
    }

    public static class Builder {
        private Boolean useHttp = false;

        /**
         * Force the client to use http instead of gRPC
         * @return Builder
         */
        Builder useHttp() {
            this.useHttp = true;
            return this;
        }

        ClientOptions build() {
            return new ClientOptions(this);
        }
    }
}
