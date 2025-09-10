# 1.0.0

**BREAKING CHANGES:**
- **Java 21 Required**: Upgraded minimum Java version from 8 to 21
- **Builder Pattern**: Replaced all constructor overloads with fluent builder pattern for HTTPClient and GRPCClient
- **Scope Parameter Removed**: Eliminated unused scope parameter from all client constructors
- **HTTP Stack**: Completely replaced Apache HttpClient with Java 11+ `java.net.http.HttpClient`

**Major API Changes:**
```java
// OLD - Multiple constructor overloads (removed)
HTTPClient client = new HTTPClient(host, scope, secure, timeout);
GRPCClient client = new GRPCClient(host, scope, secure, timeout);

// NEW - Builder pattern (required)
HTTPClient client = HTTPClient.builder()
    .host(host)
    .secure(secure)
    .defaultTimeout(timeout)
    .build();

GRPCClient client = GRPCClient.builder()
    .host(host)
    .credentials(myCredentials)
    .secure(true)
    .build();
```

**HTTP Stack Modernization:**
- **HTTPClient**: Migrated from Apache HttpClient to Java 11+ `java.net.http.HttpClient`
- **VAXCredentials**: Replaced Apache HttpClient with modern Java HTTP client for token refresh
- **Unified Stack**: Consistent `java.net.http` usage across entire codebase
- **Better Error Handling**: Improved HTTP status code checking and timeout management

**Enhanced Credential Management:**
- **Public Credentials Class**: Made `VAXCredentials.Credentials` public static with getters/setters
- **Flexible Sources**: Support for environment variables, Credentials objects, or InputStreams
- **Builder Integration**: Fluent credential configuration in client builders

**Dependencies Removed:**
- `google-auth-library-oauth2-http` (never used)
- `httpclient` (Apache HttpClient - replaced with java.net.http)
- `annotations-api` (Apache Tomcat - never used)

**Dependencies Updated:**
- gRPC: 1.29.0 → 1.58.0
- Protobuf: 3.6.1 → 3.24.4
- BouncyCastle: 1.58 → 1.70
- Nimbus JOSE JWT: 5.8 → 9.31
- Netty TCNative: 2.0.3.Final → 2.0.62.Final

**Code Quality Improvements:**
- **Type Safety**: Replaced reflection-based gRPC calls with type-safe method references
- **Resource Management**: Both clients implement AutoCloseable for proper cleanup
- **Input Validation**: Comprehensive null checks and parameter validation
- **Default Values**: Sensible defaults (secure=true, defaultTimeout=10000ms)
- **Timeout Fixes**: Fixed critical timeout calculation bug in GRPCClient (was 1000x too short)

**Development Environment:**
- **Git Ignore**: Added comprehensive .gitignore with standard Java SDK patterns
- **Java Version**: Added .java-version file for development consistency
- **Maven Plugins**: Updated all Maven plugins to latest versions
- **Project Configuration**: Added UTF-8 encoding and proper compiler release settings

**Migration Guide:**
- **Java 21**: Update minimum Java version requirement
- **Constructor Migration**: Replace all constructor calls with builder pattern
- **Scope Removal**: Remove scope parameter (was unused)
- **Resource Cleanup**: Consider using try-with-resources for automatic cleanup

# 0.2.3
- HTTP client ignores unknown field when parsing from json to protobuf

# 0.2.1
- Make new instance from blocking stub

# 0.2.0
- Allow a input stream to be passed to the vax credentials constructor

# 0.1.3
- Update httpcomponents depedency

# 0.1.1
- Move creds to use cloud storage
- Fix Credentials Manager


# 0.1.0
- Initial build
