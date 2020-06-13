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
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Service to capture user credentials.
 */
class UserAuthorizationFlow {

  /**
   * Retrieves user's credential from local-storage or prompts the user to provide authorization
   * code.
   *
   * @return the authorized credentials for the user.
   * @throws IOException in case of error in reading/creating credential.
   */
  public static Credential authorize() throws IOException {
    return
        new AuthorizationCodeInstalledApp(
            buildCodeFlow(),
            CommandLinePromptReceiver.newReceiver())
            .authorize("user");
  }

  private static GoogleAuthorizationCodeFlow buildCodeFlow() throws IOException {
    return new GoogleAuthorizationCodeFlow.Builder(
        new NetHttpTransport(), new JacksonFactory(), readClientSecrets(),
        Constants.REQUIRED_SCOPES)
        .setDataStoreFactory(buildFileDatastoreFactory())
        .setAccessType("offline")
        .build();
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
        UserAuthorizationFlow.class.getResourceAsStream("/client_secrets.json"))) {
      // load client secrets
      return GoogleClientSecrets.load(new JacksonFactory(), clientSecretsReader);
    }
  }

}
