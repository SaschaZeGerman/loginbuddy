package net.loginbuddy.demoserver.provider;

import javax.servlet.http.HttpServlet;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class LoginbuddyProviderCommon extends HttpServlet {

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
                e.printStackTrace();
            }
            return new String(Base64.getUrlEncoder().encode(md.digest(ppidSub.getBytes()))).replace("=", "").replace("-", "");
        } else {
            return email;
        }
    }
}
