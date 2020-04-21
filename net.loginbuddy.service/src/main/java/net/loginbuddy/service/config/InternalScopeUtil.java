package net.loginbuddy.service.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.service.config.internal.scope.*;

import java.io.File;
import java.util.logging.Logger;

public class InternalScopeUtil implements Bootstrap {

    private Logger LOGGER = Logger.getLogger(String.valueOf(InternalScopeUtil.class));

    private com.fasterxml.jackson.databind.ObjectMapper MAPPER = new ObjectMapper();

    private String path;

    public InternalScopeUtil() {
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    private InternalScopeConfig getConfig() {
        try {
            InternalScopeConfig config = (InternalScopeConfig) LoginbuddyCache.getInstance().get("InternalScopeConfig");
            if (config == null) {
                SimpleModule module = new SimpleModule();
                module.addDeserializer(Read.class, new ScopeReadDeserializer());
                module.addDeserializer(Write.class, new ScopeWriteDeserializer());
                MAPPER.registerModule(module);
                JsonNode node = MAPPER.readValue(new File(this.path).getAbsoluteFile(), JsonNode.class);
                config = MAPPER.readValue(node.toString(), InternalScopeConfig.class);
                LoginbuddyCache.getInstance().put("InternalScopeConfig", config);
            }
            return config;
        } catch (Exception e) {
            LOGGER.severe("internalScopes.json file could not be loaded or it is invalid JSON! Exiting!");
        }
        return null; // we should never get here
    }

    @Override
    public boolean isConfigured() {
        return getConfig() != null;
    }
}
