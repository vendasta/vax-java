# VAX Java SDK

[![Java Version](https://img.shields.io/badge/Java-21+-blue.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Maven Central](https://img.shields.io/badge/Maven%20Central-1.1.0-green.svg)](https://mvnrepository.com/artifact/com.vendasta/vax.v1)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

The **VAX (Vendasta API Extensions) Java SDK** provides a modern, type-safe client library for interacting with Vendasta's APIs using both HTTP and gRPC protocols. Built with Java 21, it offers a clean builder pattern API, comprehensive credential management, and excellent performance.

## 🚀 Features

- **🔧 Builder Pattern API**: Fluent, readable client configuration
- **🔒 Flexible Authentication**: Support for environment variables, credential objects, or input streams
- **⚡ Modern HTTP Stack**: Uses Java 11+ `java.net.http.HttpClient` for optimal performance
- **🛡️ Type Safety**: Compile-time safety with method references instead of reflection
- **♻️ Resource Management**: AutoCloseable clients for proper cleanup
- **🎯 Smart Defaults**: Sensible default values (HTTPS enabled, 10-second timeouts)
- **📦 Minimal Dependencies**: Lean dependency footprint with no unused libraries

## 📋 Requirements

- **Java 21** or later
- **Maven 3.6+** or **Gradle 7.0+**

## 📦 Installation

### Maven

```xml
<dependency>
    <groupId>com.vendasta</groupId>
    <artifactId>vax.v1</artifactId>
    <version>1.1.0</version>
</dependency>
```

### Gradle

```gradle
implementation 'com.vendasta:vax.v1:1.1.0'
```

## 🏁 Quick Start

### Environment Setup

Set your service account credentials:

```bash
export VENDASTA_APPLICATION_CREDENTIALS=/path/to/your/service-account.json
```

### HTTP Client Example

```java
import com.vendasta.vax.HTTPClient;
import com.vendasta.vax.RequestOptions;

// Simple HTTP client with defaults
HTTPClient client = HTTPClient.builder()
    .host("api.vendasta.com")
    .build();

// Make a request
MyResponse response = client.doRequest(
    "/api/v1/resource",
    myRequest,
    MyResponse.newBuilder(),
    RequestOptions.newBuilder()
);

// Don't forget to close!
client.close();
```

### gRPC Client Example

```java
import com.vendasta.vax.GRPCClient;
import com.vendasta.vax.RequestOptions;

// Custom gRPC client implementation
public class MyServiceClient extends GRPCClient<MyServiceGrpc.MyServiceBlockingStub> {
    
    public MyServiceClient(String host) throws SDKException {
        super(GRPCClient.builder().host(host));
    }
    
    @Override
    protected MyServiceGrpc.MyServiceBlockingStub newBlockingStub(ManagedChannel channel) {
        return MyServiceGrpc.newBlockingStub(channel);
    }
    
    public MyResponse callService(MyRequest request) throws SDKException {
        return doRequest(
            stub -> stub.myMethod(request),
            RequestOptions.newBuilder()
        );
    }
}

// Usage
try (MyServiceClient client = new MyServiceClient("grpc.vendasta.com")) {
    MyResponse response = client.callService(myRequest);
    // Process response
}
```

## 🛠️ Configuration Options

### Builder Configuration

Both `HTTPClient` and `GRPCClient` support the same builder pattern:

```java
HTTPClient client = HTTPClient.builder()
    .host("api.example.com")           // Required: API hostname
    .secure(true)                      // Optional: HTTPS/secure gRPC (default: true)
    .defaultTimeout(15000)             // Optional: timeout in milliseconds (default: 10000)
    .credentials(myCredentials)        // Optional: custom credentials
    .serviceAccount(inputStream)       // Optional: service account stream
    .build();
```

### Credential Management

#### 1. Environment Variable (Default)
```java
// Uses VENDASTA_APPLICATION_CREDENTIALS environment variable
HTTPClient client = HTTPClient.builder()
    .host("api.example.com")
    .build();
```

#### 2. Credentials Object
```java
VAXCredentials.Credentials creds = new VAXCredentials.Credentials();
creds.setPrivateKeyID("your-key-id");
creds.setPrivateKey("-----BEGIN PRIVATE KEY-----\n...");
creds.setEmail("service@example.com");
creds.setTokenURI("https://oauth2.googleapis.com/token");

HTTPClient client = HTTPClient.builder()
    .host("api.example.com")
    .credentials(creds)
    .build();
```

#### 3. Service Account Stream
```java
InputStream serviceAccount = new FileInputStream("service-account.json");

HTTPClient client = HTTPClient.builder()
    .host("api.example.com")
    .serviceAccount(serviceAccount)
    .build();
```

#### 4. Calling a Service as Yourself (Local Testing)
To test an SDK against demo from your laptop as yourself, build the client without credentials and use one of these.

Your mscli key, which `mscli auth login -e demo` writes and which refreshes its own token:

```bash
export VENDASTA_APPLICATION_CREDENTIALS=~/.config/vendasta/demo_default_credentials.json
```

Or your Vendasta bearer token, sent as `Authorization: Bearer <token>` (a value copied with its `Bearer ` prefix works too). It is not refreshed, so get a new one when calls start failing with `UNAUTHENTICATED`:

```bash
export VENDASTA_ACCESS_TOKEN=<your token>
```

`VENDASTA_ACCESS_TOKEN` takes precedence over `VENDASTA_APPLICATION_CREDENTIALS` (vax logs a warning when both are set), and explicit `credentials(...)` or `serviceAccount(...)` take precedence over both.

### Request Options

Configure individual requests:

```java
RequestOptions options = RequestOptions.newBuilder()
    .setIncludeToken(true)             // Include auth token (default: true)
    .setTimeout(30000)                 // Request timeout in ms
    .build();

MyResponse response = client.doRequest("/path", request, responseBuilder, options);
```

## 🔧 Advanced Usage

### Resource Management

Both clients implement `AutoCloseable` for proper resource cleanup:

```java
// Try-with-resources (recommended)
try (HTTPClient client = HTTPClient.builder().host("api.example.com").build()) {
    // Use client
} // Automatically closed

// Manual cleanup
HTTPClient client = HTTPClient.builder().host("api.example.com").build();
try {
    // Use client
} finally {
    client.close();
}
```

### Error Handling

```java
try {
    HTTPClient client = HTTPClient.builder()
        .host("api.example.com")
        .build();
        
    MyResponse response = client.doRequest(path, request, responseBuilder, options);
    
} catch (SDKException e) {
    // getStatus() is the gRPC status carried by the failure, with UNAVAILABLE as the fallback when there is none.
    // A rejected key is UNAUTHENTICATED, and an unreachable host times out as DEADLINE_EXCEEDED.
    if (e.getStatus().getCode() == io.grpc.Status.Code.PERMISSION_DENIED) {
        // ...
    }
    logger.error("SDK Error: {}", e.getMessage(), e);
} catch (Exception e) {
    // Handle other errors
    logger.error("Unexpected error: {}", e.getMessage(), e);
}
```

### Custom gRPC Client

```java
public class AccountServiceClient extends GRPCClient<AccountServiceGrpc.AccountServiceBlockingStub> {
    
    public AccountServiceClient(Builder builder) throws SDKException {
        super(builder);
    }
    
    public static AccountServiceClient create(String host) throws SDKException {
        return new AccountServiceClient(
            GRPCClient.builder().host(host)
        );
    }
    
    @Override
    protected AccountServiceGrpc.AccountServiceBlockingStub newBlockingStub(ManagedChannel channel) {
        return AccountServiceGrpc.newBlockingStub(channel);
    }
    
    public Account getAccount(String accountId) throws SDKException {
        GetAccountRequest request = GetAccountRequest.newBuilder()
            .setAccountId(accountId)
            .build();
            
        return doRequest(
            stub -> stub.getAccount(request),
            RequestOptions.newBuilder()
        );
    }
}
```

## 🔄 Migration from 0.x

The 1.0.0 release introduces breaking changes. Here's how to migrate:

### Constructor to Builder Pattern

**Before (0.x):**
```java
HTTPClient client = new HTTPClient(host, scope, secure, timeout);
GRPCClient client = new GRPCClient(host, scope, secure, timeout);
```

**After (1.0.0):**
```java
HTTPClient client = HTTPClient.builder()
    .host(host)
    .secure(secure)
    .defaultTimeout(timeout)
    .build();

GRPCClient client = GRPCClient.builder()
    .host(host)
    .secure(secure)
    .defaultTimeout(timeout)
    .build();
```

### Scope Parameter Removal

The `scope` parameter has been completely removed as it was unused:

**Before:**
```java
// scope parameter was required but ignored
new HTTPClient(host, "unused-scope", secure);
```

**After:**
```java
// No scope parameter needed
HTTPClient.builder().host(host).secure(secure).build();
```

### Java Version

Update your Java version and build configuration:

```xml
<!-- Maven -->
<properties>
    <maven.compiler.release>21</maven.compiler.release>
</properties>
```

```gradle
// Gradle
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
```

## 📊 Performance Tips

1. **Reuse Clients**: Create clients once and reuse them for multiple requests
2. **Resource Cleanup**: Always close clients when done (use try-with-resources)
3. **Timeout Configuration**: Set appropriate timeouts for your use case
4. **Connection Pooling**: HTTP clients automatically pool connections

## 🐛 Troubleshooting

### Common Issues

**Authentication Errors:**
```
CredentialsException: Could not refresh token
```
- Verify `VENDASTA_APPLICATION_CREDENTIALS` environment variable
- Ensure service account file exists and is readable
- Check service account permissions

**Timeout Errors:**
```
SDKException: Request timeout
```
- Increase timeout with `.defaultTimeout()` or `RequestOptions.setTimeout()`
- Check network connectivity
- Verify server responsiveness

**Build Errors:**
```
UnsupportedClassVersionError
```
- Ensure you're using Java 21 or later
- Update your IDE and build tools

## 📚 API Reference

### HTTPClient Builder Methods

| Method | Description | Default |
|--------|-------------|---------|
| `host(String)` | API hostname (required) | - |
| `secure(boolean)` | Use HTTPS | `true` |
| `defaultTimeout(float)` | Timeout in milliseconds | `10000` |
| `credentials(Credentials)` | Custom credentials | Environment variable |
| `serviceAccount(InputStream)` | Service account stream | Environment variable |

### GRPCClient Builder Methods

| Method | Description | Default |
|--------|-------------|---------|
| `host(String)` | gRPC server hostname (required) | - |
| `secure(boolean)` | Use secure gRPC (TLS) | `true` |
| `defaultTimeout(float)` | Timeout in milliseconds | `10000` |
| `credentials(Credentials)` | Custom credentials | Environment variable |
| `serviceAccount(InputStream)` | Service account stream | Environment variable |

### RequestOptions Builder Methods

| Method | Description | Default |
|--------|-------------|---------|
| `setIncludeToken(boolean)` | Include auth token | `true` |
| `setTimeout(float)` | Request timeout in ms | Client default |

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Documentation**: [API Docs](https://docs.vendasta.com)
- **Issues**: [GitHub Issues](https://github.com/vendasta/vax-java/issues)
- **Email**: [support@vendasta.com](mailto:support@vendasta.com)

## 📈 Changelog

See [CHANGELOG.md](CHANGELOG.md) for detailed release notes and migration guides.
