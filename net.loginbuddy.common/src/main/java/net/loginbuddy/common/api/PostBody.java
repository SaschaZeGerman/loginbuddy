package net.loginbuddy.common.api;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

public class PostBody {

    private List<NameValuePair> formParameters;

    private PostBody() {
        formParameters = new ArrayList<>();
    }

    public static PostBody create() {
        return new PostBody();
    }

    public PostBody addParameter(String param, String value) {
        if(value != null) {
            formParameters.add(new BasicNameValuePair(param, value));
        }
        return this;
    }

    public List<NameValuePair> build() {
        return formParameters;
    }
}
