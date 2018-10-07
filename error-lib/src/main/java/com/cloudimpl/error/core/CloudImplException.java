/*
 * To change this license header, choose License Headers in Project Properties. To change this template file, choose
 * Tools | Templates and open the template in the editor.
 */
package com.cloudimpl.error.core;

import java.util.Collection;
import java.util.Map;

/**
 *
 * @author nuwansa
 */
public class CloudImplException extends RuntimeException {

  public static boolean STACK_FILL = true;
  public static boolean FILL_TAGS = true;

  private final Enum<? extends ErrorCode> errorCode;
  private final Map<String, Object> tags;
  private final Throwable exception;

  protected CloudImplException(ErrorBuilder builder) {
    super(CloudImplException.generateMsg(builder));
    this.errorCode = builder.errorCode;
    this.tags = builder.tags;
    this.exception = builder.exception;
  }

  @Override
  public synchronized Throwable fillInStackTrace() {
    if (STACK_FILL) {
      return super.fillInStackTrace();
    }
    return this;
  }

  public <T extends Enum<T> & ErrorCode> T getErrorCode() {
    return (T) errorCode;
  }

  public Collection<String> getTags() {
    return tags.keySet();
  }

  public Object getTag(String key) {
    return tags.get(key);
  }

  public static <T> CloudImplException throwException(ErrorBuilder builder) {
    throw builder.build();
  }

  private static String generateMsg(ErrorBuilder builder) {
    if (!FILL_TAGS) {
      return ((ErrorCode) builder.errorCode).getFormat();
    } else {
      return fillTags(((ErrorCode) builder.errorCode).getFormat(), builder.tags);
    }
  }

  private static String fillTags(String format, Map<String, Object> tags) {
    StringBuilder builder = new StringBuilder();
    boolean openTag = false;
    StringBuilder tagName = new StringBuilder();
    for (int i = 0; i < format.length(); i++) {
      char c = format.charAt(i);
      if (c == ']') {
        openTag = false;
        Object value = tags.get(tagName.toString().trim());
        if (value != null) {
          builder.append(value);
        } else {
          builder.append("[").append(tagName).append("]");
        }
        tagName.setLength(0);
      } else if (c == '[') {
        openTag = true;
      } else if (openTag) {
        tagName.append(c);
      } else {
        builder.append(c);
      }
    }
    return builder.toString();
  }
}
