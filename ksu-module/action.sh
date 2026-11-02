#!/system/bin/sh
LOGDIR=/data/adb/fuxi_haptics
if [ -f "$LOGDIR/runtime-status.txt" ]; then
  cat "$LOGDIR/runtime-status.txt"
else
  echo "Fuxi Haptics Engine: runtime status not found. The module service may not have run yet."
fi
