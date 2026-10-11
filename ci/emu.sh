#!/bin/bash
set -x
SDK=$ANDROID_HOME; [ -z "$SDK" ] && SDK=$ANDROID_SDK_ROOT
export PATH=$SDK/cmdline-tools/latest/bin:$SDK/emulator:$SDK/platform-tools:$PATH
ls -la /dev/kvm
yes | sdkmanager --licenses >/dev/null
sdkmanager "emulator" "platform-tools" "system-images;android-34;google_apis;x86_64" > /tmp/sdk.log 2>&1; tail -3 /tmp/sdk.log
echo no | avdmanager create avd -n t -k "system-images;android-34;google_apis;x86_64" -d pixel_6 --force
emulator -avd t -no-window -gpu swiftshader_indirect -noaudio -no-boot-anim -no-snapshot -camera-back none -memory 4096 > /tmp/emu_run.log 2>&1 &
adb wait-for-device
for i in $(seq 1 120); do [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1 ] && break; sleep 3; done
adb shell getprop sys.boot_completed
adb shell settings put global window_animation_scale 0; adb shell settings put global transition_animation_scale 0; adb shell settings put global animator_duration_scale 0
adb shell wm size
bash ci/shots.sh
tail -50 /tmp/emu_run.log
exit 0
