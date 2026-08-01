#!/system/bin/sh

MODDIR=${0%/*}
LOGDIR="$MODDIR/logs"
LIBDIR="$MODDIR/payload/lib64"
BINDIR="$MODDIR/payload/bin"
DOLBY_LD_LIBRARY_PATH="$LIBDIR:/apex/com.android.media.swcodec/lib64:/system/lib64:/system_ext/lib64:/vendor/lib64"
EXPECTED_DEVICE="MD_PH_001"
EXPECTED_ANDROID="14"
USB_RESTORE_ATTEMPTS=10
ORIGINAL_IGNORE_EFFECTS="$(getprop ro.audio.ignore_effects)"
ORIGINAL_ATMOS_MUSIC_STREAM="$(getprop ro.atmos.music_stream)"
ORIGINAL_MONOSPEAKER="$(getprop dolby.monospeaker)"
dolby_properties_active=0

if [ "$(getprop ro.product.device)" != "$EXPECTED_DEVICE" ] ||
   [ "$(getprop ro.build.version.release)" != "$EXPECTED_ANDROID" ]; then
    touch "$MODDIR/disable"
    exit 0
fi

mkdir -p "$LOGDIR"
echo $$ >"$LOGDIR/watchdog.pid"

restore_audio_property() {
    name="$1"
    value="$2"
    if [ -n "$value" ]; then
        resetprop -n "$name" "$value"
    else
        resetprop -d "$name"
    fi
}

apply_dolby_properties() {
    [ "$dolby_properties_active" -eq 1 ] && return 0
    resetprop -n ro.audio.ignore_effects false
    resetprop -n ro.atmos.music_stream false
    resetprop -n dolby.monospeaker false
    dolby_properties_active=1
}

restore_original_audio_properties() {
    [ "$dolby_properties_active" -eq 0 ] && return 0
    restore_audio_property ro.audio.ignore_effects "$ORIGINAL_IGNORE_EFFECTS"
    restore_audio_property ro.atmos.music_stream "$ORIGINAL_ATMOS_MUSIC_STREAM"
    restore_audio_property dolby.monospeaker "$ORIGINAL_MONOSPEAKER"
    resetprop -d dolby.ds.state
    dolby_properties_active=0
}

apply_dolby_properties

start_dms() {
    if ! pidof vendor.dolby.dms.service >/dev/null 2>&1; then
        LD_LIBRARY_PATH="$DOLBY_LD_LIBRARY_PATH" \
            "$BINDIR/vendor.dolby.dms.service" >>"$LOGDIR/dms.log" 2>&1 &
        echo $! >"$LOGDIR/dms.pid"
    fi
}

start_codec2() {
    if ! pidof vendor.dolby_sp.media.c2@1.0-service >/dev/null 2>&1; then
        LD_LIBRARY_PATH="$DOLBY_LD_LIBRARY_PATH" \
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

is_global_processing_disabled() {
    for marker in /data/user/*/com.cirrest.dolbycontrol.mdph001/files/global_processing.disabled; do
        [ -f "$marker" ] && return 0
    done
    return 1
}

stop_dax2() {
    if pidof com.cirrest.atmos.DAX2 >/dev/null 2>&1; then
        killall com.cirrest.atmos.DAX2 2>/dev/null
    fi
}

restore_usb_data_function() {
    [ -f "$MODDIR/.usb-data-function" ] || return 0
    saved_function="$(cat "$MODDIR/.usb-data-function")"
    case "$saved_function" in
        mtp|ptp) ;;
        *) return 0 ;;
    esac
    attempt=0
    while [ "$attempt" -lt "$USB_RESTORE_ATTEMPTS" ]; do
        current_state="$(getprop sys.usb.state)"
        data_ready="$(getprop "vendor.usb.ffs.$saved_function.ready")"
        data_link="$(readlink /config/usb_gadget/g1/configs/b.1/f1 2>/dev/null)"
        if [ "$data_ready" = "1" ]; then
            case ",$current_state," in
                *,$saved_function,*)
                    case "$data_link" in
                        */ffs.$saved_function)
                            echo "$(date '+%Y-%m-%d %H:%M:%S') USB=$current_state/$data_link" \
                                >>"$LOGDIR/usb.log"
                            return 0
                            ;;
                    esac
                    ;;
            esac
        fi
        env -u LD_LIBRARY_PATH svc usb setFunctions "$saved_function" >/dev/null 2>&1
        attempt=$((attempt + 1))
        sleep 1
    done
    echo "$(date '+%Y-%m-%d %H:%M:%S') USB restore failed" \
        >>"$LOGDIR/usb.log"
    return 1
}

start_dax2() {
    if is_global_processing_disabled; then
        return 0
    fi
    if pidof com.cirrest.atmos.DAX2 >/dev/null 2>&1; then
        return 0
    fi
    if ! am start-foreground-service \
        -n com.cirrest.dolbycontrol.mdph001/com.mdph.dolbycontrol.DolbyControlService \
        >/dev/null 2>&1; then
        echo "$(date '+%Y-%m-%d %H:%M:%S') controller start failed" \
            >>"$LOGDIR/dax2.log"
        return 1
    fi
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
    if ! wait_for_codec2; then
        echo "Dolby Codec2 service did not register" >>"$LOGDIR/codec.log"
    fi
else
    echo "Dolby DMS service did not register" >>"$LOGDIR/codec.log"
fi

until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 2
done

if is_global_processing_disabled; then
    restore_original_audio_properties
    stop_dax2
else
    apply_dolby_properties
    start_dax2
fi
restore_usb_data_function &
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
            wait_for_codec2
        fi
        if ! is_global_processing_disabled &&
           ! pidof com.cirrest.atmos.DAX2 >/dev/null 2>&1; then
            start_dax2
        fi
    fi

    next_audio_pid="$(pidof audioserver)"
    audio_restarted=0
    if [ -n "$next_audio_pid" ] && [ "$next_audio_pid" != "$audio_pid" ]; then
        audio_pid="$next_audio_pid"
        audio_restarted=1
    fi

    if is_global_processing_disabled; then
        restore_original_audio_properties
        stop_dax2
    else
        apply_dolby_properties
        if [ "$audio_restarted" -eq 1 ]; then
            stop_dax2
            sleep 1
            start_dax2
        fi
    fi

    sleep 3
done
