
package py.exception;

/**
 * exception throw when delete a account but the account has available volumes.
 */
public class EventDataOlderException extends Exception {

  private static final long serialVersionUID = -1065415306395856777L;

  public EventDataOlderException() {
    super();
  }

  public EventDataOlderException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public EventDataOlderException(String message, Throwable cause) {
    super(message, cause);
  }

  public EventDataOlderException(String message) {
    super(message);
  }

  public EventDataOlderException(Throwable cause) {
    super(cause);
  }

}
