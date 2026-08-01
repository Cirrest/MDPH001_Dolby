package com.mdph.dolbycontrol;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

final class GeqEditorView extends View {
    interface OnBandChangeListener {
        void onBandChanged(int band, int db);
    }

    static final int[] FREQUENCIES = {
            47, 141, 234, 328, 469,
            656, 844, 1031, 1313, 1688,
            2250, 3000, 3750, 4688, 5813,
            7125, 9000, 11250, 13875, 19688
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] values = new int[GeqGainMapper.BAND_COUNT];
    private MaterialColorScheme colors;
    private OnBandChangeListener listener;
    private int selectedBand = -1;

    GeqEditorView(Context context) {
        super(context);
        init();
    }

    GeqEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    void setOnBandChangeListener(OnBandChangeListener listener) {
        this.listener = listener;
    }

    void setColorScheme(MaterialColorScheme colors) {
        this.colors = colors;
        invalidate();
    }

    void setValues(int[] newValues) {
        if (newValues == null || newValues.length != values.length) {
            return;
        }
        for (int i = 0; i < values.length; i++) {
            values[i] = GeqGainMapper.sanitizeDb(newValues[i]);
        }
        invalidate();
    }

    int getSelectedBand() {
        return selectedBand;
    }

    int getSelectedDb() {
        return selectedBand < 0 ? 0 : values[selectedBand];
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float left = 8f * density;
        float right = getWidth() - 8f * density;
        float top = 12f * density;
        float bottom = getHeight() - 30f * density;
        float plotHeight = bottom - top;
        float zeroY = top + plotHeight / 2f;
        float bandWidth = (right - left) / values.length;

        MaterialColorScheme scheme = colors == null
                ? MaterialColorScheme.fallback(false)
                : colors;
        paint.setColor(scheme.surfaceContainer);
        canvas.drawRoundRect(new RectF(0, 0, getWidth(), getHeight()),
                6f * density, 6f * density, paint);

        paint.setStrokeWidth(density);
        for (int db = -10; db <= 10; db += 5) {
            float y = dbToY(db, top, bottom);
            paint.setColor(db == 0 ? scheme.outline : scheme.outlineVariant);
            canvas.drawLine(left, y, right, y, paint);
        }

        for (int i = 0; i < values.length; i++) {
            float center = left + bandWidth * (i + 0.5f);
            float barHalf = Math.max(2f * density, bandWidth * 0.27f);
            float valueY = dbToY(values[i], top, bottom);
            paint.setColor(values[i] >= 0
                    ? scheme.primary
                    : scheme.error);
            float barTop = Math.min(zeroY, valueY);
            float barBottom = Math.max(zeroY, valueY);
            if (Math.abs(barBottom - barTop) < density) {
                barTop = zeroY - density;
                barBottom = zeroY + density;
            }
            canvas.drawRoundRect(
                    new RectF(center - barHalf, barTop, center + barHalf, barBottom),
                    2f * density,
                    2f * density,
                    paint);

            if (i == selectedBand) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2f * density);
                paint.setColor(scheme.secondary);
                canvas.drawRoundRect(
                        new RectF(
                                center - bandWidth * 0.44f,
                                top,
                                center + bandWidth * 0.44f,
                                bottom),
                        3f * density,
                        3f * density,
                        paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }

        paint.setTextSize(9f * getResources().getDisplayMetrics().scaledDensity);
        paint.setColor(scheme.onSurfaceVariant);
        paint.setTextAlign(Paint.Align.CENTER);
        int[] labels = {0, 4, 8, 12, 16, 19};
        for (int index : labels) {
            float center = left + bandWidth * (index + 0.5f);
            canvas.drawText(formatFrequency(FREQUENCIES[index]), center, getHeight() - 10f * density, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                || event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            updateFromTouch(event.getX(), event.getY());
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            performClick();
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void init() {
        setMinimumHeight(dp(236));
        setFocusable(true);
        setContentDescription("20 band graphic equalizer");
    }

    private void updateFromTouch(float x, float y) {
        float density = getResources().getDisplayMetrics().density;
        float left = 8f * density;
        float right = getWidth() - 8f * density;
        float top = 12f * density;
        float bottom = getHeight() - 30f * density;
        float safeX = Math.max(left, Math.min(right - 1f, x));
        int band = (int) ((safeX - left) * values.length / (right - left));
        band = Math.max(0, Math.min(values.length - 1, band));
        float safeY = Math.max(top, Math.min(bottom, y));
        int db = Math.round(10f - (safeY - top) * 20f / (bottom - top));
        db = GeqGainMapper.sanitizeDb(db);
        selectedBand = band;
        if (values[band] != db) {
            values[band] = db;
            if (listener != null) {
                listener.onBandChanged(band, db);
            }
        }
        invalidate();
    }

    private float dbToY(int db, float top, float bottom) {
        return top + (10f - db) * (bottom - top) / 20f;
    }

    private static String formatFrequency(int frequency) {
        if (frequency >= 1000) {
            float value = frequency / 1000f;
            if (frequency % 1000 == 0) {
                return ((int) value) + "k";
            }
            return String.format(java.util.Locale.US, "%.1fk", value);
        }
        return String.valueOf(frequency);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
