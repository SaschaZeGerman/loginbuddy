package net.loginbuddy.common.util;

import net.loginbuddy.common.util.ParameterValidatorResult.RESULT;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class ParameterValidator {

  public static ParameterValidatorResult getSingleValue(Enumeration<String> input) {
    return getSingleValue(input, null);
  }

  public static ParameterValidatorResult getSingleValue(Enumeration<String> input, String defaultValue) {
    List<String> values = Collections.list(input);
    return getSingleValue( values.toArray(new String[0]), defaultValue);
  }

  /**
   * the array should have one, non-empty string
   *
   * @param input
   * @return
   */
  public static ParameterValidatorResult getSingleValue(String[] input) {
    return getSingleValue(input, null);
  }

  public static ParameterValidatorResult getSingleValue(String[] input, String defaultValue) {
    if(input == null || input.length == 0) {
      return new ParameterValidatorResult(RESULT.NONE, defaultValue);
    } else if(input.length != 1) {
      return new ParameterValidatorResult(RESULT.MULTIPLE, defaultValue);
    } else if(input[0].trim().length() == 0) {
      return new ParameterValidatorResult(RESULT.EMPTY, defaultValue);
    }
    return new ParameterValidatorResult(RESULT.VALID, input[0]);
  }
}