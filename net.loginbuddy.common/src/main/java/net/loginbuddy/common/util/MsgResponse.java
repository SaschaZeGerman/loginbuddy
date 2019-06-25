/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.common.util;

import java.io.Serializable;

/**
 * The data model that is returned to Loginbuddy after receiving a response from providers.
 */
public class MsgResponse implements Serializable {

    private String contentType, msg;
    private int status;

    public MsgResponse() {
    }

    public MsgResponse(String contentType, String msg, int status) {
        this.contentType = contentType;
        this.msg = msg;
        this.status = status;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return msg;
    }
}
