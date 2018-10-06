/*
 * To change this license header, choose License Headers in Project Properties. To change this template file, choose
 * Tools | Templates and open the template in the editor.
 */
package com.cloudimpl.error.core;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author nuwansa
 */
public class ErrorBuilder {

  protected Enum<? extends ErrorCode> errorCode;
  protected final Map<String, Object> tags = new HashMap<>();
  protected Throwable exception = null;

  protected ErrorBuilder() {

  }

  protected final ErrorBuilder withCode(Enum<? extends ErrorCode> errorCode) {
    this.errorCode = errorCode;
    return this;
  }

  protected final ErrorBuilder withTag(String tag, Object value) {
    tags.put(tag, value);
    return this;
  }

  public final ErrorBuilder wrap(Throwable thr) {
    this.exception = thr;
    return this;
  }

  public CloudImplException build() {
    return new CloudImplException(this);
  }

}
