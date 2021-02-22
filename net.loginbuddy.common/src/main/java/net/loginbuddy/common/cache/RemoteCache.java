package net.loginbuddy.common.cache;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.util.Arrays;

public class RemoteCache implements Cache {

    private HazelcastInstance hz;
    private IMap loginbuddySessions;

    public RemoteCache(String addresses) {

        ClientNetworkConfig netConfig = new ClientNetworkConfig();
        netConfig.setAddresses(Arrays.asList(addresses.split("[,; ]")));

        ClientConfig config = new ClientConfig();
        config.setNetworkConfig(netConfig);

        // Start the Hazelcast Client and connect to an already running Hazelcast Cluster on 127.0.0.1
        this.hz = HazelcastClient.newHazelcastClient(config);
        this.loginbuddySessions = hz.getMap("loginbuddySessions");
    }

    @Override
    public void flush() {
        loginbuddySessions.flush();
    }

    @Override
    public Object put(String key, Object value) {
        return loginbuddySessions.put(key, value);
    }

    @Override
    public Object remove(String key) {
        return loginbuddySessions.remove(key);
    }

    @Override
    public void delete(String key) {
        loginbuddySessions.delete(key);
    }

    @Override
    public Object get(String key) {
        return loginbuddySessions.get(key);
    }

    @Override
    public int getSize() {
        return loginbuddySessions.size();
    }
}
