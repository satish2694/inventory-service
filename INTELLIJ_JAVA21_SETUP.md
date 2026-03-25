# IntelliJ IDEA - Java 21 JDK Configuration Guide

## Problem
Error: "Cannot find JRE 'homebrew-21'"

## Solution

### Step 1: Locate Java 21 Installation
```bash
/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```

### Step 2: Configure in IntelliJ IDEA

#### Method 1: Project Settings (Recommended)

1. **Open Project Settings**
   - Go to: `IntelliJ IDEA` → `Preferences` (or `Settings` on Linux/Windows)
   - Or use keyboard shortcut: `Cmd + ,` (Mac) or `Ctrl + Alt + S` (Linux/Windows)

2. **Navigate to Project Structure**
   - Search for "Project Structure" in the search box
   - Or go to: `Preferences` → `Project Structure` → `Project`

3. **Set Project SDK**
   - Click on the SDK dropdown
   - Select "Add SDK" → "Add JDK"
   - Navigate to: `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`
   - Click "Open"
   - Name it: `openjdk-21` or `Java 21`
   - Click "OK"

4. **Set as Project SDK**
   - Select the newly added Java 21 SDK
   - Click "Apply" and "OK"

#### Method 2: Module Settings

1. **Open Project Structure**
   - `Preferences` → `Project Structure` → `Modules`

2. **Select Your Module**
   - Select `inventory-service` module

3. **Set Module SDK**
   - In the "Module SDK" dropdown, select the Java 21 SDK you added
   - Click "Apply" and "OK"

#### Method 3: Maven Settings

1. **Open Preferences**
   - `Preferences` → `Build, Execution, Deployment` → `Build Tools` → `Maven`

2. **Set Maven JDK**
   - In "JDK for importer", select Java 21
   - Click "Apply" and "OK"

### Step 3: Configure Run Configuration

1. **Edit Run Configuration**
   - Click on the Run Configuration dropdown (top right)
   - Select "Edit Configurations..."

2. **Set JDK**
   - In the "JDK version" field, select Java 21
   - Click "Apply" and "OK"

### Step 4: Verify Configuration

1. **Check Project Settings**
   - `Preferences` → `Project Structure` → `Project`
   - Verify SDK is set to Java 21

2. **Check Maven Settings**
   - `Preferences` → `Build, Execution, Deployment` → `Build Tools` → `Maven`
   - Verify JDK for importer is Java 21

3. **Invalidate Caches**
   - `File` → `Invalidate Caches...`
   - Select "Invalidate and Restart"

## Alternative: Command Line Build

If IDE configuration is problematic, use command line:

```bash
# Build
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn clean install -DskipTests

# Run
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn spring-boot:run
```

## Troubleshooting

### Issue: Still showing "Cannot find JRE 'homebrew-21'"

**Solution:**
1. Delete the cached JDK reference
2. Go to `Preferences` → `Project Structure` → `SDKs`
3. Remove any broken "homebrew-21" entries
4. Add Java 21 again using the full path

### Issue: Maven still using wrong JDK

**Solution:**
1. Go to `Preferences` → `Build, Execution, Deployment` → `Build Tools` → `Maven` → `Runner`
2. Set "JVM" to Java 21
3. Invalidate caches and restart

### Issue: Build still fails

**Solution:**
1. Close IntelliJ IDEA
2. Run from command line:
   ```bash
   JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn clean install -DskipTests
   ```
3. Reopen IntelliJ IDEA
4. Invalidate caches: `File` → `Invalidate Caches...`

## Verify Java 21 is Configured

### Check in Terminal
```bash
# Verify Java 21 path
/opt/homebrew/opt/openjdk@21/bin/java -version

# Should output:
# openjdk version "21.0.10" 2026-01-20
# OpenJDK Runtime Environment Homebrew (build 21.0.10)
# OpenJDK 64-Bit Server VM Homebrew (build 21.0.10, mixed mode, sharing)
```

### Check in IntelliJ
1. Open Terminal in IntelliJ: `View` → `Tool Windows` → `Terminal`
2. Run:
   ```bash
   java -version
   ```
3. Should show Java 21

## Quick Reference

| Setting | Value |
|---------|-------|
| Project SDK | openjdk-21 |
| Module SDK | openjdk-21 |
| Maven JDK | Java 21 |
| JDK Path | `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home` |

## Summary

✅ Java 21 installed at `/opt/homebrew/opt/openjdk@21`  
✅ Configure in IntelliJ IDEA Project Structure  
✅ Set Maven to use Java 21  
✅ Invalidate caches and restart  
✅ Build should now work  

If issues persist, use command line build with `JAVA_HOME` environment variable.
