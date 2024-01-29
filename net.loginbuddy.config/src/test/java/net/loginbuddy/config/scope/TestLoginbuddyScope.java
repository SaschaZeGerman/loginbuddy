package net.loginbuddy.config.scope;

import net.loginbuddy.config.management.scope.LoginbuddyScope;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

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

    @Test
    public void testGrantMananagementScope() {
        Set<String> actual = LoginbuddyScope.Management.grantScope("management.configuration.read.clients management");
        assertEquals(2, actual.size());
        assertTrue(actual.contains("management"));
        assertTrue(actual.contains("management.configuration.read.clients"));
    }

    @Test
    public void testGrantMultipleConfigurationScope() {
        Set<String> actual = LoginbuddyScope.Configuration.grantScope("management.configuration.read management.configuration.write management management.configuration");
        assertEquals(3, actual.size());
        assertTrue(actual.contains("management.configuration"));
        assertTrue(actual.contains("management.configuration.read"));
        assertTrue(actual.contains("management.configuration.write"));
    }

    @Test
    public void testGrantReadConfigurationScope() {
        Set<String> actual = LoginbuddyScope.Read.grantScope("management.configuration.read management.configuration.write management management.configuration");
        assertEquals(1, actual.size());
        assertTrue(actual.contains("management.configuration.read"));
    }

    @Test
    public void testGrantWriteClientsConfigurationScope() {
        Set<String> actual = LoginbuddyScope.WriteClients.grantScope("management.configuration.write.clients management.configuration.write.providers management.configuration.write");
        assertEquals(1, actual.size());
        assertTrue(actual.contains("management.configuration.write.clients"));
    }

    @Test
    public void testGrantReadWriteConfigurationScope() {
        Set<String> actual = LoginbuddyScope.Management.grantScope("management.configuration.write management.configuration.read");
        assertEquals(2, actual.size());
        assertTrue(actual.contains("management.configuration.write"));
        assertTrue(actual.contains("management.configuration.read"));
    }

    @Test
    public void testGrantReadWriteManagementRuntimeScope() {
        Set<String> actual = LoginbuddyScope.Management.grantScope("management.runtime.write management.runtime.read");
        assertEquals(2, actual.size());
        assertTrue(actual.contains("management.runtime.read"));
        assertTrue(actual.contains("management.runtime.write"));
    }

    @Test
    public void testGrantReadWriteRuntimeRuntimeScope() {
        Set<String> actual = LoginbuddyScope.Runtime.grantScope("management.runtime.write management.runtime.read");
        assertEquals(2, actual.size());
        assertTrue(actual.contains("management.runtime.read"));
        assertTrue(actual.contains("management.runtime.write"));
    }

    @Test
    public void testGrantReadRuntimeScope() {
        Set<String> actual = LoginbuddyScope.RuntimeRead.grantScope("management.runtime.write management.runtime.read management.runtime");
        assertEquals(1, actual.size());
        assertTrue(actual.contains("management.runtime.read"));
    }
}