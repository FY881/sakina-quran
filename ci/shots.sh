#!/bin/bash
set -x
PKG=app.sakina.quran
APK=$(ls app-src/app/build/outputs/apk/debug/*.apk | head -1)
mkdir -p shots
adb install -r -g "$APK"
adb shell appops set $PKG SCHEDULE_EXACT_ALARM allow || true
adb shell settings put system time_12_24 24 || true
tap_text() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
  python3 - "$1" <<'PY'
import re,sys,subprocess
x=open('/tmp/ui.xml',encoding='utf-8').read()
for t in sys.argv[1].split('|'):
  for m in re.finditer(r'<node [^>]*?(?:text|content-desc)="%s"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'%re.escape(t),x):
    a,b,c,d=map(int,m.groups()); subprocess.call(['adb','shell','input','tap',str((a+c)//2),str((b+d)//2)]); sys.exit(0)
print('notfound',sys.argv[1])
PY
}
shot() { sleep 3; adb exec-out screencap -p > "shots/$1.png"; }
for night in no yes; do
 for lang in ar en; do
  adb shell cmd uimode night $night
  adb shell cmd locale set-app-locales $PKG --locales $lang || true
  adb shell am force-stop $PKG
  adb shell am start -W -n $PKG/.MainActivity
  sleep 6
  m=$([ $night = yes ] && echo dark || echo light)
  shot "${lang}_${m}_01_home"
  adb shell input swipe 540 1700 540 600 400; shot "${lang}_${m}_02_home_scrolled"
  tap_text "الصلاة|Prayer|Prayers"; shot "${lang}_${m}_03_prayer"
  tap_text "الإعدادات|Settings|إعدادات الصلاة|Prayer settings"; shot "${lang}_${m}_04_settings"
  adb shell input keyevent 4; sleep 1
  tap_text "المزيد|More"; shot "${lang}_${m}_05_more"
  tap_text "الرئيسية|Home"; sleep 1
  tap_text "القبلة|Qibla"; shot "${lang}_${m}_06_qibla"
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null; adb pull /sdcard/ui.xml "shots/${lang}_${m}_ui.xml" >/dev/null
 done
done
adb logcat -d -b crash > shots/crash.txt || true
adb logcat -d | grep -i "sakina\|AndroidRuntime" | tail -300 > shots/logcat.txt || true
