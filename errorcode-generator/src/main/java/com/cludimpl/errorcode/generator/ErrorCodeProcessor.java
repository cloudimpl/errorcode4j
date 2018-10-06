/*
 * To change this license header, choose License Headers in Project Properties. To change this template file, choose
 * Tools | Templates and open the template in the editor.
 */
package com.cludimpl.errorcode.generator;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author nuwansa
 */
public class ErrorCodeProcessor {

  public static List<String> getTags(String format) {
    List<String> tags = new LinkedList<>();
    StringBuilder builder = new StringBuilder();
    boolean openBracket = false;
    for (char c : format.toCharArray()) {
      switch (c) {
        case '[':
          if (openBracket) {
            throw new InvalidErrorFormatException(format);
          }
          openBracket = true;
          break;
        case ']':
          if (!openBracket) {
            throw new InvalidErrorFormatException(format);
          } else if (!builder.toString().trim().isEmpty()) {
            tags.add(builder.toString().trim());

          } else {
            throw new InvalidErrorFormatException("empty tag not allowed.format = " + format);
          }
          builder.setLength(0);
          openBracket = false;
          break;
        default:
          if (openBracket) {
            if (Character.isWhitespace(c)) {
              throw new InvalidErrorFormatException("whitespace not allowed inside the tag.format = " + format);
            }
            builder.append(c);
          }
          break;
      }

    }
    return tags;
  }
}
