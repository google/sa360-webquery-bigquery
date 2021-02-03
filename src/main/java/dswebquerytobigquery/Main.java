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

import static com.google.common.base.Preconditions.checkArgument;
import static dswebquerytobigquery.Constants.MAX_THREADS;

import com.google.auth.oauth2.UserCredentials;
import com.google.common.flogger.GoogleLogger;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Stream;

class Main {

  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  public static void main(String[] args) throws IOException {

    checkArgument(args.length == 2, "Provide Configuration CSV and location for csv reports");
    // Load Config File
    logger.atInfo().log("config file: %s", args[0]);
    var configFile = new File(args[0]);

    var tmpFolder = new File(args[1]);
    checkArgument(tmpFolder.isDirectory(), "provided URI is not a folder: %s", args[1]);
    TransferConfig[] transferConfigs = ConfigReader.loadConfig(configFile);

    logger.atFine().log("Loaded %s configurations", transferConfigs.length);

    // Get Google Credential
    var userCredential = Authorizer.authorize();
    logCredentialInfo(userCredential);

    var executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_THREADS);

    // Run all configs
    Stream.of(transferConfigs)
      .map(config ->
        new TransferRunner(
          config,
          userCredential,
          BigQueryFactory.getDefaultInstance(userCredential),
          StorageServiceFactory.getDefaultInstance(userCredential),
          tmpFolder))
      .forEach(executor::execute);

    executor.shutdown();
  }

  private static void logCredentialInfo(UserCredentials credential) {
    logger.atInfo()
      .log("access_token: %s%nrefreshTokenPresent: %s",
        credential.getAccessToken(),
        (credential.getRefreshToken() != null));
  }
}
