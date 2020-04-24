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
import com.google.api.client.extensions.java6.auth.oauth2.AbstractPromptReceiver;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/** Service to capture user credentials. */
class UserAuthorizationFlow {

  /**
   * Retrieves user's credential from local-storage or prompts the user to provide authorization code.
   *
   * @return the authorized credentials for the user.
   * @throws IOException in case of error in reading/creating credential.
   */
  public static Credential authorize() throws IOException {

    //Filebased Datastore
    File datastoreDirectory = new File(Constants.CREDENTIAL_DATASTORE_FOLDER);
    datastoreDirectory.mkdir();

    // load client secrets
    GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(new JacksonFactory(),
        new InputStreamReader(
            UserAuthorizationFlow.class.getResourceAsStream("/client_secrets.json")));
    // set up authorization code flow
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
        new NetHttpTransport(), new JacksonFactory(), clientSecrets,
        Constants.REQUIRED_SCOPES)
        .setDataStoreFactory(new FileDataStoreFactory(datastoreDirectory))
        .build();
    // authorize
    return new AuthorizationCodeInstalledApp(flow, new AbstractPromptReceiver() {
      @Override
      public String getRedirectUri() {
        return "urn:ietf:wg:oauth:2.0:oob";
      }
    }).authorize("user");
  }

}
