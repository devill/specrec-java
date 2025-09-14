# Publishing to Maven Central

## Prerequisites

1. **Central Portal Account**: Created at https://central.sonatype.com
2. **Namespace Verification**: `link.specrec` namespace verified (via specrec.link domain)
3. **GPG Key**: Set up for signing artifacts
4. **Maven Settings**: Configure ~/.m2/settings.xml with credentials (see settings-template.xml)

## GPG Setup

```bash
# Generate a key pair (if you don't have one)
gpg --gen-key

# List your keys
gpg --list-secret-keys --keyid-format=long

# Export public key to keyserver
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

## Maven Settings

1. **Generate User Token** at https://central.sonatype.com:
   - Log in to Central Portal
   - Go to Account > Generate User Token
   - Copy the username and password

2. **Configure credentials** by copying `settings-template.xml` to `~/.m2/settings.xml` and update:
   - Token username/password (from step 1)
   - GPG passphrase

## Publishing Process

### 1. Prepare Release

```bash
# Ensure all tests pass
mvn clean test

# Update version in pom.xml (remove -SNAPSHOT for release)
# Current: 0.0.1
```

### 2. Deploy to Central Portal

```bash
# Deploy to Central Portal
mvn clean deploy -P release

# This will:
# - Build the project
# - Generate source and javadoc JARs
# - Sign all artifacts with GPG
# - Upload to Central Portal for validation
```

### 3. Review and Publish

1. Log in to https://central.sonatype.com
2. Go to "Deployments" section
3. Find your deployment
4. Review the artifacts
5. Click "Publish" to release to Maven Central

### 4. Verify Release

After publishing (can take 30 minutes to 2 hours):
- Check https://central.sonatype.com/artifact/link.specrec/specrec
- Search on https://search.maven.org

## Version Management

After releasing:
1. Update pom.xml to next SNAPSHOT version (e.g., 0.0.2-SNAPSHOT)
2. Commit and push changes

## Troubleshooting

### GPG Signing Issues
- Ensure GPG key is available: `gpg --list-keys`
- For GPG passphrase prompts: `export GPG_TTY=$(tty)`

### Authentication Failures
- Verify user token in ~/.m2/settings.xml (not username/password)
- Generate fresh token from Central Portal Account page
- Check Central Portal login works
- Ensure server ID is "central", not "ossrh"

### Validation Errors
- Missing javadocs: Check all public methods have documentation
- Missing sources: Ensure maven-source-plugin is configured
- POM issues: Verify all required metadata is present

## Automated Releases (Future)

Consider setting up GitHub Actions for automated releases:
- Use secrets for credentials
- Trigger on version tags
- Automate the entire deploy process