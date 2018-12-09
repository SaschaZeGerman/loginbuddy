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
import net.loginbuddy.oauth.util.Pkce;
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
        response.addHeader("Cache-Control", "no-store");
        response.addHeader("Pragma", "no-cache");
        JSONObject resp = new JSONObject();

        String code = request.getParameter("code");

        if(code == null || code.trim().length() == 0 || request.getParameterValues(Constants.CODE.getKey()).length > 1) {
            resp.put("error", "invalid_request");
            resp.put("error_description", "The given code parameter is invalid or was provided multiple times");
            response.setStatus(400);
            response.getWriter().write(resp.toJSONString());
            return;
        }

        Map<String, Object> sessionValues = (Map<String, Object>)LoginbuddyCache.getInstance().remove(code);
        if(sessionValues == null) {
            resp.put("error", "invalid_request");
            resp.put("error_description", "The given code is invalid or has expired");
            response.setStatus(400);
            response.getWriter().write(resp.toJSONString());
            return;
        } else {
            String clientCodeChallenge = (String)sessionValues.get(Constants.CLIENT_CODE_CHALLENGE.getKey());
            if(clientCodeChallenge != null) {
                String clientCodeVerifier = request.getParameter(Constants.CODE_VERIFIER.getKey());
                if(clientCodeVerifier != null && request.getParameterValues(Constants.CODE_VERIFIER.getKey()).length == 1) {
                    if(!Pkce.validate(clientCodeChallenge, (String)sessionValues.get(Constants.CLIENT_CODE_CHALLENGE_METHOD.getKey()), clientCodeVerifier)) {
                        resp.put("error", "invalid_request");
                        resp.put("error_description", "The code_verifier is invalid!");
                        response.setStatus(400);
                        response.getWriter().write(resp.toJSONString());
                        return;
                    }
                } else {
                    resp.put("error", "invalid_request");
                    resp.put("error_description", "The code_verifier parameter is invalid!");
                    response.setStatus(400);
                    response.getWriter().write(resp.toJSONString());
                    return;
                }
            }
            response.getWriter().write((String)sessionValues.get("eb"));
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }
}