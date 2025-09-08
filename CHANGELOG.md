# 1.0.0

**BREAKING CHANGES:**
- **Java 21 Required**: Upgraded minimum Java version from 8 to 21
- **Constructor Changes**: HTTPClient and GRPCClient constructors now throw SDKException for input validation
- **Dependencies**: All major dependencies updated (see below)

**Major Improvements:**
- **HTTPClient**: Migrated from deprecated Apache HttpClient to Java 11+ HttpClient for better security and performance
- **GRPCClient**: Fixed critical timeout calculation bug (was 1000x too short) and added type-safe method calls
- **Resource Management**: Both clients now implement AutoCloseable for proper cleanup
- **Input Validation**: Added comprehensive null checks and parameter validation
- **Error Handling**: Improved exception handling with context preservation and detailed messages

**Dependency Updates:**
- gRPC: 1.29.0 → 1.58.0
- Protobuf: 3.6.1 → 3.24.4  
- Protobuf: 3.6.1 → 3.24.4  
- Google Auth Library: 0.7.0 → 1.19.0
- Netty TCNative: 2.0.3.Final → 2.0.62.Final
- BouncyCastle: 1.58 → 1.70
- Apache HttpClient: 4.3.6 → 4.5.14
- Nimbus JOSE JWT: 5.8 → 9.31

**Technical Improvements:**
- Enhanced .gitignore with comprehensive Java project patterns
- Updated Maven plugins to latest versions
- Extracted magic numbers to constants for better maintainability
- Improved default timeouts (10 minutes instead of 1 day)
- Maintained backwards compatibility for GRPCClient through method overloading
- Removed deprecated VAXCredentials.thisUsesUnstableApi() method
- Better code organization and import structure

**Migration Required:**
- Update to Java 21
- Wrap client constructors in try-catch blocks for SDKException
- Consider using try-with-resources for automatic resource cleanup

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
