#!/system/bin/sh

MODDIR=${0%/*}

for pidfile in "$MODDIR/logs/watchdog.pid" "$MODDIR/logs/codec2.pid" "$MODDIR/logs/dms.pid"; do
    if [ -f "$pidfile" ]; then
        kill "$(cat "$pidfile")" 2>/dev/null
    fi
done

if [ -f /odm/.md_ph_001_dolby_probe ]; then
    umount /odm 2>/dev/null
fi

setprop ctl.restart media
