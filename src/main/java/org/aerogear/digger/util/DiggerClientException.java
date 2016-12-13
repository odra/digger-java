package org.aerogear.digger.util;

/**
 * Represents internal client exception
 */
public class DiggerClientException extends Exception {

  public DiggerClientException() {
    super();
  }

  public DiggerClientException(String message) {
    super(message);
  }

  public DiggerClientException(String message, Throwable cause) {
    super(message, cause);
  }

  public DiggerClientException(Throwable cause) {
    super(cause);
  }

  @Override
  public Throwable fillInStackTrace() {
    return null;
  }
}
