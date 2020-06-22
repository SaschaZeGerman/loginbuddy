package net.loginbuddy.config.loginbuddy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.loginbuddy.config.Bootstrap;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Loginbuddy implements Bootstrap, Cloneable {

    @JsonProperty("clients")
    @JsonIgnore(false)
    private List<Clients> clients;

    @JsonProperty("providers")
    @JsonIgnore(false)
    private List<Providers> providers;

    public Loginbuddy() {
        clients = new ArrayList<>();
        providers = new ArrayList<>();
    }

    public List<Clients> getClients() {
        return clients;
    }

    public void setClients(List<Clients> clients) {
        this.clients = clients;
    }

    public List<Providers> getProviders() {
        return providers;
    }

    public void setProviders(List<Providers> providers) {
        this.providers = providers;
    }

    @Override
    @JsonIgnore
    public boolean isConfigured() {
        return clients != null && providers != null;
    }

    /**
     * All providers found in the given template will be updated with values found in the current providers configuration.
     *
     * The current configuration will be updated only if it is referencing a template (template=(templateProvider}.
     *
     * @param configTemplate
     */
//    public void assimilateProviders(Loginbuddy configTemplate) {
//        if(configTemplate != null) {
//            Providers pTemplate;
//            List<Providers> pResult = new ArrayList<>();
//            for (Providers p : providers) {
//                if (p.getTemplate() != null) {
//                    pTemplate = configTemplate.getProviders().stream()
//                            .filter(provider -> provider.getProvider().equalsIgnoreCase(p.getTemplate()))
//                            .findFirst()
//                            .orElse(null);
//                    if (pTemplate != null) {
//                        if (p.getProvider() != null) pTemplate.setProvider(p.getProvider());
//                        if (p.getIssuer() != null) pTemplate.setIssuer(p.getIssuer());
//                        if (p.getClientId() != null) pTemplate.setClientId(p.getClientId());
//                        if (p.getClientSecret() != null) pTemplate.setClientSecret(p.getClientSecret());
//                        if (p.getAuthorizationEndpoint() != null)
//                            pTemplate.setAuthorizationEndpoint(p.getAuthorizationEndpoint());
//                        if (p.getTokenEndpoint() != null) pTemplate.setTokenEndpoint(p.getTokenEndpoint());
//                        if (p.getUserinfoEndpoint() != null) pTemplate.setUserinfoEndpoint(p.getUserinfoEndpoint());
//                        if (p.getJwksUri() != null) pTemplate.setJwksUri(p.getJwksUri());
//                        if (p.getScope() != null) pTemplate.setScope(p.getScope());
//                        if (p.getResponseType() != null) pTemplate.setResponseType(p.getResponseType());
//                        if (p.getRedirectUri() != null) pTemplate.setRedirectUri(p.getRedirectUri());
//                        if (p.mappingsAsJsonNode() != null) pTemplate.setMappings(p.mappingsAsJsonNode());
//                        if (p.getResponseMode() != null) pTemplate.setResponseMode(p.getResponseMode());
//                        if (p.getOpenidConfigurationUri() != null)
//                            pTemplate.setOpenidConfigurationUri(p.getOpenidConfigurationUri());
//                        pTemplate.setPkce(p.getPkce());
//                        pResult.add(pTemplate);
//                    }
//                } else {
//                    pResult.add(p);
//                }
//            }
//            providers = pResult;
//        }
//    }
}