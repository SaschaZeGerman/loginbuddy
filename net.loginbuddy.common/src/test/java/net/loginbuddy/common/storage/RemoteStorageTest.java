package net.loginbuddy.common.storage;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class RemoteStorageTest {

    // HAZELCAST[hazelcast1:5701,node1.HAZELCAST.com:5701]
    private static Pattern pAddresses = Pattern.compile("([a-zA-Z\\d.-]{1,64}:\\d{1,5})(?:,([a-zA-Z\\d.-]{1,64}:\\d{1,5}))?");

    @Test
    public void testPatternAddresses() {
        String addresses = "HAZELCAST[hazelcast1:5701]";
        Matcher mAddress = pAddresses.matcher(addresses.toLowerCase());
        if (mAddress.find()) {
            assertEquals(2, mAddress.groupCount());
            assertEquals("hazelcast1:5701", mAddress.group(1));
            assertNull(mAddress.group(2));
        } else {
            fail("Expected to find two groups");
        }
    }

    @Test
    public void testTwoAddresses() {
        String addresses = "HAZELCAST[hazel-cast1:5701,node1.HAZELCAST.com:5701]";
        Matcher mAddress = pAddresses.matcher(addresses);
        int count = 0;
        if (mAddress.find()) {
            assertEquals("hazel-cast1:5701", mAddress.group(1));
            assertEquals("node1.HAZELCAST.com:5701", mAddress.group(2));
        } else {
            fail("Should have found 2 groups");
        }
    }
}
