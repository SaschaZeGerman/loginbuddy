/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.cache;

import javax.naming.Context;
import javax.naming.InitialContext;

public class LoginbuddyCache {

    private Cache cache;

    private static LoginbuddyCache instance = new LoginbuddyCache();

    public static LoginbuddyCache getInstance() { return instance; }

    private LoginbuddyCache() {
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            cache = (Cache) envCtx.lookup("bean/CacheFactory");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Cache getCache() {
        return cache;
    }

}