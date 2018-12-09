/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.cache;

public interface Cache {

    void flush();

    Object put(String key, Object value);

    Object remove(String key);

    void delete(String key);

    Object get(String key);

    int getSize();
}
