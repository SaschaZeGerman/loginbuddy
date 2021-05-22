package net.loginbuddy.common.cache;

import net.loginbuddy.common.config.Constants;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LoginbuddyContext  implements Serializable  {

  private UUID id;

  private Map<String, Object> context;

  public LoginbuddyContext() {
    this.id = UUID.randomUUID();
    this.context = new HashMap<>();
  }

  public String getId() {
    return id.toString();
  }

  public Object put(String key, Object value) {
    return context.put(key, value);
  }

  public Object remove(String key) {
    return remove(key, Object.class);
  }

  public <T> T remove(String key, Class T) {
    return (T)context.remove(key);
  }

  public Object get(String key) {
    return get(key, Object.class);
  }

  public <T> T get(String key, Class T) {
    return (T)context.get(key);
  }

  public String getString(String key) {
    return get(key, String.class);
  }

  public Boolean getBoolean(String key) {
    return get(key, Boolean.class);
  }
  public Long getLong(String key) { return get(key, Long.class); }

  /**
   *
   * @param responseType the responseType that was used. With that, we know what to expect at the callback
   */
  public void setSessionCallback(Constants responseType) {
    put(Constants.ACTION_EXPECTED.getKey(), Constants.ACTION_CALLBACK.getKey());
    put(Constants.ACTION_USED_RESPONSE_TYPE.getKey(), responseType.getKey());
  }
}
