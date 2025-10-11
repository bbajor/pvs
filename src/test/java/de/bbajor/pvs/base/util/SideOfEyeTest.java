package de.bbajor.pvs.base.util;

import org.junit.Test;
import static org.junit.Assert.*;



public class SideOfEyeTest {

    @Test
    public void testByDbString_left() {
        assertEquals(SideOfEye.LEFT, SideOfEye.byDbString("l"));
    }

    @Test
    public void testByDbString_right() {
        assertEquals(SideOfEye.RIGHT, SideOfEye.byDbString("r"));
    }

    @Test
    public void testByDbString_trimmedInput() {
        assertEquals(SideOfEye.LEFT, SideOfEye.byDbString(" l "));
        assertEquals(SideOfEye.RIGHT, SideOfEye.byDbString(" r "));
    }

    @Test
    public void testByDbString_invalid() {
        assertNull(SideOfEye.byDbString("x"));
        assertNull(SideOfEye.byDbString(""));
        assertNull(SideOfEye.byDbString(null));
    }

    @Test
    public void testAsDbString() {
        assertEquals("l", SideOfEye.LEFT.toDbString());
        assertEquals("r", SideOfEye.RIGHT.toDbString());
    }

    @Test
    public void testToString() {
        assertEquals("Linkes Auge", SideOfEye.LEFT.toString());
        assertEquals("Rechtes Auge", SideOfEye.RIGHT.toString());
    }
}