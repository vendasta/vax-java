# 0.3.0
- Updated Java compiler target to Java 21
- Updated all dependencies to latest versions:
  - gRPC from 1.29.0 to 1.58.0
  - Protobuf from 3.6.1 to 3.24.4
  - Google Auth Library from 0.7.0 to 1.19.0
  - Netty TCNative from 2.0.3.Final to 2.0.62.Final
  - BouncyCastle from 1.58 to 1.70
  - Apache HttpClient from 4.3.6 to 4.5.14
  - Nimbus JOSE JWT from 5.8 to 9.31
- Updated Maven plugins to latest versions
- Replaced reflection-based gRPC calls with type-safe method references in GRPCClient
- Removed deprecated thisUsesUnstableApi() method from VAXCredentials
- Reorganized imports for better code organization

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
