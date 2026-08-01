#!/system/bin/sh

MODDIR=${0%/*}

if [ -f "$MODDIR/logs/watchdog.pid" ]; then
    kill "$(cat "$MODDIR/logs/watchdog.pid")" 2>/dev/null
fi

for request in /data/user/*/com.cirrest.dolbycontrol.mdph001/files/restart_audio_service.request; do
    [ -f "$request" ] && rm -f "$request"
done

if [ -f "$MODDIR/.reenable-atmos-on-uninstall" ]; then
    rm -f /data/adb/modules/Atmos/disable
fi
