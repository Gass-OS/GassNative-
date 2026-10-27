#!/data/data/com.termux/files/usr/bin/bash
set -e
cd "$(dirname "$0")"
SDK=${ANDROID_HOME:-$HOME/android-sdk}
AJ=$(ls -d "$SDK"/platforms/android-*/android.jar | tail -1)
KS=$HOME/.gass.keystore

rm -rf build && mkdir -p build/classes
aapt2 compile --dir res -o build/resources.zip
aapt2 link --auto-add-overlay -o build/base.apk -I "$AJ" \
  --manifest AndroidManifest.xml -R build/resources.zip
find src -name "*.java" -print0 | xargs -0 javac -source 8 -target 8 -cp "$AJ" -d build/classes
d8 --lib "$AJ" --min-api 24 --output build $(find build/classes -name '*.class')
cp build/base.apk build/app.apk
(cd build && zip -j app.apk classes.dex)
mkdir -p build/lib && cp -r jniLibs/arm64-v8a build/lib/ && (cd build && zip -r app.apk lib)
zip -r build/app.apk assets/tar-tool
zip -r build/app.apk assets/gassnative-brandmark.png
zip -r build/app.apk assets/audio
zip -r build/app.apk assets/ui
zip -r build/app.apk assets/gassnative-square.png
zip -j build/app.apk assets/1061942.png
zip -j build/app.apk assets/GassNative-PUMPKIN-R8.png
zip -r build/app.apk assets/ai-knowledge
zip -r build/app.apk assets/hangover
zipalign -f 4 build/app.apk build/aligned.apk
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -storepass gassnative \
  -keypass gassnative -alias gass -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=Gass"
apksigner sign --ks "$KS" --ks-pass pass:gassnative --out gassnative.apk build/aligned.apk
echo "OK: $(pwd)/gassnative.apk"

cp -f "$PWD/gassnative.apk" /sdcard/Download/gassnative.apk 2>/dev/null && echo "Copied: /sdcard/Download/gassnative.apk" || echo "Run termux-setup-storage once to enable the copy"
