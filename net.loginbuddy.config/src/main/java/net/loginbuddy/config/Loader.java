package net.loginbuddy.config;

public interface Loader {

    /**
     * Load the configuration. Each type of configuration extends this interface and provider method to retrieve the loaded configuration
     *
     * @throws Exception
     */
    void load() throws Exception;

    /**
     * Reload the configuration after an update.
     *
     * @throws Exception
     */
    void reload() throws Exception;

    /**
     * This method may be used to override configurations.
     * @param configuration  the configuration to override
     * @param <T> one if these 'Discovery', 'Clients', 'Providers', 'Properties'
     * @return the replaced configuration
     * @throws Exception
     */
    <T> T save(T configuration) throws Exception;

    /**
     * This method may be used to update configurations.
     * @param configuration  the configuration to override
     * @param <T> one if these 'Discovery', 'Clients', 'Providers', 'Properties'
     * @return the replaced configuration
     * @throws Exception
     */
    <T> T update(T configuration) throws Exception;
}
