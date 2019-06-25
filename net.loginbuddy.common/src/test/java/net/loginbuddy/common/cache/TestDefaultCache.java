/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.common.cache;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestDefaultCache {

    private Cache instance;

    @Before
    public void setup() {
        instance = new DefaultCache();
    }

    @After
    public void tearDown() {
        instance = null;
    }

    @Test
    public void testPutStringObject() {
        assertNull("String was not added", instance.put("testkey01", "testvalue01"));
        assertEquals("String was not added", "testvalue01", instance.put("testkey01", "testvalue02"));
        assertEquals("Number of elements is wrong", 1, instance.getSize());
    }

    @Test
    public void testRemoveString() {
        assertNull("String was not removed", instance.remove("testkey01"));
        assertNull("String was not added", instance.put("testkey01", "testvalue01"));
        assertEquals("String was not removed", "testvalue01", instance.remove("testkey01"));
        assertEquals("Number of elements is wrong", 0, instance.getSize());
    }

    @Test
    public void testDeleteString() {
        assertNull("String was not added", instance.put("testkey01", "testvalue01"));
        instance.delete("testkey01");
        assertEquals("Number of elements is wrong", 0, instance.getSize());
    }

    @Test
    public void testGetString() {
        assertNull("String was not added", instance.put("testkey01", "testvalue01"));
        assertNull("String was not added", instance.put("testkey02", "testvalue02"));
        assertNull("String was not added", instance.put("testkey03", "testvalue03"));
        assertEquals("Wrong value found", "testvalue01", instance.get("testkey01"));
        assertEquals("Wrong value found", "testvalue02", instance.get("testkey02"));
    }

    @Test
    public void testFlush() {
        assertNull("String was not added", instance.put("testkey01", "testvalue01"));
        assertNull("String was not added", instance.put("testkey02", "testvalue02"));
        assertNull("String was not added", instance.put("testkey03", "testvalue03"));
        assertEquals("Cahce was not flushed", 3, instance.getSize());
        instance.flush();
        assertEquals("Cahce was not flushed", 0, instance.getSize());
    }
}