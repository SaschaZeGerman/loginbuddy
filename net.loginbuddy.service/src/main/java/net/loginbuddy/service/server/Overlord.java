package net.loginbuddy.service.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.config.loginbuddy.OverlordMaster;

public class Overlord extends HttpServlet {

    @Override
    public void init() throws ServletException {
        super.init();
        new OverlordMaster().initializeConfiguration();
    }
}