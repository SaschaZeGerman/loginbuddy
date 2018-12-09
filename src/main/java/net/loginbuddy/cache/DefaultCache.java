/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.cache;

import java.util.HashMap;
import java.util.Map;

public class DefaultCache implements Cache {

    private Map<String, Object> cache;

    public DefaultCache() {
        cache = new HashMap<>();
    }

    public void flush() {
        cache = new HashMap<>();
    }

    public Object put(String key, Object value) {
        return cache.put(key,value);
    }

    public Object remove(String key) {
        return cache.remove(key);
    }

    public void delete(String key) {
        cache.remove(key);
    }

    public Object get(String key)
    {
        return cache.get(key);
    }

    public int getSize() {
        return cache.size();
    }
}