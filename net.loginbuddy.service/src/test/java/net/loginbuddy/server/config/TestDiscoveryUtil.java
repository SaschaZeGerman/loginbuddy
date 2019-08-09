package net.loginbuddy.server.config;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import net.loginbuddy.service.config.LoginbuddyConfig;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class TestDiscoveryUtil {

//  private final Context context = mock(Context.class);

//  @Rule
//  public MockInitialContextRule mockInitialContextRule = new MockInitialContextRule(context);



  @BeforeClass
  public static void setupClass() {
    try {


      Hashtable<String, Object> envTable = new Hashtable<>();
      envTable.put("bean/ConfigUtilFactory", net.loginbuddy.service.config.ConfigUtil.class);
      Context envCtx = new InitialContext(envTable);
//      Context envCtx = new InitialContext();
//      envCtx.bind("bean/ConfigUtilFactory", net.loginbuddy.service.config.ConfigUtil.class);

      Hashtable<String, Object> table = new Hashtable<>();
      table.put("java:comp/env", envCtx);
      Context context = new InitialContext(table);

      System.setProperty(Context.INITIAL_CONTEXT_FACTORY, MockInitialContextFactory.class.getName());
      MockInitialContextFactory.setCurrentContext(context);

    } catch (NamingException e) {
      e.printStackTrace();
    }

//    try {
//      table.put("java:comp/env","something");
//      Context initCtx = new InitialContext(table);
//    } catch (NamingException e) {
//      e.printStackTrace();
//    }
  }

  @Before
  public void setup() {
    LoginbuddyConfig.getInstance().getDiscoveryUtil().setPath("src/test/resources/test_discovery.json");
  }

  @Test
  public void testIssuer() {
    try {
      assertEquals("https://{your-domain}", LoginbuddyConfig.getInstance().getDiscoveryUtil().getIssuer());
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testResponseTypesSupported() {
    try {
      assertArrayEquals(new String[]{"code"}, LoginbuddyConfig.getInstance().getDiscoveryUtil().getResponseTypesSupported());
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testGrantTypesSupported() {
    try {
      assertArrayEquals(new String[]{"authorization_code"}, LoginbuddyConfig.getInstance().getDiscoveryUtil().getGrantTypesSupported());
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testTokenEndpointAuthMethodsSupported() {
    try {
      assertArrayEquals(new String[]{"client_secret_post","client_secret_basic"}, LoginbuddyConfig.getInstance().getDiscoveryUtil().getTokenEndpointAuthMethodsSupported());
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testScopesSupported() {
    try {
      assertArrayEquals(new String[]{"openid", "email", "profile"}, LoginbuddyConfig.getInstance().getDiscoveryUtil().getScopesSupported());
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testAuthorizationEndpoint() {
    try {
      assertEquals("https://{your-domain}/authorize", LoginbuddyConfig.getInstance().getDiscoveryUtil().getAuthorizationEndpoint());
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }
}