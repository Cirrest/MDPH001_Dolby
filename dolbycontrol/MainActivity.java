package com.mdph.dolbycontrol;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.graphics.Typeface;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Space;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public final class MainActivity extends Activity implements DolbyControlService.Listener {
    private DolbyControlService.LocalBinder service;
    private boolean bound;
    private boolean rendering;
    private UiText uiText;
    private MaterialColorScheme colors;

    private TextView statusDot;
    private TextView statusText;
    private TextView routeText;
    private TextView tuningText;
    private final Button[] themeButtons = new Button[3];
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
        uiText = UiText.forLanguageTag(Locale.getDefault().toLanguageTag());
        colors = createColorScheme();
        applySystemBars();
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
        scroll.setBackgroundColor(colors.surface);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(18), dp(20), dp(32));
        scroll.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = text(uiText.get(UiText.Key.APP_TITLE), 28, colors.onSurface);
        title.setGravity(Gravity.START);
        content.addView(title, matchWrap());

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusRow.setPadding(0, dp(6), 0, dp(18));
        statusDot = text("\u25cf", 14, colors.error);
        statusText = text(uiText.get(UiText.Key.CONNECTING), 14, colors.onSurfaceVariant);
        statusRow.addView(statusDot, wrapWrap());
        statusRow.addView(space(dp(8), 1));
        statusRow.addView(statusText, wrapWrap());
        content.addView(statusRow, matchWrap());

        addSectionTitle(content, uiText.get(UiText.Key.THEME));
        LinearLayout themes = segmentRow();
        String[] themeNames = {
                uiText.get(UiText.Key.THEME_LIGHT),
                uiText.get(UiText.Key.THEME_DARK),
                uiText.get(UiText.Key.THEME_SYSTEM)};
        for (int i = 0; i < themeButtons.length; i++) {
            final int theme = i == 0
                    ? ThemePolicy.THEME_LIGHT
                    : (i == 1 ? ThemePolicy.THEME_DARK : ThemePolicy.THEME_SYSTEM);
            themeButtons[i] = segmentButton(themeNames[i]);
            themeButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getPreferences(MODE_PRIVATE).edit()
                            .putInt("theme", theme).apply();
                    recreate();
                }
            });
            themes.addView(themeButtons[i], weightedButton());
        }
        content.addView(themes, matchWrap());

        enabledSwitch = rowSwitch(uiText.get(UiText.Key.GLOBAL_PROCESSING));
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
        addSectionTitle(content, uiText.get(UiText.Key.MODE));
        LinearLayout modes = segmentRow();
        String[] modeNames = {
                uiText.get(UiText.Key.MODE_DYNAMIC),
                uiText.get(UiText.Key.MODE_MOVIE),
                uiText.get(UiText.Key.MODE_MUSIC),
                uiText.get(UiText.Key.MODE_CUSTOM)};
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

        routeText = text(uiText.get(UiText.Key.OUTPUT) + ": -", 14, colors.onSurface);
        routeText.setPadding(0, dp(14), 0, dp(4));
        content.addView(routeText, matchWrap());
        tuningText = text(uiText.get(UiText.Key.TUNING) + ": -", 13, colors.onSurfaceVariant);
        content.addView(tuningText, matchWrap());

        addDivider(content);
        addSectionTitle(content, uiText.get(UiText.Key.INTELLIGENT_EQUALIZER));
        LinearLayout ieq = segmentRow();
        String[] ieqNames = {
                uiText.get(UiText.Key.OFF),
                uiText.get(UiText.Key.IEQ_BALANCED),
                uiText.get(UiText.Key.IEQ_WARM),
                uiText.get(UiText.Key.IEQ_DETAILED)};
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
        addSectionTitle(content, uiText.get(UiText.Key.DIALOG_ENHANCEMENT));
        dialogSwitch = rowSwitch(uiText.get(UiText.Key.DIALOG_ENHANCEMENT));
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
        dialogValue = text("0", 15, colors.onSurface);
        dialogValue.setGravity(Gravity.CENTER);
        dialogAmountRow.addView(dialogValue, new LinearLayout.LayoutParams(dp(42), dp(42)));
        content.addView(dialogAmountRow, matchWrap());

        addDivider(content);
        addSectionTitle(content, uiText.get(UiText.Key.SOUNDSTAGE));
        levelerSwitch = rowSwitch(uiText.get(UiText.Key.VOLUME_LEVELER));
        levelerSwitch.setOnCheckedChangeListener(toggleListener(0));
        content.addView(levelerSwitch, matchWrap());
        headphoneSwitch = rowSwitch(uiText.get(UiText.Key.HEADPHONE_VIRTUALIZER));
        headphoneSwitch.setOnCheckedChangeListener(toggleListener(1));
        content.addView(headphoneSwitch, matchWrap());
        speakerSwitch = rowSwitch(uiText.get(UiText.Key.SPEAKER_VIRTUALIZER));
        speakerSwitch.setOnCheckedChangeListener(toggleListener(2));
        content.addView(speakerSwitch, matchWrap());

        addDivider(content);
        LinearLayout geqHeader = new LinearLayout(this);
        geqHeader.setOrientation(LinearLayout.HORIZONTAL);
        geqHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView geqTitle = text(uiText.get(UiText.Key.GRAPHIC_EQUALIZER_20), 18, colors.onSurface);
        geqHeader.addView(geqTitle, new LinearLayout.LayoutParams(0, dp(48), 1f));
        Button reset = new Button(this);
        reset.setText(uiText.get(UiText.Key.RESET));
        reset.setTextSize(13);
        reset.setAllCaps(false);
        reset.setMinWidth(0);
        reset.setMinimumWidth(0);
        reset.setTextColor(colors.onSurface);
        reset.setBackground(buttonBackground(colors.surfaceContainerHigh, colors.outlineVariant));
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
        geqSwitch = rowSwitch(uiText.get(UiText.Key.ENABLE_CUSTOM_EQUALIZER));
        geqSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!rendering && service != null) {
                    service.setGeqEnabled(isChecked);
                }
            }
        });
        geqControls.addView(geqSwitch, matchWrap());
        geqSelection = text("47 Hz   0 dB", 14, colors.onSurfaceVariant);
        geqSelection.setPadding(0, dp(8), 0, dp(4));
        geqControls.addView(geqSelection, matchWrap());
        geqEditor = new GeqEditorView(this);
        geqEditor.setColorScheme(colors);
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

        addDivider(content);
        addSectionTitle(content, uiText.get(UiText.Key.MAINTENANCE));
        final Button restartAudio = new Button(this);
        restartAudio.setText(uiText.get(UiText.Key.RESTART_AUDIO_SERVICE));
        restartAudio.setTextSize(15);
        restartAudio.setAllCaps(false);
        restartAudio.setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.ic_popup_sync, 0, 0, 0);
        restartAudio.setCompoundDrawablePadding(dp(8));
        restartAudio.setTextColor(colors.onSurface);
        restartAudio.setBackground(buttonBackground(
                colors.surfaceContainerHigh, colors.outlineVariant));
        restartAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (service == null) {
                    return;
                }
                service.restartAudioService();
                restartAudio.setEnabled(false);
                restartAudio.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        restartAudio.setEnabled(true);
                    }
                }, 3000L);
                Toast.makeText(
                        MainActivity.this,
                        uiText.get(UiText.Key.RESTART_REQUESTED),
                        Toast.LENGTH_SHORT).show();
            }
        });
        content.addView(restartAudio, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)));

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
            statusDot.setTextColor(value.released || (value.connected && value.hasControl)
                    ? statusConnected() : statusDisconnected());
            if (value.released) {
                statusText.setText(uiText.get(UiText.Key.PROCESSING_RELEASED));
            } else if (value.connected && value.hasControl) {
                statusText.setText(value.enabled
                        ? uiText.get(UiText.Key.CONNECTED_ENABLED)
                        : uiText.get(UiText.Key.CONNECTED_DISABLED));
            } else {
                statusText.setText(value.lastError.length() == 0
                        ? uiText.get(UiText.Key.CONNECTING)
                        : value.lastError);
            }
            enabledSwitch.setChecked(value.enabled);
            routeText.setText(uiText.get(UiText.Key.OUTPUT) + ": " + value.outputRoute);
            tuningText.setText(uiText.get(UiText.Key.TUNING) + ": " + value.tuningStatus
                    + "   " + uiText.get(UiText.Key.VOLUME) + " "
                    + value.volume + "/" + value.maxVolume);
            setSegmentSelection(modeButtons, value.mode);
            setSegmentSelection(ieqButtons, ControlValuePolicy.sanitizeIeq(value.ieq));
            int selectedTheme = ThemePolicy.sanitize(getPreferences(MODE_PRIVATE)
                    .getInt("theme", ThemePolicy.THEME_SYSTEM));
            setSegmentSelection(themeButtons,
                    selectedTheme == ThemePolicy.THEME_SYSTEM ? 2 : selectedTheme - 1);
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
            buttons[i].setTextColor(active ? colors.onPrimaryContainer : colors.onSurface);
            buttons[i].setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
            buttons[i].setBackground(buttonBackground(
                    active ? colors.primaryContainer : colors.surfaceContainerHigh,
                    active ? colors.primary : colors.outlineVariant));
        }
    }

    private void addSectionTitle(LinearLayout parent, String title) {
        TextView view = text(title, 18, colors.onSurface);
        view.setPadding(0, 0, 0, dp(10));
        parent.addView(view, matchWrap());
    }

    private void addDivider(LinearLayout parent) {
        View divider = new View(this);
        divider.setBackgroundColor(colors.outlineVariant);
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
        control.setTextColor(colors.onSurface);
        control.setButtonTintList(switchTintList());
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
        button.setMinHeight(dp(48));
        button.setStateListAnimator(null);
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

    private MaterialColorScheme createColorScheme() {
        boolean systemDark = MaterialColorScheme.isDarkMode(
                getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK);
        boolean dark = ThemePolicy.isDark(
                getPreferences(MODE_PRIVATE).getInt("theme", ThemePolicy.THEME_SYSTEM), systemDark);
        if (Build.VERSION.SDK_INT < 31) {
            return MaterialColorScheme.fallback(dark);
        }
        return MaterialColorScheme.dynamic(dark, new MaterialColorScheme.DynamicColorSource() {
            @Override
            public int resolve(String name, int fallback) {
                try {
                    int id = getResources().getIdentifier(name, "color", "android");
                    return id == 0 ? fallback : getColor(id);
                } catch (RuntimeException ignored) {
                    return fallback;
                }
            }
        });
    }

    private void applySystemBars() {
        getWindow().setStatusBarColor(colors.surface);
        getWindow().setNavigationBarColor(colors.surface);
        int flags = getWindow().getDecorView().getSystemUiVisibility();
        if (colors.dark) {
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        } else {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    private ColorStateList switchTintList() {
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] {}
        };
        int[] values = new int[] { colors.primary, colors.outline };
        return new ColorStateList(states, values);
    }

    private int statusConnected() {
        return colors.dark ? 0xff81c784 : 0xff2e7d32;
    }

    private int statusDisconnected() {
        return colors.dark ? 0xffef9a9a : 0xffc62828;
    }
}
