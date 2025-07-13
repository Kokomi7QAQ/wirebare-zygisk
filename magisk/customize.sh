
ui_print "****************************************"
ui_print "  Check boot mode..."
ui_print "  INFO: boot mode: $BOOTMODE"
if $BOOTMODE; then
  ui_print "****************************************"
  ui_print "  Install from Magisk now."
  ui_print "****************************************"
else
  ui_print "****************************************"
  ui_print "  ERROR: Install from recovery is NOT supported!"
  ui_print "  ERROR: Please install from Magisk!"
  abort "****************************************"
fi

ui_print "****************************************"
ui_print "  Check api version..."
ui_print "  INFO: api: $API"
if [ "$API" -lt 34 ]; then
  rm -rf "$MODPATH/zygisk"
fi
ui_print "****************************************"


