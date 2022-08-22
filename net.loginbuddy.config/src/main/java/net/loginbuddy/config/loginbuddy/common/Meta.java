package net.loginbuddy.config.loginbuddy.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

public class Meta {

    public final static String STATUS_COMPLETE = "COMPLETE";

    // incomplete, missing required configuration
    public final static String STATUS_INCOMPLETE = "INCOMPLETE";

    // unsupported configuration
    public final static String STATUS_UNSUPPORTED = "UNSUPPORTED";

    // could not register itself (dynamic registration)
    public final static String STATUS_REGISTRATION_ERROR = "REGISTRATION_ERROR";

    // OpenID Connect configuration endpoint could not be reached or had missing values
    public final static String STATUS_OIDC_CONFIG_ERROR = "OIDC_CONFIG_ERROR";

    @JsonProperty("status")
    @JsonIgnore(false)
    private Map<String, List<String>> status;

    public Meta() {
        status = new HashMap<>();
    }

    public Map<String, List<String>> getStatus() {
        return status;
    }

    public void setStatus(Map<String, List<String>> status) {
        this.status = status;
    }

    public void addStatus(String status, String detail) {
        if(this.status.containsKey(status)) {
            this.status.get(status).add(detail);
        } else {
            List<String> details = new ArrayList<>();
            details.add(detail);
            this.status.put(status, details);
        }
    }

}
