#!/bin/bash
# Q2 screenshots: mushaf, tafsir, translations, khatma, stats, share studio, reciters, topics, surah info, marks, tajweed
# + regression of every existing screen. Theme CLASSIC_TEAL. One emulator job per set (SHOT_SET=ar_dark|en_dark|ar_light),
# so every set gets its own time budget. Sample data via debug extras qseed/qset.
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
  timeout 10 adb shell uiautomator dump /sdcard/d.xml >/dev/null 2>&1; adb pull /sdcard/d.xml /tmp/d.xml >/dev/null 2>&1
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
shot() { sleep 2; dismiss; adb exec-out screencap -p > "shots/$1.png"; }
snap() { adb exec-out screencap -p > "shots/$1.png"; }
launch() { # lang route mode [qset]
  adb shell am force-stop $PKG
  adb shell "am start -W -n $PKG/app.sakina.quran.MainActivity --es lang $1 --es route '$2' --es theme CLASSIC_TEAL --es mode $3 --es glass FULL --es icons AUTO --es onboarded true --es qseed 1 --es qset '${4:-tajweed=0,paper=0,translit=0,tafsir=0,script=0,tr=auto}'"
  sleep 6
}
# Wait (max $1 s) until no loading text is on screen.
waitloaded() {
  for i in $(seq 1 ${1:-15}); do
    timeout 8 adb shell uiautomator dump /sdcard/w.xml >/dev/null 2>&1; adb pull /sdcard/w.xml /tmp/w.xml >/dev/null 2>&1
    grep -q "جارٍ التحميل\|Loading…" /tmp/w.xml || return 0
    sleep 1
  done
  echo "STILL LOADING after ${1:-15}s"
}
crashdump() { adb logcat -d -b crash > shots/crash.txt 2>/dev/null || true; }
SET=${SHOT_SET:-ar_dark}
lang=${SET%_*}; m=${SET#*_}; M=$(echo $m | tr a-z A-Z)
adb shell cmd locale set-app-locales $PKG --locales $lang || true
launch $lang quran $M;                         shot "${lang}_${m}_q01_quran_tab"
launch $lang "mushaf/2" $M;                    shot "${lang}_${m}_q02_mushaf_p2"
launch $lang "mushaf/42" $M "tajweed=1";       shot "${lang}_${m}_q03_mushaf_tajweed"
adb shell input tap 540 1150; sleep 3;         shot "${lang}_${m}_q04_ayah_sheet"
adb shell input swipe 540 1900 540 500 500; sleep 1; waitloaded 15; shot "${lang}_${m}_q04b_ayah_sheet_tafsir"
launch $lang "mushaf/582" $M "tajweed=0,paper=1"; shot "${lang}_${m}_q05_mushaf_sepia"
launch $lang "reader/2/255" $M "translit=1,tafsir=1,tr=en.sahih"; waitloaded 10; shot "${lang}_${m}_q06_reader_tr"
# Auto-scroll (QR17): start it from the top-bar button; the pill must appear only now and not cover the text.
if [ $lang = ar ]; then ax=330; else ax=750; fi
adb shell input tap $ax 205; sleep 3; snap "${lang}_${m}_q06c_reader_autoscroll"
launch $lang "tafsir/2/255" $M;                waitloaded 15; shot "${lang}_${m}_q07_tafsir"
launch $lang quran_library $M;                 shot "${lang}_${m}_q08_library"
launch $lang khatma $M;                        shot "${lang}_${m}_q09_khatma"
launch $lang khatma_new $M;                    shot "${lang}_${m}_q10_khatma_new"
launch $lang quran_stats $M;                   shot "${lang}_${m}_q11_stats"
adb shell input swipe 540 1800 540 600 500; sleep 2; shot "${lang}_${m}_q11b_stats_challenges"
launch $lang "share/2/255/255" $M;             sleep 2; shot "${lang}_${m}_q12_share"
launch $lang reciters $M;                      shot "${lang}_${m}_q13_reciters"
launch $lang "topic/patience" $M;              shot "${lang}_${m}_q14_topic"
launch $lang "surah_info/18" $M;               shot "${lang}_${m}_q15_surah_info"
launch $lang quran_marks $M;                   shot "${lang}_${m}_q16_marks"
launch $lang tajweed $M;                       shot "${lang}_${m}_q17_tajweed"
if [ $lang = ar ]; then q='%D8%B1%D8%AD%D9%85%D8%A9'; else q='mercy'; fi
launch $lang "quran_search?q=$q" $M;           shot "${lang}_${m}_q18_search"
# Regression of every existing screen
launch $lang home $M;                          shot "${lang}_${m}_r01_home"
launch $lang prayers $M;                       shot "${lang}_${m}_r02_prayers"
launch $lang explore $M;                       shot "${lang}_${m}_r03_explore"
launch $lang "reader/1/1" $M;                  shot "${lang}_${m}_r04_reader"
launch $lang azkar $M;                         shot "${lang}_${m}_r05_azkar"
launch $lang "azkar/morning@1" $M;             shot "${lang}_${m}_r06_azkar_session"
launch $lang "hisn?cat=SICK" $M;               shot "${lang}_${m}_r07_hisn"
launch $lang tasbih $M;                        shot "${lang}_${m}_r08_tasbih"
launch $lang names $M;                         shot "${lang}_${m}_r09_names"
launch $lang profile $M;                       shot "${lang}_${m}_r10_profile"
launch $lang "onboarding?step=0" $M;           shot "${lang}_${m}_r11_onboarding"
launch $lang "duas?tab=1" $M;                  shot "${lang}_${m}_r12_duas_refs"
crashdump
mv shots/crash.txt shots/crash_${SET}.txt 2>/dev/null || true
adb logcat -d | grep -i "sakina\|AndroidRuntime\|FATAL" | tail -300 > shots/logcat_${SET}.txt || true
echo "SHOTS DONE $SET"
