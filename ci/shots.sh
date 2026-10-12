#!/bin/bash
# D1a screenshots: theme CLASSIC_TEAL (dark), AR + EN, Home + Prayers (+ scrolled), via debug intent extras.
set -x
APK=$(ls app-src/app/build/outputs/apk/debug/*.apk | head -1)
PKG=$(aapt2 dump packagename "$APK" 2>/dev/null || echo app.sakina.quran.debug)
echo PKG=$PKG
mkdir -p shots
adb shell settings put global hide_error_dialogs 1 || true
adb install -r -g "$APK"
adb shell appops set $PKG SCHEDULE_EXACT_ALARM allow || true
adb shell settings put system time_12_24 12 || true
dismiss() {
  timeout 20 adb shell uiautomator dump /sdcard/d.xml >/dev/null 2>&1; adb pull /sdcard/d.xml /tmp/d.xml >/dev/null 2>&1
  if grep -q "isn.t responding\|لا يستجيب" /tmp/d.xml; then
    python3 - <<'PY'
import re,subprocess
x=open('/tmp/d.xml',encoding='utf-8').read()
for t in ('Wait','انتظار','Close app'):
  m=re.search(r'<node [^>]*?text="%s"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'%t,x)
  if m:
    a,b,c,d=map(int,m.groups()); subprocess.call(['adb','shell','input','tap',str((a+c)//2),str((b+d)//2)]); break
PY
    sleep 2
  fi
}
shot() { sleep 3; dismiss; adb exec-out screencap -p > "shots/$1.png"; }
launch() { # lang route mode
  adb shell am force-stop $PKG
  adb shell am start -W -n $PKG/app.sakina.quran.MainActivity \
    --es lang "$1" --es route "$2" --es theme CLASSIC_TEAL --es mode "$3" --es glass FULL --es icons AUTO --es onboarded true
  sleep 7
}
for lang in ar en; do
  adb shell cmd locale set-app-locales $PKG --locales $lang || true
  for mode in DARK; do
    m=$(echo $mode | tr A-Z a-z)
    launch $lang home $mode;    shot "${lang}_${m}_01_home"
    adb shell input swipe 540 1800 540 700 500; shot "${lang}_${m}_02_home_scrolled"
    launch $lang prayers $mode; shot "${lang}_${m}_03_prayers"
    adb shell input swipe 540 1800 540 700 500; shot "${lang}_${m}_04_prayers_scrolled"
  done
done
# one light-mode Home for comparison
launch ar home LIGHT; shot "ar_light_01_home"
launch en prayers LIGHT; shot "en_light_03_prayers"
launch ar explore DARK; shot "ar_dark_05_explore"
timeout 20 adb shell uiautomator dump /sdcard/ui.xml >/dev/null; adb pull /sdcard/ui.xml "shots/last_ui.xml" >/dev/null
adb logcat -d -b crash > shots/crash.txt || true
adb logcat -d | grep -i "sakina\|AndroidRuntime" | tail -300 > shots/logcat.txt || true
