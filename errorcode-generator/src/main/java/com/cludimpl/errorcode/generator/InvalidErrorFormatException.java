/*
 * To change this license header, choose License Headers in Project Properties. To change this template file, choose
 * Tools | Templates and open the template in the editor.
 */
package com.cludimpl.errorcode.generator;

/**
 *
 * @author nuwansa
 */
public class InvalidErrorFormatException extends RuntimeException {

  public InvalidErrorFormatException(String message) {
    super(message);
  }

}
