#!/bin/bash
# A1 screenshots (azkar/duas/ruqyah/tasbih/names quiz); D1b core kept: theme CLASSIC_TEAL (dark), AR + EN, every new screen via debug intent extras.
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
tap_text() { # tap the first node whose text contains $1
  timeout 20 adb shell uiautomator dump /sdcard/t.xml >/dev/null 2>&1; adb pull /sdcard/t.xml /tmp/t.xml >/dev/null 2>&1
  python3 - "$1" <<'PY'
import re,subprocess,sys
x=open('/tmp/t.xml',encoding='utf-8').read(); t=sys.argv[1]
for m in re.finditer(r'<node [^>]*?text="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',x):
  if t in m.group(1):
    a,b,c,d=map(int,m.groups()[1:]); subprocess.call(['adb','shell','input','tap',str((a+c)//2),str((b+d)//2)]); print('tapped',t); break
else: print('notfound',t)
PY
}
crashdump() { adb logcat -d -b crash > shots/crash.txt 2>/dev/null || true; }
snap() { adb exec-out screencap -p > "shots/$1.png"; }
for lang in ar en; do
  adb shell cmd locale set-app-locales $PKG --locales $lang || true
  m=dark
  launch $lang azkar DARK;             shot "${lang}_${m}_a01_azkar"
  adb shell input swipe 540 1900 540 300 600; sleep 2; shot "${lang}_${m}_a03_azkar_reminders"
  launch $lang "azkar/morning@3" DARK
  adb shell input tap 540 1000; sleep 1; shot "${lang}_${m}_a05_session_counted"
  launch $lang "azkar/sleep@7" DARK;   shot "${lang}_${m}_a06_session_sleep33"
  launch $lang "hisn?cat=SICK" DARK;   shot "${lang}_${m}_a08_hisn_sick"
  launch $lang duas DARK;              shot "${lang}_${m}_a10_duas_quran"
  launch $lang "duas?tab=1" DARK;      shot "${lang}_${m}_a11_duas_prophetic"
  launch $lang ruqyah DARK;            shot "${lang}_${m}_a12_ruqyah"
  launch $lang istikhara DARK
  adb shell input swipe 540 1900 540 500 600; sleep 2; shot "${lang}_${m}_a13_istikhara_dua"
  launch $lang names_quiz DARK
  if [ $lang = ar ]; then tap_text "ابدأ الاختبار"; else tap_text "Start the quiz"; fi
  sleep 2; adb shell input tap 540 1350; sleep 0.3; snap "${lang}_${m}_a16_quiz_answered"
  launch $lang tasbih DARK
  adb shell input swipe 540 1900 540 400 600; sleep 1; adb shell input swipe 540 1900 540 700 600; sleep 2; shot "${lang}_${m}_a19_tasbih_options"
  launch $lang azkar_mine DARK;        shot "${lang}_${m}_a20_mine"
  if [ $lang = ar ]; then q='%D8%A7%D8%B0%D9%83%D8%A7%D8%B1'; else q='azkar'; fi
  launch $lang "explore?q=$q" DARK;    shot "${lang}_${m}_a21_explore_azkar"
  crashdump
done
adb shell cmd locale set-app-locales $PKG --locales ar || true
launch ar "azkar/evening" LIGHT; shot "ar_light_a04_session_evening"
launch ar tasbih LIGHT;          shot "ar_light_a17_tasbih"
launch ar ruqyah LIGHT;          shot "ar_light_a12_ruqyah"
launch ar profile DARK;          shot "ar_dark_r09_profile"
launch ar home DARK;             shot "ar_dark_r01_home"
crashdump
adb logcat -d | grep -i "sakina\|AndroidRuntime\|FATAL" | tail -300 > shots/logcat.txt || true
