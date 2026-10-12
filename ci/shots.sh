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
for lang in ar en; do
  adb shell cmd locale set-app-locales $PKG --locales $lang || true
  m=dark
  launch $lang home DARK;              shot "${lang}_${m}_01_home"
  launch $lang azkar DARK;             shot "${lang}_${m}_a01_azkar"
  adb shell input swipe 540 1900 540 500 400; sleep 1; shot "${lang}_${m}_a02_azkar_scrolled"
  adb shell input swipe 540 1900 540 300 400; sleep 1; shot "${lang}_${m}_a03_azkar_reminders"
  launch $lang "azkar/morning" DARK
  adb shell input tap 540 1000; sleep 1; shot "${lang}_${m}_a04_session_morning"
  launch $lang "azkar/morning@3" DARK
  adb shell input tap 540 1000; sleep 1; shot "${lang}_${m}_a05_session_counted"
  launch $lang "azkar/sleep@7" DARK;   shot "${lang}_${m}_a06_session_sleep33"
  launch $lang hisn DARK;              shot "${lang}_${m}_a07_hisn"
  launch $lang "hisn?cat=SICK" DARK;   shot "${lang}_${m}_a08_hisn_sick"
  launch $lang "azkar_search?q=%D8%A7%D9%84%D8%B3%D9%81%D8%B1" DARK; shot "${lang}_${m}_a09_search"
  launch $lang duas DARK;              shot "${lang}_${m}_a10_duas_quran"
  launch $lang "duas?tab=1" DARK;      shot "${lang}_${m}_a11_duas_prophetic"
  launch $lang ruqyah DARK;            shot "${lang}_${m}_a12_ruqyah"
  launch $lang istikhara DARK;         shot "${lang}_${m}_a13_istikhara"
  launch $lang names_quiz DARK;        shot "${lang}_${m}_a14_quiz_start"
  if [ $lang = ar ]; then tap_text "ابدأ الاختبار"; else tap_text "Start the quiz"; fi
  sleep 2; shot "${lang}_${m}_a15_quiz_question"
  adb shell input tap 540 1500; sleep 0.4; shot "${lang}_${m}_a16_quiz_answered"
  launch $lang tasbih DARK
  for i in 1 2 3 4 5 6 7; do adb shell input tap 540 1000; done
  shot "${lang}_${m}_a17_tasbih"
  adb shell input swipe 540 1900 540 400 400; sleep 1; shot "${lang}_${m}_a18_tasbih_chart"
  adb shell input swipe 540 1900 540 400 400; sleep 1; shot "${lang}_${m}_a19_tasbih_options"
  launch $lang azkar_mine DARK;        shot "${lang}_${m}_a20_mine"
  if [ $lang = ar ]; then q='%D8%A7%D8%B0%D9%83%D8%A7%D8%B1'; else q='azkar'; fi
  launch $lang "explore?q=$q" DARK;    shot "${lang}_${m}_a21_explore_azkar"
  launch $lang names DARK;             shot "${lang}_${m}_a22_names"
done
launch ar azkar LIGHT;          shot "ar_light_a01_azkar"
launch ar "azkar/evening" LIGHT; shot "ar_light_a04_session_evening"
launch ar duas LIGHT;           shot "ar_light_a10_duas"
launch ar tasbih LIGHT;         shot "ar_light_a17_tasbih"
launch ar ruqyah LIGHT;         shot "ar_light_a12_ruqyah"
timeout 20 adb shell uiautomator dump /sdcard/ui.xml >/dev/null; adb pull /sdcard/ui.xml "shots/last_ui.xml" >/dev/null
adb logcat -d -b crash > shots/crash.txt || true
adb logcat -d | grep -i "sakina\|AndroidRuntime" | tail -300 > shots/logcat.txt || true
