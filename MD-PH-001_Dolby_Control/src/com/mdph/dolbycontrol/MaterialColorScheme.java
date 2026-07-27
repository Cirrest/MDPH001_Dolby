package com.mdph.dolbycontrol;

final class MaterialColorScheme {
    interface DynamicColorSource {
        int resolve(String name, int fallback);
    }

    final boolean dark;
    final boolean dynamic;
    final int primary;
    final int onPrimary;
    final int primaryContainer;
    final int onPrimaryContainer;
    final int secondary;
    final int onSecondary;
    final int surface;
    final int surfaceContainer;
    final int surfaceContainerHigh;
    final int onSurface;
    final int onSurfaceVariant;
    final int outline;
    final int outlineVariant;
    final int error;
    final int onError;
    final int selection;

    private MaterialColorScheme(
            boolean dark,
            boolean dynamic,
            int primary,
            int onPrimary,
            int primaryContainer,
            int onPrimaryContainer,
            int secondary,
            int onSecondary,
            int surface,
            int surfaceContainer,
            int surfaceContainerHigh,
            int onSurface,
            int onSurfaceVariant,
            int outline,
            int outlineVariant,
            int error,
            int onError,
            int selection) {
        this.dark = dark;
        this.dynamic = dynamic;
        this.primary = primary;
        this.onPrimary = onPrimary;
        this.primaryContainer = primaryContainer;
        this.onPrimaryContainer = onPrimaryContainer;
        this.secondary = secondary;
        this.onSecondary = onSecondary;
        this.surface = surface;
        this.surfaceContainer = surfaceContainer;
        this.surfaceContainerHigh = surfaceContainerHigh;
        this.onSurface = onSurface;
        this.onSurfaceVariant = onSurfaceVariant;
        this.outline = outline;
        this.outlineVariant = outlineVariant;
        this.error = error;
        this.onError = onError;
        this.selection = selection;
    }

    static boolean isDarkMode(int uiMode) {
        return (uiMode & 0x30) == 0x20;
    }

    static MaterialColorScheme fallback(boolean dark) {
        return dark
                ? new MaterialColorScheme(
                        true, false,
                        0xff9bd0c8, 0xff003731,
                        0xff1e514b, 0xffb9eee5,
                        0xffb4ccc8, 0xff1f3532,
                        0xff101413, 0xff181c1b, 0xff222827,
                        0xffe0e3e1, 0xffbec9c6,
                        0xff899390, 0xff3f4947,
                        0xffffb4ab, 0xff690005,
                        0xffffb4ab)
                : new MaterialColorScheme(
                        false, false,
                        0xff006a60, 0xffffffff,
                        0xff9df0e4, 0xff00201c,
                        0xff4a635f, 0xffffffff,
                        0xfff8faf8, 0xffeef2f0, 0xffe8ecea,
                        0xff191c1b, 0xff3f4947,
                        0xff6f7976, 0xffbec9c6,
                        0xffba1a1a, 0xffffffff,
                        0xff006a60);
    }

    static MaterialColorScheme dynamic(boolean dark, DynamicColorSource source) {
        MaterialColorScheme base = fallback(dark);
        if (source == null) {
            return base;
        }
        int primary = source.resolve(
                dark ? "system_accent1_200" : "system_accent1_600", base.primary);
        int onPrimary = source.resolve(
                dark ? "system_accent1_900" : "system_accent1_0", base.onPrimary);
        int primaryContainer = source.resolve(
                dark ? "system_accent1_700" : "system_accent1_100", base.primaryContainer);
        int onPrimaryContainer = source.resolve(
                dark ? "system_accent1_100" : "system_accent1_900", base.onPrimaryContainer);
        int secondary = source.resolve(
                dark ? "system_accent2_200" : "system_accent2_600", base.secondary);
        int onSecondary = source.resolve(
                dark ? "system_accent2_900" : "system_accent2_0", base.onSecondary);
        int surface = source.resolve(
                dark ? "system_neutral1_1000" : "system_neutral1_10", base.surface);
        int surfaceContainer = source.resolve(
                dark ? "system_neutral1_900" : "system_neutral1_50", base.surfaceContainer);
        int surfaceContainerHigh = source.resolve(
                dark ? "system_neutral1_800" : "system_neutral1_100", base.surfaceContainerHigh);
        int onSurface = source.resolve(
                dark ? "system_neutral1_0" : "system_neutral1_900", base.onSurface);
        int onSurfaceVariant = source.resolve(
                dark ? "system_neutral2_200" : "system_neutral2_700", base.onSurfaceVariant);
        int outline = source.resolve(
                dark ? "system_neutral2_400" : "system_neutral2_500", base.outline);
        int outlineVariant = source.resolve(
                dark ? "system_neutral2_800" : "system_neutral2_200", base.outlineVariant);
        return new MaterialColorScheme(
                dark, true, primary, onPrimary, primaryContainer, onPrimaryContainer,
                secondary, onSecondary, surface, surfaceContainer, surfaceContainerHigh,
                onSurface, onSurfaceVariant, outline, outlineVariant,
                base.error, base.onError, primary);
    }

}
