package net.loginbuddy.service.management;

import jakarta.servlet.http.HttpServlet;
import net.loginbuddy.config.discovery.DiscoveryUtil;

public abstract class Management extends HttpServlet {

    public static boolean isManagementEnabled() {
        return DiscoveryUtil.UTIL.getManagement() != null;
    }

}
