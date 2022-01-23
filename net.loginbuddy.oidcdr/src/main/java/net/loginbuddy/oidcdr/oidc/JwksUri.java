package net.loginbuddy.oidcdr.oidc;

import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.oidcdr.OIDCDRMaster;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

public class JwksUri  extends OIDCDRMaster {

  private static Logger LOGGER = Logger.getLogger(String.valueOf(JwksUri.class));

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    ParameterValidatorResult targetEndpointResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.TARGET_PROVIDER.getKey()));

    MsgResponse msg= HttpHelper.getAPI(targetEndpointResult.getValue());

    // TODO: validate msg as good as possible
    response.setStatus(msg.getStatus());
    response.setContentType(msg.getContentType());
    response.getWriter().write(msg.getMsg());

  }

}
