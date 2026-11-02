#!/system/bin/sh
MODDIR=${0%/*}
LOGDIR=/data/adb/fuxi_haptics
mkdir -p "$LOGDIR"
chmod 0700 "$LOGDIR" 2>/dev/null
{
  echo "Fuxi Haptics Engine runtime check"
  echo "time=$(date 2>/dev/null)"
  echo "sdk=$(getprop ro.build.version.sdk)"
  echo "device=$(getprop ro.product.device)"
  echo "model=$(getprop ro.product.model)"
  echo "build=$(getprop ro.build.version.incremental)"
  echo "kernel_su=${KSU:-unknown}"
  echo "zygisk_next=not_probed_by_shell"
  echo "lsposed=not_probed_by_shell"
  echo "mode=opt_in_profiles_in_lsposed_apk"
  echo "status=foundation_loaded"
} > "$LOGDIR/runtime-status.txt"
chmod 0600 "$LOGDIR/runtime-status.txt" 2>/dev/null
exit 0
