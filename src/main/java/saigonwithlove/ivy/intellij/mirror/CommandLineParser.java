// Copyright 2010 Christian d'Heureuse, Inventec Informatik AG, Zurich, Switzerland
// www.source-code.biz, www.inventec.ch/chdh
//
// This module is multi-licensed and may be used under the terms of any of the following licenses:
//
//  LGPL, GNU Lesser General Public License, V2.1 or later, http://www.gnu.org/licenses/lgpl.html
//  EPL, Eclipse Public License, V1.0 or later, http://www.eclipse.org/legal
//
// Please contact the author if you need another license.
// This module is provided "as is", without warranties of any kind.
//
// Home page: http://www.source-code.biz/filemirrorsync

package saigonwithlove.ivy.intellij.mirror;

public final class CommandLineParser {

  public static class CommandLineException extends RuntimeException {
    private static final long serialVersionUID = 1;

    public CommandLineException(String msg) {
      super(msg);
    }
  }

  private String[] args;
  private int n;
  private int p;
  private int parmPos;

  public CommandLineParser(String[] args) {
    this.args = args;
    n = args.length;
  }

  public int getNoOfTokens() {
    return n;
  }

  public boolean eol() {
    return p >= n;
  }

  public String getToken() {
    if (p >= n) throw new CommandLineException("Unexpected end-of-line on command line.");
    return args[p];
  }

  public void skipToken() {
    p++;
  }

  public String nextToken() {
    String s = getToken();
    skipToken();
    return s;
  }

  public String nextArg() {
    if (isSwitch())
      throw new CommandLineException(
          "Option \"" + getToken() + "\" encountered when expecting argument.");
    return nextToken();
  }

  public int nextArgInt() {
    String s = nextArg();
    try {
      return Integer.parseInt(s);
    } catch (Exception e) {
      throw new CommandLineException("Invalid integer command line parameter value \"" + s + "\".");
    }
  }

  public float nextArgFloat() {
    String s = nextArg();
    try {
      return Float.parseFloat(s);
    } catch (Exception e) {
      throw new CommandLineException(
          "Invalid floating point number command line parameter value \"" + s + "\".");
    }
  }

  public <T extends Enum<T>> T nextArgEnum(Class<T> enumType) {
    String s = nextArg();
    try {
      return Enum.valueOf(enumType, s);
    } catch (IllegalArgumentException e) {
      throw new CommandLineException(
          "Invalid enumeration command line parameter value \"" + s + "\".");
    }
  }

  public boolean isSwitch() {
    return getToken().length() >= 2 && getToken().charAt(0) == '-';
  }

  public boolean check(String s) {
    if (getToken().equals(s)) {
      skipToken();
      return true;
    }
    return false;
  }

  public int getParameterPosition() {
    return parmPos;
  }

  public int nextParameterPosition() {
    return parmPos++;
  }
} // end class CommandLineParser
