/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.provider;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LoginbuddyUserinfo extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.getWriter().println("{\n" +
                " \"sub\": \"1234567890\",\n" +
                " \"name\": \"Login Buddy\",\n" +
                " \"given_name\": \"Login\",\n" +
                " \"family_name\": \"Buddy\",\n" +
                " \"email\": \"admin@loginbuddy.net\",\n" +
                " \"email_verified\": true,\n" +
                " \"gender\": \"male\",\n" +
                " \"locale\": \"en\"\n" +
                "}");


    }

}
