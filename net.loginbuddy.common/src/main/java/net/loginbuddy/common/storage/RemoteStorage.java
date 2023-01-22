package net.loginbuddy.common.storage;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.core.HazelcastInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RemoteStorage extends LocalStorage {

    private final Pattern pAddresses = Pattern.compile("([a-zA-Z\\d.-]{1,64}:\\d{1,5})(?:,([a-zA-Z\\d.-]{1,64}:\\d{1,5}))?");
    private HazelcastInstance hz;

    /**
     * @param addresses HAZELCAST[host1:port,host2:port]
     */
    public RemoteStorage(String addresses) {
        super();
        ClientNetworkConfig netConfig = new ClientNetworkConfig();

        Matcher mAddress = pAddresses.matcher(addresses.toLowerCase());
        if (mAddress.find()) {

            List<String> addressList = new ArrayList<>();
            if (mAddress.group(1) != null) {
                addressList.add(mAddress.group(1));
            }
            if (mAddress.group(2) != null) {
                addressList.add(mAddress.group(2));
            }
            netConfig.setAddresses(addressList);

            ClientConfig config = new ClientConfig();
            config.setNetworkConfig(netConfig);

            // Start the Hazelcast Client and connect to an already running Hazelcast Cluster on 127.0.0.1
            this.hz = HazelcastClient.newHazelcastClient(config);
            setLoginbuddyStorage(hz.getMap(getStorageName()));
        }
    }
}