package com.mdph.dolbycontrol;

public final class MaterialColorSchemeTest {
    public static void main(String[] args) {
        assertFalse(MaterialColorScheme.isDarkMode(0x10), "light uiMode");
        assertTrue(MaterialColorScheme.isDarkMode(0x20), "dark uiMode");

        MaterialColorScheme light = MaterialColorScheme.fallback(false);
        MaterialColorScheme dark = MaterialColorScheme.fallback(true);
        assertFalse(light.dark, "light scheme flag");
        assertTrue(dark.dark, "dark scheme flag");
        assertOpaque(light.primary, "light primary");
        assertOpaque(light.surface, "light surface");
        assertOpaque(dark.primary, "dark primary");
        assertOpaque(dark.surface, "dark surface");
        assertNotEquals(light.surface, dark.surface, "light and dark surface");
        assertNotEquals(light.onSurface, dark.onSurface, "light and dark text");

        MaterialColorScheme dynamicLight = MaterialColorScheme.dynamic(
                false,
                new MaterialColorScheme.DynamicColorSource() {
                    @Override
                    public int resolve(String name, int fallback) {
                        return "system_accent1_600".equals(name) ? 0xff123456 : fallback;
                    }
                });
        assertTrue(dynamicLight.dynamic, "dynamic light flag");
        assertEquals(0xff123456, dynamicLight.primary, "dynamic light primary");

        MaterialColorScheme dynamicDark = MaterialColorScheme.dynamic(
                true,
                new MaterialColorScheme.DynamicColorSource() {
                    @Override
                    public int resolve(String name, int fallback) {
                        return "system_accent1_200".equals(name) ? 0xffabcdef : fallback;
                    }
                });
        assertTrue(dynamicDark.dynamic, "dynamic dark flag");
        assertEquals(0xffabcdef, dynamicDark.primary, "dynamic dark primary");

        MaterialColorScheme dynamicSurfaceLight = MaterialColorScheme.dynamic(
                false, MaterialColorSchemeTest::resolveNeutralColor);
        assertEquals(0xfffdfdfd, dynamicSurfaceLight.surface, "dynamic light surface");
        assertEquals(0xfff5f5f5, dynamicSurfaceLight.surfaceContainer,
                "dynamic light surface container");
        assertEquals(0xffe8e8e8, dynamicSurfaceLight.surfaceContainerHigh,
                "dynamic light high surface container");
        assertEquals(0xff202020, dynamicSurfaceLight.onSurface, "dynamic light text");

        MaterialColorScheme dynamicSurfaceDark = MaterialColorScheme.dynamic(
                true, MaterialColorSchemeTest::resolveNeutralColor);
        assertEquals(0xff050505, dynamicSurfaceDark.surface, "dynamic dark surface");
        assertEquals(0xff202020, dynamicSurfaceDark.surfaceContainer,
                "dynamic dark surface container");
        assertEquals(0xff383838, dynamicSurfaceDark.surfaceContainerHigh,
                "dynamic dark high surface container");
        assertEquals(0xffffffff, dynamicSurfaceDark.onSurface, "dynamic dark text");

        System.out.println("MaterialColorSchemeTest PASS");
    }

    private static int resolveNeutralColor(String name, int fallback) {
        if ("system_neutral1_0".equals(name)) return 0xffffffff;
        if ("system_neutral1_10".equals(name)) return 0xfffdfdfd;
        if ("system_neutral1_50".equals(name)) return 0xfff5f5f5;
        if ("system_neutral1_100".equals(name)) return 0xffe8e8e8;
        if ("system_neutral1_800".equals(name)) return 0xff383838;
        if ("system_neutral1_900".equals(name)) return 0xff202020;
        if ("system_neutral1_1000".equals(name)) return 0xff050505;
        return fallback;
    }

    private static void assertOpaque(int color, String label) {
        if ((color >>> 24) != 0xff) {
            throw new AssertionError(label + " must be opaque: 0x" + Integer.toHexString(color));
        }
    }

    private static void assertEquals(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + " expected 0x" + Integer.toHexString(expected)
                    + " but was 0x" + Integer.toHexString(actual));
        }
    }

    private static void assertNotEquals(int first, int second, String label) {
        if (first == second) {
            throw new AssertionError(label + " must differ");
        }
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label + " must be true");
        }
    }

    private static void assertFalse(boolean value, String label) {
        if (value) {
            throw new AssertionError(label + " must be false");
        }
    }
}
