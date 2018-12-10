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
import java.util.*;

public class LoginbuddyCache {

    private Cache cache;

    private static LoginbuddyCache instance;
    private static Map<Long, String> listOfExpirations;


    public static LoginbuddyCache getInstance() {
        if(listOfExpirations == null) {
            listOfExpirations = new HashMap<>();
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
                        cache.delete(listOfExpirations.get(next));
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

    public Object put(String key, Object obj) {
        return putWithExpiration(key, obj, 60L);
    }

    private Object putWithExpiration(String key, Object obj, Long lifetimeInSeconds) {

        if(lifetimeInSeconds == null || lifetimeInSeconds > 3600 || lifetimeInSeconds <=0) {
            lifetimeInSeconds = 60L;
        }

        listOfExpirations.put(new Date().getTime() + lifetimeInSeconds*1000, key);

        return cache.put(key, obj);
    }

    public Object remove(String key) {
        return cache.remove(key);
    }

    public void delete(String key) {
        cache.remove(key);
    }

    public Object get(String key) {
        return cache.get(key);
    }

    public int getSize() {
        return cache.getSize();
    }

    public void flush() {
        cache.flush();
    }
}