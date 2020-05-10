package net.loginbuddy.service.management;

import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.Jwt;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.service.config.discovery.DiscoveryUtil;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.JwksVerificationKeyResolver;

import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

public class AccessToken {

    private static Logger LOGGER = Logger.getLogger(String.valueOf(AccessToken.class));

    private JwtClaims claims;

    enum TokenLocation {
        HEADER
    }

    public AccessToken(HttpServletRequest requestWithtoken, TokenLocation location) {
        if (TokenLocation.HEADER.equals(location)) {
            ParameterValidatorResult tokenResult = ParameterValidator.getSingleValue(requestWithtoken.getHeaders(Constants.AUTHORIZATION.getKey()));
            if (tokenResult.getResult().equals(ParameterValidatorResult.RESULT.VALID)) {
                String[] headerValue = tokenResult.getValue().split(" ");
                if (headerValue.length == 2 && headerValue[0].equalsIgnoreCase(Constants.BEARER.getKey())) {
                    try {
                        claims = validateAccessToken(
                                headerValue[1],
                                DiscoveryUtil.UTIL.getIssuer(),
                                DiscoveryUtil.UTIL.getIssuer(),
                                Jwt.DEFAULT.getJwksForSigning());
                    } catch (InvalidJwtException e) {
                        LOGGER.severe(e.getMessage());
                        throw new IllegalArgumentException("Invalid access_token!");
                    }
                } else {
                    throw new IllegalArgumentException("Invalid authorization header, it includes an unsupported scheme or otherwise invalid content!");
                }
            } else {
                throw new IllegalArgumentException("Invalid authorization header, no or multiple access_token found!");
            }
        } else {
            throw new IllegalArgumentException("No authorization header found!");
        }
    }

    public String getScope() {
        return claims.getClaimValueAsString(Constants.SCOPE.getKey());
    }

    public String getResource() {
        return claims.getClaimValueAsString(Constants.RESOURCE.getKey());
    }

    private JwtClaims validateAccessToken(String jwtAccessToken, String audience, String issuer, JsonWebKeySet jwks) throws InvalidJwtException {
        return new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setAllowedClockSkewInSeconds(10)
                .setRequireIssuedAt()
                .setRequireSubject()
                .setExpectedAudience(audience)
                .setExpectedIssuer(issuer)
                .setVerificationKeyResolver(new JwksVerificationKeyResolver(jwks.getJsonWebKeys()))
                .setJwsAlgorithmConstraints(
                        AlgorithmConstraints.ConstraintType.WHITELIST,
                        AlgorithmIdentifiers.RSA_USING_SHA256)
                .build()
                .processToClaims(jwtAccessToken);
    }
}