package net.loginbuddy.service.management;

import net.loginbuddy.config.discovery.DiscoveryUtil;

import jakarta.servlet.http.HttpServlet;

public abstract class Management extends HttpServlet {

    public static boolean isManagementEnabled() {
        return DiscoveryUtil.UTIL.getManagement() != null;
    }

}
