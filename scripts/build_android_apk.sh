#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$ROOT_DIR"

echo "========================================="
echo "  Building Android APK for Tommi OS"
echo "========================================="

# Detect Java
if [ -z "$JAVA_HOME" ]; then
    if [ -d "/usr/lib/jvm/java-17-openjdk-amd64" ]; then
        export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
    elif [ -d "/usr/lib/jvm/default-java" ]; then
        export JAVA_HOME="/usr/lib/jvm/default-java"
    fi
fi

if [ -n "$JAVA_HOME" ]; then
    export PATH="$JAVA_HOME/bin:/opt/kotlinc/bin:$PATH"
fi

BUILD_DIR="/tmp/tommi_apk_build"
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/gen" "$BUILD_DIR/classes" "$BUILD_DIR/dex"
mkdir -p app/build/outputs/apk/debug

# 1. Build React Web Application if needed
if [ ! -d "dist" ] || [ ! -f "dist/index.html" ]; then
    echo "[1/7] Building Web Assets with Vite..."
    npm run build
else
    echo "[1/7] Web assets already built in dist/"
fi

mkdir -p app/src/main/assets/www
rm -rf app/src/main/assets/www/*
cp -r dist/* app/src/main/assets/www/

ANDROID_JAR="/opt/android-sdk/platforms/android-34/android.jar"
D8_JAR="/opt/android-sdk/build-tools/34.0.0/d8.jar"

# 2. Generate R.java using AAPT
echo "[2/7] Generating R.java with AAPT..."
aapt package -f -m \
    -J "$BUILD_DIR/gen" \
    -M app/src/main/AndroidManifest.xml \
    -S app/src/main/res \
    -I "$ANDROID_JAR"

# 3. Compile Kotlin Sources
echo "[3/7] Compiling Kotlin and Java sources..."
kotlinc -cp "$ANDROID_JAR:$BUILD_DIR/gen" \
    -d "$BUILD_DIR/classes" \
    app/src/main/java/com/tommi/os/MainActivity.kt \
    app/src/main/java/com/tommi/os/AndroidBridge.kt \
    app/src/main/java/com/tommi/os/TommiWebChromeClient.kt \
    app/src/main/java/com/tommi/os/TommiWebViewClient.kt \
    "$BUILD_DIR/gen/com/tommi/os/R.java"

javac -cp "$ANDROID_JAR" -d "$BUILD_DIR/classes" "$BUILD_DIR/gen/com/tommi/os/R.java"

# 4. Dex Classes with D8
echo "[4/7] Dexing classes with Android D8..."
CLASS_FILES=$(find "$BUILD_DIR/classes" -name "*.class")
java -cp "$D8_JAR" com.android.tools.r8.D8 \
    --min-api 24 \
    --lib "$ANDROID_JAR" \
    --output "$BUILD_DIR/dex" \
    $CLASS_FILES \
    /opt/android-libs/kotlin-stdlib.jar

# 5. Package APK Resources & Assets
echo "[5/7] Packaging resources into APK..."
aapt package -f \
    -M app/src/main/AndroidManifest.xml \
    -S app/src/main/res \
    -A app/src/main/assets \
    -I "$ANDROID_JAR" \
    -F "$BUILD_DIR/unaligned.apk"

# Add classes.dex
(cd "$BUILD_DIR/dex" && aapt add "$BUILD_DIR/unaligned.apk" classes.dex)

# 6. Zipalign 4-byte boundaries
echo "[6/7] Aligning APK (4-byte page alignment)..."
zipalign -v -p 4 "$BUILD_DIR/unaligned.apk" "$BUILD_DIR/aligned.apk"

# 7. Sign APK with Android Debug Keystore
echo "[7/7] Signing APK with apksigner (v1, v2, v3)..."
KEYSTORE_PATH="/tmp/debug.keystore"
if [ ! -f "$KEYSTORE_PATH" ]; then
    keytool -genkeypair -v -keystore "$KEYSTORE_PATH" -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
fi

FINAL_APK="app/build/outputs/apk/debug/app-debug.apk"
apksigner sign \
    --ks "$KEYSTORE_PATH" \
    --ks-pass pass:android \
    --ks-key-alias androiddebugkey \
    --key-pass pass:android \
    --v1-signing-enabled true \
    --v2-signing-enabled true \
    --v3-signing-enabled true \
    --out "$FINAL_APK" \
    "$BUILD_DIR/aligned.apk"

# Verify APK
echo "Verifying APK..."
apksigner verify --verbose "$FINAL_APK"

echo ""
echo "========================================="
echo "  APK Build Complete Successfully!"
echo "  Output: $FINAL_APK"
echo "  Size: $(du -h "$FINAL_APK" | cut -f1)"
echo "========================================="
