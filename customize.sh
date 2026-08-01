#!/system/bin/sh

EXPECTED_DEVICE="MD_PH_001"
EXPECTED_ANDROID="14"

DEVICE="$(getprop ro.product.device)"
ANDROID_VERSION="$(getprop ro.build.version.release)"

ui_print "- 安装前的设备兼容检查"
[ "$DEVICE" = "$EXPECTED_DEVICE" ] || abort "不受支持的设备: $DEVICE"
[ "$ANDROID_VERSION" = "$EXPECTED_ANDROID" ] || abort "不受支持的Android版本: $ANDROID_VERSION"

ui_print "- ✔设备检查通过，进行安装"
ui_print "- "
ui_print "- "
ui_print "- "
ui_print "- 该模块安装以下为MD-PH-001适配的DolbyAtmos服务及依赖"
ui_print "- AC-3, E-AC-3 ， AC-4 音频解码器"
ui_print "- DMS、DXP、DAX服务, DAP控制器、私有NDK库等"
ui_print "- DVL服务会正常注册，但不会自动附加到该音频框架。(省的全局SRC绕过这特色功能没了)"
ui_print "- "
ui_print "- "
ui_print "- "
ui_print "- ⚠该模块为免费模块，自己逆向半年kernel适配手搓的成果之一⚠"
ui_print "- ⚠禁止任何形式商业化、收费、二改⚠"
ui_print "- ⚠该模块所有文件均已打上数字水印，均可追溯⚠"

LEGACY_ATMOS="/data/adb/modules/Atmos"
if [ -d "$LEGACY_ATMOS" ] && [ ! -f "$LEGACY_ATMOS/disable" ]; then
    ui_print "- Disabling the conflicting Dolby Atmos Magic Revision module"
    touch "$LEGACY_ATMOS/disable"
    touch "$MODPATH/.reenable-atmos-on-uninstall"
fi

set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm "$MODPATH/customize.sh" 0 0 0755
set_perm "$MODPATH/post-fs-data.sh" 0 0 0755
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/uninstall.sh" 0 0 0755
set_perm_recursive "$MODPATH/system/priv-app" 0 0 0755 0644
set_perm_recursive "$MODPATH/system/vendor/lib" 0 0 0755 0644
set_perm_recursive "$MODPATH/payload" 0 0 0755 0644
set_perm "$MODPATH/payload/bin/vendor.dolby.dms.service" 0 0 0755
set_perm "$MODPATH/payload/bin/vendor.dolby_sp.media.c2@1.0-service" 0 0 0755
