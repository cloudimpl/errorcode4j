/*
 * To change this license header, choose License Headers in Project Properties. To change this template file, choose
 * Tools | Templates and open the template in the editor.
 */
package com.cludimpl.errorcode.generator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *
 * @author nuwansa
 */
public class Reflector {

  private Method methodErrorNo;
  private Method methodErrorFormat;
  private Method methodName;

  public Reflector(Class clazz) {
    try {
      methodErrorNo = clazz.getDeclaredMethod("getErrorNo");
      methodErrorFormat = clazz.getDeclaredMethod("getFormat");
      methodName = clazz.getSuperclass().getDeclaredMethod("name");

    } catch (NoSuchMethodException | SecurityException ex) {
      throw new ReflectorException(ex);
    }
  }

  public int getErrorCode(Object object) {
    try {
      return (int) methodErrorNo.invoke(object);
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
      throw new ReflectorException(ex);
    }
  }

  public String getErrorFormat(Object object) {
    try {
      return (String) methodErrorFormat.invoke(object);
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
      throw new ReflectorException(ex);
    }
  }

  public String getName(Object object) {
    try {
      return (String) methodName.invoke(object);
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
      throw new ReflectorException(ex);
    }
  }

  public static final class ReflectorException extends RuntimeException {

    public ReflectorException(Throwable thr) {
      super(thr);
    }

  }
}
