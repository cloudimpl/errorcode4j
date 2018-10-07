/*
 * To change this license header, choose License Headers in Project Properties. To change this template file, choose
 * Tools | Templates and open the template in the editor.
 */
package com.cloudimpl.example;

import com.cloudimpl.sample.error.SampleException;

/**
 *
 * @author nuwansa
 */
public class Foo {

  public void func1() {
    throw SampleException.LOGIN_FAILED(err -> err.setUsername("john").setReason("invalid username or password"));
  }


  public void func2() throws CustomException {
    throw new CustomException("func2 exception");
  }

  // wrap original execption
  public void func3() {

    try {
      func2();
    } catch (CustomException ex) {
      throw SampleException.USER_NOT_FOUND(err -> err.setUsername("xxxx").wrap(ex));
    }
  }



  public static final class CustomException extends Exception {

    public CustomException(String message) {
      super(message);
    }


  }


  public static void main(String[] args) {
    Foo f = new Foo();
    try {
      f.func1();
    } catch (Exception ex) {
      System.out.println("error : " + ex.getMessage());
    }

    try {
      f.func3();
    } catch (Exception ex) {
      System.out.println("error : " + ex.getMessage());
    }

  }
}
