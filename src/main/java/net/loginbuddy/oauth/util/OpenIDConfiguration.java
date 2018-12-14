/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.oauth.util;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.logging.Logger;


public class OpenIDConfiguration {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(OpenIDConfiguration.class));

    /**
     * Retrieve the openid-configuration of the given provider
     *
     * @param openidConfigUrl
     */
    public static JSONObject getOpenIDConfiguration(String openidConfigUrl) {

        HttpClient httpClient = HttpClientBuilder.create().build();

        try {
            HttpGet httpGet = new HttpGet(openidConfigUrl);
            HttpResponse response = httpClient.execute(httpGet);

            if (response.getStatusLine().getStatusCode() == 200) {
                // TODO: Handle other status and contentTypes
                return (JSONObject) new JSONParser().parse(EntityUtils.toString(response.getEntity()));
            } else throw new Exception(EntityUtils.toString(response.getEntity()));

        } catch (Exception e) {
            LOGGER.warning("The openid-configuration could not be retrieved. Given URL: '" + openidConfigUrl + "'");
            e.printStackTrace();
        }

        // should never get here
        return null;
    }
}
