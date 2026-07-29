#!/system/bin/sh

MODDIR=${0%/*}
EXPECTED_DEVICE="MD_PH_001"
EXPECTED_ANDROID="14"

if [ "$(getprop ro.product.device)" != "$EXPECTED_DEVICE" ] ||
   [ "$(getprop ro.build.version.release)" != "$EXPECTED_ANDROID" ]; then
    touch "$MODDIR/disable"
    exit 0
fi

MAGISKPOLICY="/data/adb/magisk/magiskpolicy"
[ -x "$MAGISKPOLICY" ] || MAGISKPOLICY="/system_ext/bin/magiskpolicy"
"$MAGISKPOLICY" --live --apply "$MODDIR/sepolicy.rule"

chcon u:object_r:vendor_configs_file:s0 "$MODDIR/system/vendor/etc/media_codecs.xml"
chcon u:object_r:vendor_configs_file:s0 "$MODDIR/system/vendor/etc/media_codecs_c2_dolby_audio.xml"
chcon u:object_r:vendor_configs_file:s0 "$MODDIR/system/vendor/etc/audio_effects.xml"
chcon u:object_r:vendor_configs_file:s0 \
    "$MODDIR/system/vendor/odm/etc/dolby/multimedia_dolby_dax_default.xml"
chcon -R u:object_r:vendor_file:s0 "$MODDIR/system/vendor/lib"
chcon -R u:object_r:vendor_file:s0 "$MODDIR/system/vendor/lib64"

mkdir -p /data/vendor/dolby
chown media:media /data/vendor/dolby
chmod 0770 /data/vendor/dolby
