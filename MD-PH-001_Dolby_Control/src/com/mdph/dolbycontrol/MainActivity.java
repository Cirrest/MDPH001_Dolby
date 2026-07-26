package com.codex.dolbycontrol;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Space;
import android.widget.Switch;
import android.widget.TextView;

public final class MainActivity extends Activity implements DolbyControlService.Listener {
    private static final int COLOR_TEXT = Color.rgb(31, 41, 55);
    private static final int COLOR_MUTED = Color.rgb(107, 114, 128);
    private static final int COLOR_PRIMARY = Color.rgb(15, 118, 110);
    private static final int COLOR_ACCENT = Color.rgb(245, 158, 11);
    private static final int COLOR_ERROR = Color.rgb(190, 61, 55);

    private DolbyControlService.LocalBinder service;
    private boolean bound;
    private boolean rendering;

    private TextView statusDot;
    private TextView statusText;
    private TextView routeText;
    private TextView tuningText;
    private Switch enabledSwitch;
    private final Button[] modeButtons = new Button[4];
    private final Button[] ieqButtons = new Button[4];
    private Switch dialogSwitch;
    private SeekBar dialogSeek;
    private TextView dialogValue;
    private Switch levelerSwitch;
    private Switch headphoneSwitch;
    private Switch speakerSwitch;
    private Switch geqSwitch;
    private GeqEditorView geqEditor;
    private TextView geqSelection;
    private LinearLayout geqControls;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = (DolbyControlService.LocalBinder) binder;
            bound = true;
            service.registerListener(MainActivity.this);
            service.refresh();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (service != null) {
                service.unregisterListener(MainActivity.this);
            }
            service = null;
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(250, 250, 250));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        buildUi();
        Intent serviceIntent = new Intent(this, DolbyControlService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, DolbyControlService.class), connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        if (bound) {
            service.unregisterListener(this);
            unbindService(connection);
            bound = false;
            service = null;
        }
        super.onStop();
    }

    @Override
    public void onSnapshotChanged(DolbySnapshot snapshot) {
        render(snapshot);
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(250, 250, 250));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(18), dp(20), dp(32));
        scroll.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = text("Dolby Atmos", 28, COLOR_TEXT);
        title.setGravity(Gravity.START);
        content.addView(title, matchWrap());

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusRow.setPadding(0, dp(6), 0, dp(18));
        statusDot = text("\u25cf", 14, COLOR_ERROR);
        statusText = text("\u6b63\u5728\u8fde\u63a5", 14, COLOR_MUTED);
        statusRow.addView(statusDot, wrapWrap());
        statusRow.addView(space(dp(8), 1));
        statusRow.addView(statusText, wrapWrap());
        content.addView(statusRow, matchWrap());

        enabledSwitch = rowSwitch("\u5168\u5c40 Dolby \u5904\u7406");
        enabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!rendering && service != null) {
                    service.setEnabled(isChecked);
                }
            }
        });
        content.addView(enabledSwitch, matchWrap());

        addDivider(content);
        addSectionTitle(content, "\u6a21\u5f0f");
        LinearLayout modes = segmentRow();
        String[] modeNames = {"\u52a8\u6001", "\u7535\u5f71", "\u97f3\u4e50", "\u81ea\u5b9a\u4e49"};
        for (int i = 0; i < modeButtons.length; i++) {
            final int mode = i;
            modeButtons[i] = segmentButton(modeNames[i]);
            modeButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (service != null) {
                        service.setMode(mode);
                    }
                }
            });
            modes.addView(modeButtons[i], weightedButton());
        }
        content.addView(modes, matchWrap());

        routeText = text("\u8f93\u51fa\uff1a-", 14, COLOR_TEXT);
        routeText.setPadding(0, dp(14), 0, dp(4));
        content.addView(routeText, matchWrap());
        tuningText = text("Tuning\uff1a-", 13, COLOR_MUTED);
        content.addView(tuningText, matchWrap());

        addDivider(content);
        addSectionTitle(content, "\u667a\u80fd\u5747\u8861");
        LinearLayout ieq = segmentRow();
        String[] ieqNames = {"\u5173", "\u5e73\u8861", "\u6e29\u6696", "\u7ec6\u8282"};
        for (int i = 0; i < ieqButtons.length; i++) {
            final int preset = i;
            ieqButtons[i] = segmentButton(ieqNames[i]);
            ieqButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (service != null) {
                        service.setIeq(preset);
                    }
                }
            });
            ieq.addView(ieqButtons[i], weightedButton());
        }
        content.addView(ieq, matchWrap());

        addDivider(content);
        addSectionTitle(content, "\u5bf9\u8bdd\u589e\u5f3a");
        dialogSwitch = rowSwitch("\u5bf9\u8bdd\u589e\u5f3a");
        dialogSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!rendering && service != null) {
                    service.setDialogEnabled(isChecked);
                }
            }
        });
        content.addView(dialogSwitch, matchWrap());

        LinearLayout dialogAmountRow = new LinearLayout(this);
        dialogAmountRow.setOrientation(LinearLayout.HORIZONTAL);
        dialogAmountRow.setGravity(Gravity.CENTER_VERTICAL);
        dialogAmountRow.setPadding(0, dp(8), 0, 0);
        dialogSeek = new SeekBar(this);
        dialogSeek.setMax(16);
        dialogSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                dialogValue.setText(String.valueOf(progress));
                if (fromUser && service != null) {
                    service.setDialogAmount(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        dialogAmountRow.addView(dialogSeek, new LinearLayout.LayoutParams(0, dp(42), 1f));
        dialogValue = text("0", 15, COLOR_TEXT);
        dialogValue.setGravity(Gravity.CENTER);
        dialogAmountRow.addView(dialogValue, new LinearLayout.LayoutParams(dp(42), dp(42)));
        content.addView(dialogAmountRow, matchWrap());

        addDivider(content);
        addSectionTitle(content, "\u97f3\u573a");
        levelerSwitch = rowSwitch("\u97f3\u91cf\u5747\u8861");
        levelerSwitch.setOnCheckedChangeListener(toggleListener(0));
        content.addView(levelerSwitch, matchWrap());
        headphoneSwitch = rowSwitch("\u8033\u673a\u865a\u62df\u5316");
        headphoneSwitch.setOnCheckedChangeListener(toggleListener(1));
        content.addView(headphoneSwitch, matchWrap());
        speakerSwitch = rowSwitch("\u626c\u58f0\u5668\u865a\u62df\u5316");
        speakerSwitch.setOnCheckedChangeListener(toggleListener(2));
        content.addView(speakerSwitch, matchWrap());

        addDivider(content);
        LinearLayout geqHeader = new LinearLayout(this);
        geqHeader.setOrientation(LinearLayout.HORIZONTAL);
        geqHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView geqTitle = text("20 \u6bb5\u56fe\u5f62\u5747\u8861", 18, COLOR_TEXT);
        geqHeader.addView(geqTitle, new LinearLayout.LayoutParams(0, dp(48), 1f));
        Button reset = new Button(this);
        reset.setText("\u6e05\u96f6");
        reset.setTextSize(13);
        reset.setAllCaps(false);
        reset.setMinWidth(0);
        reset.setMinimumWidth(0);
        reset.setBackground(buttonBackground(Color.rgb(243, 244, 246), Color.rgb(209, 213, 219)));
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (service != null) {
                    service.resetGeq();
                }
            }
        });
        geqHeader.addView(reset, new LinearLayout.LayoutParams(dp(64), dp(40)));
        content.addView(geqHeader, matchWrap());

        geqControls = new LinearLayout(this);
        geqControls.setOrientation(LinearLayout.VERTICAL);
        geqSwitch = rowSwitch("\u542f\u7528\u81ea\u5b9a\u4e49\u5747\u8861");
        geqSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!rendering && service != null) {
                    service.setGeqEnabled(isChecked);
                }
            }
        });
        geqControls.addView(geqSwitch, matchWrap());
        geqSelection = text("47 Hz   0 dB", 14, COLOR_MUTED);
        geqSelection.setPadding(0, dp(8), 0, dp(4));
        geqControls.addView(geqSelection, matchWrap());
        geqEditor = new GeqEditorView(this);
        geqEditor.setOnBandChangeListener(new GeqEditorView.OnBandChangeListener() {
            @Override
            public void onBandChanged(int band, int db) {
                geqSelection.setText(
                        GeqEditorView.FREQUENCIES[band] + " Hz   "
                                + (db > 0 ? "+" : "") + db + " dB");
                if (service != null) {
                    service.setGeqBand(band, db);
                }
            }
        });
        geqControls.addView(geqEditor, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(236)));
        content.addView(geqControls, matchWrap());

        setContentView(scroll);
    }

    private CompoundButton.OnCheckedChangeListener toggleListener(final int type) {
        return new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (rendering || service == null) {
                    return;
                }
                if (type == 0) {
                    service.setVolumeLeveler(isChecked);
                } else if (type == 1) {
                    service.setHeadphoneVirtualizer(isChecked);
                } else {
                    service.setSpeakerVirtualizer(isChecked);
                }
            }
        };
    }

    private void render(DolbySnapshot value) {
        rendering = true;
        try {
            statusDot.setTextColor(value.connected && value.hasControl ? COLOR_PRIMARY : COLOR_ERROR);
            if (value.connected && value.hasControl) {
                statusText.setText(value.enabled
                        ? "\u5df2\u8fde\u63a5\uff0c\u5904\u7406\u5f00\u542f"
                        : "\u5df2\u8fde\u63a5\uff0c\u5904\u7406\u5173\u95ed");
            } else {
                statusText.setText(value.lastError.length() == 0
                        ? "\u6b63\u5728\u8fde\u63a5"
                        : value.lastError);
            }
            enabledSwitch.setChecked(value.enabled);
            routeText.setText("\u8f93\u51fa\uff1a" + value.outputRoute);
            tuningText.setText("Tuning\uff1a" + value.tuningStatus
                    + "   \u97f3\u91cf " + value.volume + "/" + value.maxVolume);
            setSegmentSelection(modeButtons, value.mode);
            setSegmentSelection(ieqButtons, ControlValuePolicy.sanitizeIeq(value.ieq));
            dialogSwitch.setChecked(value.dialogEnabled);
            dialogSeek.setProgress(ControlValuePolicy.sanitizeDialogAmount(value.dialogAmount));
            dialogValue.setText(String.valueOf(value.dialogAmount));
            levelerSwitch.setChecked(value.volumeLeveler);
            headphoneSwitch.setChecked(value.headphoneVirtualizer);
            speakerSwitch.setChecked(value.speakerVirtualizer);
            geqSwitch.setChecked(value.geqEnabled);
            geqEditor.setValues(value.geqDb);

            boolean controlsEnabled = value.connected && value.hasControl && value.enabled;
            for (Button button : modeButtons) {
                button.setEnabled(controlsEnabled);
            }
            for (Button button : ieqButtons) {
                button.setEnabled(controlsEnabled);
            }
            dialogSwitch.setEnabled(controlsEnabled);
            dialogSeek.setEnabled(controlsEnabled && value.dialogEnabled);
            levelerSwitch.setEnabled(controlsEnabled);
            headphoneSwitch.setEnabled(controlsEnabled);
            speakerSwitch.setEnabled(controlsEnabled);
            boolean customEnabled = controlsEnabled && ModePolicy.usesCustomGeq(value.mode);
            geqSwitch.setEnabled(customEnabled);
            geqEditor.setEnabled(customEnabled && value.geqEnabled);
            geqControls.setAlpha(customEnabled ? 1f : 0.48f);
        } finally {
            rendering = false;
        }
    }

    private void setSegmentSelection(Button[] buttons, int selected) {
        for (int i = 0; i < buttons.length; i++) {
            boolean active = i == selected;
            buttons[i].setTextColor(active ? Color.WHITE : COLOR_TEXT);
            buttons[i].setBackground(buttonBackground(
                    active ? COLOR_PRIMARY : Color.rgb(243, 244, 246),
                    active ? COLOR_PRIMARY : Color.rgb(209, 213, 219)));
        }
    }

    private void addSectionTitle(LinearLayout parent, String title) {
        TextView view = text(title, 18, COLOR_TEXT);
        view.setPadding(0, 0, 0, dp(10));
        parent.addView(view, matchWrap());
    }

    private void addDivider(LinearLayout parent) {
        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(226, 229, 233));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1));
        params.setMargins(0, dp(20), 0, dp(18));
        parent.addView(divider, params);
    }

    private Switch rowSwitch(String label) {
        Switch control = new Switch(this);
        control.setText(label);
        control.setTextSize(16);
        control.setTextColor(COLOR_TEXT);
        control.setGravity(Gravity.CENTER_VERTICAL);
        control.setMinHeight(dp(48));
        return control;
    }

    private LinearLayout segmentRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private Button segmentButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(4), 0, dp(4), 0);
        return button;
    }

    private GradientDrawable buttonBackground(int fill, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(6));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private TextView text(String value, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        return view;
    }

    private Space space(int width, int height) {
        Space space = new Space(this);
        space.setLayoutParams(new LinearLayout.LayoutParams(width, height));
        return space;
    }

    private LinearLayout.LayoutParams weightedButton() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(44), 1f);
        params.setMargins(dp(2), 0, dp(2), 0);
        return params;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams wrapWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
