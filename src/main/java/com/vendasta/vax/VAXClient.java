package com.vendasta.vax;


abstract class VAXClient {
    // Default timeout is a number in milliseconds
    private float defaultTimeout = 10000;

    VAXClient() {}

    VAXClient(float defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    RequestOptions buildVAXOptions(RequestOptions.Builder options) {
        // setting defaults
        RequestOptions.Builder optsBuilder = new RequestOptions
                .Builder()
                .setTimeout(this.defaultTimeout);

        if (options != null) {
            optsBuilder.fromOptions(options);
        }

        return optsBuilder.build();
    }
}
