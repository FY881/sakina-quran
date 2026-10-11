#!/bin/bash
set -x
SDK=${ANDROID_HOME:-$ANDROID_SDK_ROOT}
export PATH=$SDK/cmdline-tools/latest/bin:$SDK/emulator:$SDK/platform-tools:$PATH
ls -la /dev/kvm; nproc; free -g
(yes | sdkmanager --licenses) >/dev/null 2>&1
sdkmanager "emulator" "platform-tools" "system-images;android-34;google_apis;x86_64" > /tmp/sdk.log 2>&1; echo sdk $?
echo no | avdmanager create avd -n t -k "system-images;android-34;google_apis;x86_64" -d pixel_6 --force 2>&1 | tail -2
emulator -version | head -2
nohup emulator -avd t -no-window -gpu swiftshader_indirect -noaudio -no-boot-anim -no-snapshot -camera-back none -camera-front none -accel on -no-metrics > emu_run.log 2>&1 &
sleep 20; tail -20 emu_run.log
timeout 300 adb wait-for-device; echo wait $?
for i in $(seq 1 150); do [ "$(timeout 10 adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ] && break; sleep 4; done
echo booted=$(adb shell getprop sys.boot_completed)
tail -30 emu_run.log
adb shell settings put global window_animation_scale 0; adb shell settings put global transition_animation_scale 0; adb shell settings put global animator_duration_scale 0
adb shell wm size
timeout 1200 bash ci/shots.sh
exit 0
