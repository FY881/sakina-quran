#!/bin/bash
# D1b screenshots: theme CLASSIC_TEAL (dark), AR + EN, every new screen via debug intent extras.
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
launch() { # lang route mode [onboarded]
  adb shell am force-stop $PKG
  adb shell "am start -W -n $PKG/app.sakina.quran.MainActivity --es lang $1 --es route '$2' --es theme CLASSIC_TEAL --es mode $3 --es glass FULL --es icons AUTO --es onboarded ${4:-true}"
  sleep 7
}
for lang in ar en; do
  adb shell cmd locale set-app-locales $PKG --locales $lang || true
  m=dark
  launch $lang home DARK;            shot "${lang}_${m}_01_home"
  launch $lang prayers DARK;         shot "${lang}_${m}_02_prayers"
  launch $lang explore DARK;         shot "${lang}_${m}_03_explore"
  adb shell input swipe 540 1900 540 500 400; sleep 1; shot "${lang}_${m}_04_explore_scrolled"
  if [ $lang = ar ]; then q='%D8%B2%D9%83%D8%A7%D8%A9'; else q='prayer'; fi
  launch $lang "explore?q=$q" DARK;  shot "${lang}_${m}_05_explore_search"
  launch $lang "explore?soon=hajj" DARK; shot "${lang}_${m}_06_soon_sheet"
  launch $lang onboarding DARK false; shot "${lang}_${m}_07_onboarding"
  launch $lang "onboarding?step=2" DARK false; shot "${lang}_${m}_08_onboarding_location"
  launch $lang profile DARK;         shot "${lang}_${m}_09_profile"
  launch $lang themes DARK;          shot "${lang}_${m}_10_themes"
  launch $lang tasbih DARK
  for i in 1 2 3 4 5 6 7; do adb shell input tap 540 1000; done
  shot "${lang}_${m}_11_tasbih"
  launch $lang names DARK;           shot "${lang}_${m}_12_names"
  launch $lang quran DARK;           shot "${lang}_${m}_13_quran_index"
  launch $lang "reader/2/1" DARK;    shot "${lang}_${m}_14_quran_reader"
  launch $lang iconpacks DARK;       shot "${lang}_${m}_15_iconpacks"
done
launch ar "quran_search?q=%D8%A7%D9%84%D8%B5%D8%A8%D8%B1" DARK; shot "ar_dark_16_quran_search"
launch ar home LIGHT; shot "ar_light_01_home"
launch ar tasbih LIGHT; shot "ar_light_11_tasbih"
timeout 20 adb shell uiautomator dump /sdcard/ui.xml >/dev/null; adb pull /sdcard/ui.xml "shots/last_ui.xml" >/dev/null
adb logcat -d -b crash > shots/crash.txt || true
adb logcat -d | grep -i "sakina\|AndroidRuntime" | tail -300 > shots/logcat.txt || true
