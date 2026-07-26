#!/system/bin/sh

EXPECTED_DEVICE="MD_PH_001"
EXPECTED_FINGERPRINT="MOONDROP/MD_PH_001/MD_PH_001:14/UP1A.231005.007/1764183053:user/release-keys"

DEVICE="$(getprop ro.product.device)"
FINGERPRINT="$(getprop ro.build.fingerprint)"

ui_print "- 安装前的设备兼容检查"
ui_print "- Checking the target device"
[ "$DEVICE" = "$EXPECTED_DEVICE" ] || abort "Unsupported device: $DEVICE"
[ "$FINGERPRINT" = "$EXPECTED_FINGERPRINT" ] || abort "Unsupported build fingerprint: $FINGERPRINT"
[ "$ARCH" = "arm64" ] || abort "Unsupported architecture: $ARCH"
[ "$API" -eq 34 ] || abort "Unsupported Android API: $API"

ui_print "- ✔设备检查通过，进行安装"
ui_print "- "
ui_print "- "
ui_print "- "
ui_print "- 该模块安装以下为MD-PH-001适配的DolbyAtmos服务及依赖"
ui_print "- AC-3, E-AC-3 ， AC-4 音频解码器"
ui_print "- DMS、DXP服务, DAP控制器及游戏DAP等"
ui_print "- DVL服务会正常注册，但不会自动附加到该音频框架。(省的全局SRC绕过这特色功能没了)"
ui_print "- "
ui_print "- "
ui_print "- "
ui_print "- ⚠该模块为免费模块，自己逆向半年kernel适配手搓的成果之一⚠"
ui_print "- ⚠禁止任何形式商业化、收费、二改⚠"
ui_print "- ⚠该模块所有文件均已打上数字水印，均可追溯⚠"

set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm "$MODPATH/customize.sh" 0 0 0755
set_perm "$MODPATH/post-fs-data.sh" 0 0 0755
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/uninstall.sh" 0 0 0755
set_perm_recursive "$MODPATH/payload/bin" 0 0 0755 0755
set_perm_recursive "$MODPATH/payload/lib64" 0 0 0755 0644
set_perm_recursive "$MODPATH/system/priv-app/DolbyControl" 0 0 0755 0644
