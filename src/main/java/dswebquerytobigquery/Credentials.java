// Copyright 2021 Google LLC
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
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import java.util.Date;

public class Credentials {

  private final GoogleClientSecrets clientSecrets;

  private Credentials(GoogleClientSecrets clientSecrets) {
    this.clientSecrets = clientSecrets;
  }

  public static Credentials usingSecrets(GoogleClientSecrets clientSecrets) {
    return new Credentials(clientSecrets);
  }

  public UserCredentials forCredential(Credential credential) {
    return UserCredentials.newBuilder()
      .setClientId(clientSecrets.getInstalled().getClientId())
      .setClientSecret(clientSecrets.getInstalled().getClientSecret())
      .setAccessToken(
        new AccessToken(credential.getAccessToken(),
          new Date(credential.getExpirationTimeMilliseconds())))
      .setRefreshToken(credential.getRefreshToken())
      .build();
  }
}
