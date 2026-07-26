#!/system/bin/sh

MODDIR=${0%/*}
EXPECTED_DEVICE="MD_PH_001"
EXPECTED_FINGERPRINT="MOONDROP/MD_PH_001/MD_PH_001:14/UP1A.231005.007/1764183053:user/release-keys"

if [ "$(getprop ro.product.device)" != "$EXPECTED_DEVICE" ] ||
   [ "$(getprop ro.build.fingerprint)" != "$EXPECTED_FINGERPRINT" ]; then
    touch "$MODDIR/disable"
    exit 0
fi

MAGISKPOLICY="/data/adb/magisk/magiskpolicy"
[ -x "$MAGISKPOLICY" ] || MAGISKPOLICY="/system_ext/bin/magiskpolicy"
"$MAGISKPOLICY" --live --apply "$MODDIR/sepolicy.rule"

chcon u:object_r:vendor_configs_file:s0 "$MODDIR/system/vendor/etc/media_codecs.xml"
chcon u:object_r:vendor_configs_file:s0 "$MODDIR/system/vendor/etc/media_codecs_c2_dolby_audio.xml"
chcon u:object_r:vendor_configs_file:s0 "$MODDIR/system/vendor/etc/audio_effects.xml"
chcon -R u:object_r:vendor_file:s0 "$MODDIR/system/vendor/lib"
chcon -R u:object_r:vendor_file:s0 "$MODDIR/system/vendor/lib64"

if [ ! -f /odm/.md_ph_001_dolby_probe ]; then
    mount --bind "$MODDIR/payload/odm" /odm
fi

mkdir -p /data/vendor/dolby
chown media:media /data/vendor/dolby
chmod 0770 /data/vendor/dolby
