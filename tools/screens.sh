#!/bin/bash
# 에뮬레이터에서 앱 화면을 캡처해 shots/ 에 남긴다. (.github/workflows/screens.yml)
set -x
adb install -r android/app/build/outputs/apk/release/app-release.apk
mkdir -p shots
adb shell settings put system screen_off_timeout 600000
sleep 40
adb shell pm disable-user --user 0 com.google.android.apps.nexuslauncher || true
clear_anr
# 에뮬레이터 런처가 '응답 없음' 창을 띄우면 [기다리기]를 눌러 닫는다
clear_anr() {
  for k in 1 2 3; do
    adb shell dumpsys window | grep -q "Application Not Responding" || return 0
    adb shell input tap 320 1360; sleep 2
  done
}
cap() { clear_anr; adb exec-out screencap -p > "shots/$1.png"; }
down() { clear_anr; adb shell input swipe 540 1900 540 700 400; sleep 2; }
open_tab() { adb shell am start -S -W -n com.moon.lotto/.MainActivity --es tab "$1" --ez demo true; sleep 8; }

for mode in light dark; do
  [ "$mode" = dark ] && adb shell cmd uimode night yes
  open_tab GEN;  cap "$mode-1gen-a"; down; cap "$mode-1gen-b"; down; cap "$mode-1gen-c"
  [ "$mode" = dark ] && continue
  open_tab STAT; cap "$mode-2stat-a"; down; cap "$mode-2stat-b"; down; down; cap "$mode-2stat-c"
  open_tab SIM;  cap "$mode-3sim-a"; down; cap "$mode-3sim-b"
  open_tab MINE; cap "$mode-4mine-a"; down; cap "$mode-4mine-b"
  open_tab CAFE; cap "$mode-5cafe-a"; down; cap "$mode-5cafe-b"; down; cap "$mode-5cafe-c"
  open_tab HELP; cap "$mode-6help-a"
done
ls -la shots
