// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package dswebquerytobigquery;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.common.flogger.FluentLogger;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Clock;

/**
 * Service to Load user credentials from file or AuthorizationFlow if not found.
 */
class Authorizer {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String DATASTORE_USER_NAME = "localUser";

  private Authorizer() {
  }

  /**
   * Retrieves user's credential from local-storage or prompts the user to provide authorization
   * code.
   *
   * @return the authorized credentials for the user.
   * @throws IOException in case of error in reading/creating credential.
   */
  public static Credential authorize() throws IOException {
    Credential credential = buildCodeFlow().loadCredential(DATASTORE_USER_NAME);

    CredentialState credentialState = identifyCredentialState(credential);
    logger.atInfo().log("Credential state: %s", credentialState);

    switch (credentialState) {
      case NO_TOKENS:
      case NO_CREDENTIAL:
        return new AuthorizationCodeInstalledApp(
            buildCodeFlow(),
            CommandLinePromptReceiver.newReceiver())
            .authorize(DATASTORE_USER_NAME);
      case VALID_NO_REFRESH_TOKEN:
      case VALID:
        return credential;
      case EXPIRED_WITH_REFRESH_TOKEN:
        return refreshCredentials(credential);
      case EXPIRED_NO_REFRESH_TOKEN:
        throw new IllegalStateException("Access Token Expired, No Refresh Token found");
    }

    throw new IllegalStateException(
        "Unexpected value: " + credentialState + " (cred: " + credential + ")");
  }

  private static CredentialState identifyCredentialState(Credential credential) {

    if (credential == null) {
      return CredentialState.NO_CREDENTIAL;
    } else if (credential.getAccessToken() == null && credential.getRefreshToken() == null) {
      return CredentialState.NO_TOKENS;
    } else if (credential.getAccessToken() != null && credential.getRefreshToken() != null
        && credential.getExpirationTimeMilliseconds() <= Clock.systemUTC().millis()) {
      return CredentialState.EXPIRED_WITH_REFRESH_TOKEN;
    } else if (credential.getAccessToken() != null && credential.getRefreshToken() == null
        && credential.getExpirationTimeMilliseconds() <= Clock.systemUTC().millis()) {
      return CredentialState.EXPIRED_NO_REFRESH_TOKEN;
    } else if (credential.getAccessToken() != null && credential.getRefreshToken() == null
        && credential.getExpirationTimeMilliseconds() > Clock.systemUTC().millis()) {
      return CredentialState.VALID_NO_REFRESH_TOKEN;
    } else if (credential.getAccessToken() != null && credential.getRefreshToken() != null
        && credential.getExpirationTimeMilliseconds() > Clock.systemUTC().millis()) {
      return CredentialState.VALID;
    }

    return CredentialState.NO_CREDENTIAL;
  }


  private static GoogleAuthorizationCodeFlow buildCodeFlow() throws IOException {
    return new GoogleAuthorizationCodeFlow.Builder(
        new NetHttpTransport(), new JacksonFactory(), readClientSecrets(),
        Constants.REQUIRED_SCOPES)
        .setDataStoreFactory(buildFileDatastoreFactory())
        .setAccessType("offline")
        .build();
  }

  private static Credential refreshCredentials(Credential credential) throws IOException {
    GoogleClientSecrets secrets = readClientSecrets();

    GoogleTokenResponse response =
        new GoogleRefreshTokenRequest(
            new NetHttpTransport(),
            new JacksonFactory(),
            credential.getRefreshToken(),
            secrets.getDetails().getClientId(),
            secrets.getDetails().getClientSecret()).execute();
    response.setRefreshToken(credential.getRefreshToken());

    return buildCodeFlow().createAndStoreCredential(response, DATASTORE_USER_NAME);
  }

  /**
   * Returns a file based data store to persist OAuth  tokens.
   */
  private static DataStoreFactory buildFileDatastoreFactory() throws IOException {
    File datastoreDirectory = new File(Constants.CREDENTIAL_DATASTORE_FOLDER);
    datastoreDirectory.mkdir();

    return new FileDataStoreFactory(datastoreDirectory);
  }

  /**
   * Returns Client credentials to use with the application by reading the embedded
   * client_secrets.json file.
   */
  private static GoogleClientSecrets readClientSecrets() throws IOException {
    try (InputStreamReader clientSecretsReader = new InputStreamReader(
        Authorizer.class.getResourceAsStream("/client_secrets.json"))) {
      // load client secrets
      return GoogleClientSecrets.load(new JacksonFactory(), clientSecretsReader);
    }
  }

  private enum CredentialState {
    NO_CREDENTIAL, NO_TOKENS, EXPIRED_WITH_REFRESH_TOKEN, EXPIRED_NO_REFRESH_TOKEN, VALID_NO_REFRESH_TOKEN, VALID
  }
}
