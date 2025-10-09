package de.bbajor.pvs.base.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;



class BasicEntityTest {

    // Concrete subclass for testing
    static class TestEntity extends BasicEntity<Long> {}

    @Test
    void testGetAndSetId() {
        TestEntity entity = new TestEntity();
        assertNull(entity.getId());
        entity.setId(42L);
        assertEquals(42L, entity.getId());
    }

    @Test
    void testGetAndSetVersion() {
        TestEntity entity = new TestEntity();
        assertEquals(0L, entity.getVersion());
        entity.setVersion(5L);
        assertEquals(5L, entity.getVersion());
    }

    @Test
    void testToString() {
        TestEntity entity = new TestEntity();
        entity.setId(123L);
        String str = entity.toString();
        assertTrue(str.contains("TestEntity"));
        assertTrue(str.contains("123"));
    }

    @Test
    void testEqualsSameInstance() {
        TestEntity entity = new TestEntity();
        assertTrue(entity.equals(entity));
    }

    @Test
    void testEqualsNull() {
        TestEntity entity = new TestEntity();
        assertFalse(entity.equals(null));
    }

    @Test
    void testEqualsDifferentClass() {
        TestEntity entity1 = new TestEntity();
        class OtherEntity extends BasicEntity<Long> {}
        OtherEntity entity2 = new OtherEntity();
        entity1.setId(1L);
        entity2.setId(1L);
        assertFalse(entity1.equals(entity2));
    }

    @Test
    void testEqualsSameId() {
        TestEntity entity1 = new TestEntity();
        TestEntity entity2 = new TestEntity();
        entity1.setId(10L);
        entity2.setId(10L);
        assertTrue(entity1.equals(entity2));
    }

    @Test
    void testEqualsDifferentId() {
        TestEntity entity1 = new TestEntity();
        TestEntity entity2 = new TestEntity();
        entity1.setId(10L);
        entity2.setId(20L);
        assertFalse(entity1.equals(entity2));
    }

    @Test
    void testEqualsNullId() {
        TestEntity entity1 = new TestEntity();
        TestEntity entity2 = new TestEntity();
        entity2.setId(1L);
        assertFalse(entity1.equals(entity2));
    }

    @Test
    void testHashCodeConsistency() {
        TestEntity entity = new TestEntity();
        int hash1 = entity.hashCode();
        int hash2 = entity.hashCode();
        assertEquals(hash1, hash2);
    }
}