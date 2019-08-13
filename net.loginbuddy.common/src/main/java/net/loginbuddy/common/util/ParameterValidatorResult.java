package net.loginbuddy.common.util;

public class ParameterValidatorResult {

  public enum RESULT {NONE, MULTIPLE, EMPTY, VALID}

  private RESULT result;
  private String value;

  ParameterValidatorResult(RESULT result, String value) {
    this.result = result;
    this.value = value;
  }

  public RESULT getResult() {
    return result;
  }

  public String getValue() {
    return value;
  }
}