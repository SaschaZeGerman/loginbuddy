package net.loginbuddy.common.util;

import org.junit.Test;

import java.util.Vector;

import static org.junit.Assert.assertEquals;

public class TestParameterValidator {

    @Test
    public void testArraySingle() {
        String[] value = new String[]{"single"};
        ParameterValidatorResult result = ParameterValidator.getSingleValue(value);
        assertEquals(ParameterValidatorResult.RESULT.VALID, result.getResult());
        assertEquals("single", result.getValue());
    }

    @Test
    public void testArrayMultiple() {
        String[] value = new String[]{"param", "param"};
        ParameterValidatorResult result = ParameterValidator.getSingleValue(value);
        assertEquals(ParameterValidatorResult.RESULT.MULTIPLE, result.getResult());
    }

    @Test
    public void testArrayEmpty() {
        String[] value = new String[]{""};
        ParameterValidatorResult result = ParameterValidator.getSingleValue(value);
        assertEquals(ParameterValidatorResult.RESULT.EMPTY, result.getResult());
    }

    @Test
    public void testArrayEmptyWithDefault() {
        String[] value = new String[]{""};
        ParameterValidatorResult result = ParameterValidator.getSingleValue(value, "default");
        assertEquals(ParameterValidatorResult.RESULT.EMPTY, result.getResult());
        assertEquals("default", result.getValue());
    }

    @Test
    public void testArrayNone() {
        String[] value = new String[0];
        ParameterValidatorResult result = ParameterValidator.getSingleValue(value);
        assertEquals(ParameterValidatorResult.RESULT.NONE, result.getResult());
    }

    @Test
    public void testEnumerationSingle() {
        Vector<String> value = new Vector<>();
        value.add("single");
        ParameterValidatorResult result = ParameterValidator.getSingleValue(value.elements());
        assertEquals(ParameterValidatorResult.RESULT.VALID, result.getResult());
        assertEquals("single", result.getValue());
    }

    @Test
    public void testEnumerationMultiple() {
        Vector<String> value = new Vector<>();
        value.add("param");
        value.add("param");
        ParameterValidatorResult result = ParameterValidator.getSingleValue(value.elements());
        assertEquals(ParameterValidatorResult.RESULT.MULTIPLE, result.getResult());
    }

    @Test
    public void testEnumerationEmpty() {
        Vector<String> value = new Vector<>();
        value.add("");
        ParameterValidatorResult result = ParameterValidator.getSingleValue(value.elements());
        assertEquals(ParameterValidatorResult.RESULT.EMPTY, result.getResult());
    }

    @Test
    public void testEnumerationEmptyWithDefault() {
        Vector<String> value = new Vector<>();
        value.add("");
        ParameterValidatorResult result = ParameterValidator.getSingleValue(value.elements(), "default");
        assertEquals(ParameterValidatorResult.RESULT.EMPTY, result.getResult());
        assertEquals("default", result.getValue());
    }

    @Test
    public void testEnumerationNone() {
        Vector<String> value = new Vector<>();
        ParameterValidatorResult result = ParameterValidator.getSingleValue(value.elements());
        assertEquals(ParameterValidatorResult.RESULT.NONE, result.getResult());
    }
}
