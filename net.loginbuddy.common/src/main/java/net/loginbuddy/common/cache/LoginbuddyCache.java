/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.common.cache;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class LoginbuddyCache {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(LoginbuddyCache.class));

    private Cache cache;

    private static LoginbuddyCache instance;
    private static Map<Long, String> listOfExpirations;


    public static LoginbuddyCache getInstance() {
        if(listOfExpirations == null) {
            listOfExpirations = new ConcurrentHashMap<>();
        }
        if(instance == null) {
            instance = new LoginbuddyCache();
        }
        return instance;
    }

    private LoginbuddyCache() {
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            cache = (DefaultCache) envCtx.lookup("bean/CacheFactory");
            removeExpiredEntries();
        } catch (Exception e) {
            LOGGER.severe("LoginbuddyCache could not be loaded!");
            e.printStackTrace();
        }
    }

    private void removeExpiredEntries() {
        TimerTask repeatedTask = new TimerTask() {
            public void run() {
                long now = new Date().getTime();
                List<Long> expired = new ArrayList<>();
                for (long next : listOfExpirations.keySet()) {
                    if (next < now) {
                        delete(listOfExpirations.get(next));
                        expired.add(next);
                    }
                }
                listOfExpirations.keySet().removeAll(expired);
                expired = null;
            }
        };
        Timer timer = new Timer("LoginbuddyCacheMaintainerTimer");
        long delay  = 1000L;
        long period = 30000L;
        timer.scheduleAtFixedRate(repeatedTask, delay, period);
    }

    /**
     * Any value added has a default lifetime of 120. For other lifetimes, use {@link #put(String, Object, Long)}.
     *
     * @param key
     * @param obj
     * @return
     */
    public Object put(String key, Object obj) {
        return put(key, obj, 120L);
    }

    /**
     * Any added value has a lifetime matching the given value. The max. lifetime is 3600s.
     *
     * @param key
     * @param obj
     * @param lifetimeInSeconds for 'null' the lifetime is set to 120
     * @return
     */
    public Object put(String key, Object obj, Long lifetimeInSeconds) {

        if(lifetimeInSeconds == null || lifetimeInSeconds > 3600 || lifetimeInSeconds <=0) {
            lifetimeInSeconds = 120L;
        }

        listOfExpirations.put(new Date().getTime() + lifetimeInSeconds*1000, key);

        return cache.put(key, obj);
    }

    // TODO: do not return expired values
    public Object remove(String key) {
        return cache.remove(key);
    }

    public void delete(String key) {
        remove(key);
    }

    // TODO: do not return expired values
    public Object get(String key) {
        return cache.get(key);
    }

    // TODO: do not include expired values
    public int getSize() {
        return cache.getSize();
    }

    public void flush() {
        cache.flush();
    }
}