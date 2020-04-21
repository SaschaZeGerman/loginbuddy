package net.loginbuddy.service.config.internal.scope;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class ScopeWriteDeserializer extends JsonDeserializer<Write> {

    @Override
    public Write deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectCodec oc = jp.getCodec();
        JsonNode node = oc.readTree(jp);
        Write parent = new Write();

        Clients c = new Clients();
        c.setParent(parent);
        c.setDescription(node.get("clients").get("description").textValue());

        Providers p = new Providers();
        p.setParent(parent);
        p.setDescription(node.get("providers").get("description").textValue());

        Discovery d = new Discovery();
        d.setParent(parent);
        d.setDescription(node.get("discovery").get("description").textValue());

        Properties props = new Properties();
        props.setParent(parent);
        props.setDescription(node.get("properties").get("description").textValue());

        parent.setClients(c);
        parent.setProviders(p);
        parent.setDiscovery(d);
        parent.setProperties(props);

        return parent;
    }

}