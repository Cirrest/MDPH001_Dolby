#!/system/bin/sh

MODDIR=${0%/*}

if [ -f "$MODDIR/logs/watchdog.pid" ]; then
    kill "$(cat "$MODDIR/logs/watchdog.pid")" 2>/dev/null
fi

for request in /data/user/*/com.cirrest.dolbycontrol.mdph001/files/restart_audio_service.request; do
    [ -f "$request" ] && rm -f "$request"
done

for marker in /data/user/*/com.cirrest.dolbycontrol.mdph001/files/global_processing.disabled; do
    [ -f "$marker" ] && rm -f "$marker"
done

if [ -f "$MODDIR/.usb-persist-config" ]; then
    usb_persist_config="$(cat "$MODDIR/.usb-persist-config")"
    if [ -n "$usb_persist_config" ]; then
        resetprop -n persist.sys.usb.config "$usb_persist_config"
    else
        resetprop -d persist.sys.usb.config
    fi
fi

if [ -f "$MODDIR/.reenable-atmos-on-uninstall" ]; then
    rm -f /data/adb/modules/Atmos/disable
fi
