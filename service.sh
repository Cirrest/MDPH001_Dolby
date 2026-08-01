#!/system/bin/sh

MODDIR=${0%/*}
LOGDIR="$MODDIR/logs"
LIBDIR="$MODDIR/payload/lib64"
BINDIR="$MODDIR/payload/bin"
EXPECTED_DEVICE="MD_PH_001"
EXPECTED_ANDROID="14"

if [ "$(getprop ro.product.device)" != "$EXPECTED_DEVICE" ] ||
   [ "$(getprop ro.build.version.release)" != "$EXPECTED_ANDROID" ]; then
    touch "$MODDIR/disable"
    exit 0
fi

mkdir -p "$LOGDIR"
echo $$ >"$LOGDIR/watchdog.pid"

export LD_LIBRARY_PATH="$LIBDIR:/apex/com.android.media.swcodec/lib64:/system/lib64:/system_ext/lib64:/vendor/lib64"

resetprop -n ro.audio.ignore_effects false
resetprop -n ro.atmos.music_stream false
resetprop -n dolby.monospeaker false

start_dms() {
    if ! pidof vendor.dolby.dms.service >/dev/null 2>&1; then
        "$BINDIR/vendor.dolby.dms.service" >>"$LOGDIR/dms.log" 2>&1 &
        echo $! >"$LOGDIR/dms.pid"
    fi
}

start_codec2() {
    if ! pidof vendor.dolby_sp.media.c2@1.0-service >/dev/null 2>&1; then
        "$BINDIR/vendor.dolby_sp.media.c2@1.0-service" >>"$LOGDIR/codec2.log" 2>&1 &
        echo $! >"$LOGDIR/codec2.pid"
    fi
}

wait_for_dms() {
    attempt=0
    while [ "$attempt" -lt 50 ]; do
        if service check vendor.dolby.dms.IDms/default 2>/dev/null | grep -q "found"; then
            return 0
        fi
        sleep 0.2
        attempt=$((attempt + 1))
    done
    return 1
}

wait_for_codec2() {
    attempt=0
    while [ "$attempt" -lt 50 ]; do
        if lshal 2>/dev/null | grep -q "android.hardware.media.c2@1.0::IComponentStore/default1"; then
            return 0
        fi
        sleep 0.2
        attempt=$((attempt + 1))
    done
    return 1
}

start_dax2() {
    if pidof com.cirrest.atmos.DAX2 >/dev/null 2>&1; then
        return 0
    fi
    am startservice -a android.dolby.IDs \
        -n com.cirrest.atmos.DAX2/com.atmos.DsService \
        >>"$LOGDIR/dax2.log" 2>&1
}

handle_audio_restart_requests() {
    for request in /data/user/*/com.cirrest.dolbycontrol.mdph001/files/restart_audio_service.request; do
        [ -f "$request" ] || continue
        rm -f "$request"
        echo "$(date '+%Y-%m-%d %H:%M:%S') restarting audioserver" \
            >>"$LOGDIR/audio-restart.log"
        setprop ctl.restart audioserver
    done
}

start_dms
if wait_for_dms; then
    start_codec2
    if wait_for_codec2; then
        setprop ctl.restart media
    fi
else
    echo "Dolby DMS service did not register" >>"$LOGDIR/codec.log"
fi

until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 2
done

start_dax2
audio_pid="$(pidof audioserver)"
watchdog_ticks=0

while true; do
    handle_audio_restart_requests

    watchdog_ticks=$((watchdog_ticks + 1))
    if [ "$watchdog_ticks" -ge 5 ]; then
        watchdog_ticks=0
        if ! pidof vendor.dolby.dms.service >/dev/null 2>&1; then
            start_dms
            wait_for_dms
        fi
        if ! pidof vendor.dolby_sp.media.c2@1.0-service >/dev/null 2>&1; then
            start_codec2
            if wait_for_codec2; then
                setprop ctl.restart media
            fi
        fi
    fi

    next_audio_pid="$(pidof audioserver)"
    if [ -n "$next_audio_pid" ] && [ "$next_audio_pid" != "$audio_pid" ]; then
        audio_pid="$next_audio_pid"
        killall com.cirrest.atmos.DAX2 2>/dev/null
        sleep 1
        start_dax2
    elif ! pidof com.cirrest.atmos.DAX2 >/dev/null 2>&1; then
        start_dax2
    fi

    sleep 3
done
