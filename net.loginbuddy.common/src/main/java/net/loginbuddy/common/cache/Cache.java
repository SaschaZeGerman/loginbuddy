/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.common.cache;

/**
 * A simple default cache interface.
 */
public interface Cache {

    /**
     * Empty the complete cache
     */
    void flush();

    /**
     * Add a value to the cache
     *
     * @param key
     * @param value
     * @return
     */
    Object put(String key, Object value);

    /**
     * Remove but also return the entry matching the key
     *
     * @param key
     * @return
     */
    Object remove(String key);

    /**
     * Delete the key matching value without returning anything
     *
     * @param key
     */
    void delete(String key);

    /**
     * Get the key matching value
     *
     * @param key
     * @return
     */
    Object get(String key);

    /**
     * The the size, or number of elements within this cache
     *
     * @return
     */
    int getSize();
}
