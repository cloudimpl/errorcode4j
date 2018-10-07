package com.cloudimpl.sample.error;

import com.cloudimpl.error.core.ErrorCode;

public enum Sample implements ErrorCode {
  USER_NOT_FOUND(0, "user [username] not found"),
  LOGIN_FAILED(1, "user login failed for user [username] and reason [reason]");

  private final int errorNo;

  private final String errorFormat;

  Sample(int errorNo, String errorFormat) {
    this.errorNo = errorNo;
    this.errorFormat = errorFormat;
  }

  @Override
  public int getErrorNo() {
    return errorNo;
  }

  @Override
  public String getFormat() {
    return errorFormat;
  }
}
