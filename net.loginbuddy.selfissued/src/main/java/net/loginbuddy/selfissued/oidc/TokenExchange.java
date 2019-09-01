package net.loginbuddy.selfissued.oidc;

import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.selfissued.SelfIssuedMaster;

public class TokenExchange extends SelfIssuedMaster {

  private static Logger LOGGER = Logger.getLogger(String.valueOf(TokenExchange.class));

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

    ParameterValidatorResult targetEndpointResult = ParameterValidator
        .getSingleValue(request.getParameterValues("target"));
    ParameterValidatorResult authCodeResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.CODE.getKey()));
    ParameterValidatorResult clientIdResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.CLIENT_ID.getKey()));
    ParameterValidatorResult clientSecretResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.CLIENT_SECRET.getKey()));
    ParameterValidatorResult redirectUriResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.REDIRECT_URI.getKey()));
    ParameterValidatorResult grantTypeResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.GRANT_TYPE.getKey()));
    ParameterValidatorResult codeVerifierResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.CODE_VERIFIER.getKey()));

    MsgResponse msg= HttpHelper
        .postTokenExchange(
            clientIdResult.getValue(),
            clientSecretResult.getValue(),
            redirectUriResult.getValue(),
            authCodeResult.getValue(),
            targetEndpointResult.getValue(),
            codeVerifierResult.getValue());

    // TODO: validate msg as good as possible
    response.setStatus(msg.getStatus());
    response.setContentType(msg.getContentType());
    response.getWriter().write(msg.getMsg());

  }
}
