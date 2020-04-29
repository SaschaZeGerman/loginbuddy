package net.loginbuddy.service.config;

import net.loginbuddy.service.management.LoginbuddyScope;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestLoginbuddyScope {

    @Test
    public void testValidateManagement() {
        assertTrue(LoginbuddyScope.Management.isScopeValid("management"));
        assertFalse(LoginbuddyScope.Management.isScopeValid("Management"));
        assertFalse(LoginbuddyScope.Management.isScopeValid(null));
        assertFalse(LoginbuddyScope.Management.isScopeValid("management.configuration.read.clients"));
    }

    @Test
    public void testValidateWrite() {
        assertFalse(LoginbuddyScope.WriteClients.isScopeValid("management.configuration.read.clients"));
        assertTrue(LoginbuddyScope.WriteClients.isScopeValid("management.configuration"));
        assertFalse(LoginbuddyScope.WriteClients.isScopeValid("configuration.write.clients"));
    }

    @Test
    public void testValidateMultiple() {
        assertTrue(LoginbuddyScope.Management.isScopeValid("management.configuration.read.clients management"));
        assertTrue(LoginbuddyScope.ReadProperties.isScopeValid("management.configuration.read.clients,management.configuration.read.properties"));
        assertFalse(LoginbuddyScope.Read.isScopeValid("management.configuration.write;management.configuration.read.properties"));
    }
}