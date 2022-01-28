package net.loginbuddy.demoserver.provider;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Logger;

public abstract class LoginbuddyProviderCommon extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(LoginbuddyProviderCommon.class));

    protected String scheme;
    protected String hostname;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        scheme = System.getenv("SCHEME_LOGINBUDDY_DEMOSERVER");
        if("http".equalsIgnoreCase(scheme)) {
            scheme = "http://";
            LOGGER.warning("Loginbuddy Demoserver is using http!");
        } else {
            scheme = "https://";
        }
        hostname = System.getenv("HOSTNAME_LOGINBUDDY_DEMOSERVER");
    }

    /**
     * Get the 'sub' value. Either plain or a PPID
     *
     * @param clientId
     * @param email
     * @param ppid
     * @return
     */
    protected String getSub(String clientId, String email, boolean ppid) {
        if (ppid) {
            // Create a fake PPID to be used with 'sub'
            String ppidSub = "fakeProviderSalt".concat(clientId).concat(email);
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                // should never happen
                e.printStackTrace();
            }
            return new String(Base64.getUrlEncoder().encode(md.digest(ppidSub.getBytes()))).replace("=", "").replace("-", "");
        } else {
            return email;
        }
    }
}
