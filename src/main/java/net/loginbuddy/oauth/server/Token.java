/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.oauth.server;

import net.loginbuddy.cache.LoginbuddyCache;
import net.loginbuddy.config.Constants;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@WebServlet(name = "Token")
public class Token extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Whatever happens, we'll return JSON
        response.setContentType("application/json");
        JSONObject resp = new JSONObject();

        String code = request.getParameter("code");

        if(code == null || code.trim().length() == 0 || request.getParameterValues("code").length > 1) {
            resp.put("error", "invalid_request");
            resp.put("error_description", "The given code parameter is invalid or was provided multiple times");
            response.sendError(400, resp.toJSONString());
            return;
        }

        String providerResponse = (String)LoginbuddyCache.getInstance().getCache().get(code);
        if(providerResponse == null) {
            resp.put("error", "invalid_request");
            resp.put("error_description", "The given code is invalid or has expired");
            response.sendError(400, resp.toJSONString());
        } else {
            response.getWriter().write(providerResponse);
        }

        // TODO: no matter what, code has to be invalidated once it has been used in this API
        LoginbuddyCache.getInstance().getCache().remove(code);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }
}