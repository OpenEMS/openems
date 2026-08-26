pod install
cd ui/ios/App

# Provisioning profiles & verify signing
xcodebuild -showBuildSettings | grep -i "provisioning"

# List schemes
xcodebuild -list 

# Building for release
xcodebuild -workspace App.xcworkspace -scheme -configuration release -sdk iphoneos -archivePath App.xcarchive -destination 'generic/platform=iOS'

# archive (upload) 
xcodebuild -scheme App -workspace App.xcworkspace -configuration Release archive -archivePath App/output/App.xcarchive

# Unlocking security keychain
security unlock-keychain -p <macMini-fems-pw> login.keychain

# Debugging upload
xcrun notarytool log <submission-id (included)>

# Check if ipa is signed
codesign -dv --verbose=4 *.ipa

# create new target
- update capacitor.config.ts
- update Podfile add new target