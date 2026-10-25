ORBIT v4 by Malel - Skyworth 55 inch Google TV (Android 11)
1. Unzip, upload the CONTENTS to your GitHub repository (overwrite existing files).
2. Check .github/workflows/build.yml exists (copy in build-workflow-copy.txt).
3. Actions tab: wait for the green tick. Releases: download OrbitTV.apk (about 70-90 KB).
4. TV: Downloader > the release link > Install. Uninstall the old Orbit TV first if needed.
Optional home screen: adb shell cmd package set-home-activity com.orbit.tv/.MainActivity
Undo: adb uninstall com.orbit.tv
