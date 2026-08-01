#!/system/bin/sh

MODDIR=${0%/*}
EXPECTED_DEVICE="MD_PH_001"
EXPECTED_ANDROID="14"

if [ "$(getprop ro.product.device)" != "$EXPECTED_DEVICE" ] ||
   [ "$(getprop ro.build.version.release)" != "$EXPECTED_ANDROID" ]; then
    touch "$MODDIR/disable"
    exit 0
fi

if command -v magiskpolicy >/dev/null 2>&1; then
    magiskpolicy --live --apply "$MODDIR/sepolicy.rule" 2>/dev/null
elif [ -x /data/adb/magisk/magiskpolicy ]; then
    /data/adb/magisk/magiskpolicy --live --apply "$MODDIR/sepolicy.rule" 2>/dev/null
fi

for config in \
    "$MODDIR/system/vendor/etc/audio_effects.xml" \
    "$MODDIR/system/vendor/etc/media_codecs.xml" \
    "$MODDIR/system/vendor/etc/media_codecs_c2_dolby_audio.xml" \
    "$MODDIR/system/vendor/etc/vintf/manifest/vendor.dolby.dms.xml" \
    "$MODDIR/system/vendor/etc/vintf/manifest/vendor.dolby.media.c2.xml" \
    "$MODDIR/system/vendor/odm/etc/dolby/multimedia_dolby_dax_default.xml"; do
    [ -f "$config" ] && chcon u:object_r:vendor_configs_file:s0 "$config"
done
chcon -R u:object_r:vendor_file:s0 "$MODDIR/system/vendor/lib"
chcon -R u:object_r:vendor_file:s0 "$MODDIR/payload/lib64" "$MODDIR/payload/bin"
chcon u:object_r:vendor_file:s0 "$MODDIR/payload/lib64/libcodec2_soft_ac4dec_sp.so" \
    "$MODDIR/payload/lib64/libcodec2_soft_ddpdec_sp.so" 2>/dev/null
chcon -R u:object_r:system_file:s0 "$MODDIR/system/priv-app"

chmod 0755 "$MODDIR/payload/bin/vendor.dolby.dms.service" \
    "$MODDIR/payload/bin/vendor.dolby_sp.media.c2@1.0-service" 2>/dev/null

mkdir -p /data/vendor/dolby
chown media:media /data/vendor/dolby
chmod 0770 /data/vendor/dolby
