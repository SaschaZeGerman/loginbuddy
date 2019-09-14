package net.loginbuddy.common.api;

import org.json.simple.JSONArray;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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