package dswebquerytobigquery;

import com.google.api.client.extensions.java6.auth.oauth2.AbstractPromptReceiver;

/**
 * Provides Prompt receiver for command line prompt to allow user to supply Authorization code.
 */
public class CommandLinePromptReceiver extends AbstractPromptReceiver {

  public static CommandLinePromptReceiver newReceiver() {
    return new CommandLinePromptReceiver();
  }

  @Override
  public String getRedirectUri() {
    return "urn:ietf:wg:oauth:2.0:oob";
  }
}
