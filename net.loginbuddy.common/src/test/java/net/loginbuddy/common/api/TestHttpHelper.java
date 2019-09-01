package net.loginbuddy.common.api;

import static org.junit.Assert.assertEquals;

import net.loginbuddy.common.config.Constants;
import org.json.simple.JSONArray;
import org.junit.Test;

public class TestHttpHelper {

  @Test
  public void testJsonArrayToString() {
    JSONArray array = new JSONArray();
    array.add("first");
    array.add("second");
    array.add("third");

    assertEquals("first second third", HttpHelper.jsonArrayToString(array) );
  }

}