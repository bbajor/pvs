package de.bbajor.pvs.base.util;

import org.junit.Test;
import static org.junit.Assert.*;



public class PhoneUtilsTest {

    @Test
    public void testNullInput() {
        assertEquals("", PhoneUtils.formatPhoneNumber(null));
    }

    @Test
    public void testEmptyInput() {
        assertEquals("", PhoneUtils.formatPhoneNumber(""));
        assertEquals("", PhoneUtils.formatPhoneNumber("   "));
    }

    @Test
    public void testGermanNumberWithZero() {
        assertEquals("+49123456789", PhoneUtils.formatPhoneNumber("0123456789"));
        assertEquals("+49123456789", PhoneUtils.formatPhoneNumber("0 123 456 789"));
        assertEquals("+49123456789", PhoneUtils.formatPhoneNumber("0-123-456-789"));
    }

    @Test
    public void testNumberWithCountryCode() {
        assertEquals("+49123456789", PhoneUtils.formatPhoneNumber("+49123456789"));
        assertEquals("+49123456789", PhoneUtils.formatPhoneNumber("+49 123 456 789"));
        assertEquals("+49123456789", PhoneUtils.formatPhoneNumber("+49-123-456-789"));
    }

    @Test
    public void testNumberWithoutZeroOrPlus() {
        assertEquals("+49123456789", PhoneUtils.formatPhoneNumber("123456789"));
        assertEquals("+49123456789", PhoneUtils.formatPhoneNumber("123 456 789"));
    }

    @Test
    public void testNumberWithSpecialCharacters() {
        assertEquals("+49123456789", PhoneUtils.formatPhoneNumber("(0)123-456/789"));
        assertEquals("+49123456789", PhoneUtils.formatPhoneNumber("0.123.456.789"));
    }

    @Test
    public void testShortNumber() {
        assertEquals("+49123", PhoneUtils.formatPhoneNumber("0123"));
        assertEquals("+49123", PhoneUtils.formatPhoneNumber("123"));
    }
}