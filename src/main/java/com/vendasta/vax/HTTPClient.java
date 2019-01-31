package com.vendasta.vax;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public abstract class HTTPClient extends VAXClient {
    private String host;
    private boolean secure;
    private VAXCredentials credentialsManager;
    private HttpClient httpClient;
    private Gson gson = new Gson();

    public HTTPClient(String host, String scope, boolean secure) {
        this(host, scope, secure, 10000);
    }

    public HTTPClient(String host, String scope, boolean secure, float defaultTimeout) {
        super(defaultTimeout);
        this.host = host;
        this.secure = secure;
        this.credentialsManager = new VAXCredentials(scope);
        this.httpClient = HttpClientBuilder.create().build();
    }

    public HTTPClient(String host, String scope, InputStream serviceAccount, boolean secure, float defaultTimeout) {
        super(defaultTimeout);
        this.host = host;
        this.secure = secure;
        this.credentialsManager = new VAXCredentials(scope, serviceAccount);
        this.httpClient = HttpClientBuilder.create().build();
    }

    private String buildUrl(String path) {
        String scheme = (secure) ? "https" : "http";
        return String.format("%s://%s/%s", scheme, host, path);
    }

    private String toJson(com.google.protobuf.AbstractMessage msg) throws SDKException {
        try {
            return JsonFormat.printer().print(msg);
        } catch (InvalidProtocolBufferException e) {
            throw new SDKException(e.getMessage());
        }
    }

    class GRPCError {
        @SerializedName("code")
        private int code;
        @SerializedName("message")
        private String message;
    }

    protected <V extends AbstractMessage.Builder> V doRequest(String path, com.google.protobuf.AbstractMessage req, V responseType, RequestOptions.Builder builder) throws SDKException {
        HttpPost request = new HttpPost(buildUrl(path));
        RequestOptions options = this.buildVAXOptions(builder);

        int timeout = Math.round(options.getTimeout());
        RequestConfig httpRequestOptions = RequestConfig.custom()
                .setConnectionRequestTimeout(timeout)
                .setSocketTimeout(timeout)
                .setConnectTimeout(timeout)
                .build();

        request.setConfig(httpRequestOptions);
        if (options.getIncludeToken()) {
            request.addHeader("Authorization", credentialsManager.getAuthorizationToken());
        }

        StringEntity params;
        try {
            params = new StringEntity(toJson(req));
        } catch (UnsupportedEncodingException e) {
            throw new SDKException(e.getMessage());
        }

        request.addHeader("content-type", "application/json");
        request.setEntity(params);
        String responseAsString;
        HttpResponse response;
        try {
            response = httpClient.execute(request);
            responseAsString = EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            throw new SDKException(e.getMessage());
        }

        if (response.getStatusLine().getStatusCode() == 200) {
            try {
                JsonFormat.parser().merge(responseAsString, responseType);
                return responseType;
            } catch (InvalidProtocolBufferException e) {
                throw new SDKException(e.getMessage());
            }
        } else {
            GRPCError err = gson.fromJson(responseAsString, GRPCError.class);
            throw new SDKException(err.message);
        }
    }
}
