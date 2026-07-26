#!/system/bin/sh

MODDIR=${0%/*}
EXPECTED_DEVICE="MD_PH_001"
EXPECTED_FINGERPRINT="MOONDROP/MD_PH_001/MD_PH_001:14/UP1A.231005.007/1764183053:user/release-keys"

if [ "$(getprop ro.product.device)" != "$EXPECTED_DEVICE" ] ||
   [ "$(getprop ro.build.fingerprint)" != "$EXPECTED_FINGERPRINT" ]; then
    touch "$MODDIR/disable"
    exit 0
fi

LOGDIR="$MODDIR/logs"
LIBDIR="$MODDIR/payload/lib64"
BINDIR="$MODDIR/payload/bin"
mkdir -p "$LOGDIR"
echo $$ >"$LOGDIR/watchdog.pid"

export LD_LIBRARY_PATH="$LIBDIR:/apex/com.android.media.swcodec/lib64:/system/lib64:/system_ext/lib64:/vendor/lib64"

start_dms() {
    "$BINDIR/vendor.dolby.dms.service" >>"$LOGDIR/dms.log" 2>&1 &
    echo $! >"$LOGDIR/dms.pid"
}

start_codec2() {
    "$BINDIR/vendor.dolby_sp.media.c2@1.0-service" >>"$LOGDIR/codec2.log" 2>&1 &
    echo $! >"$LOGDIR/codec2.pid"
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

if ! pidof vendor.dolby.dms.service >/dev/null 2>&1; then
    start_dms
fi

wait_for_dms

if ! pidof vendor.dolby_sp.media.c2@1.0-service >/dev/null 2>&1; then
    start_codec2
fi

if wait_for_codec2; then
    setprop ctl.restart media
fi

while true; do
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

    sleep 10
done
