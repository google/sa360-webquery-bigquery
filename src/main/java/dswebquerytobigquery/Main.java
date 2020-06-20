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

import static com.google.common.flogger.util.Checks.checkArgument;
import static dswebquerytobigquery.Constants.MAX_THREADS;

import com.google.api.client.auth.oauth2.Credential;
import com.google.common.flogger.FluentLogger;
import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Stream;

class Main {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public static void main(String[] args) throws IOException {

    checkArgument(args.length == 1, "Provide Configuration CSV");
    // Load Config File
    logger.atInfo().log("config file: %s", args[0]);
    File configFile = new File(args[0]);
    TransferConfig[] transferConfigs = ConfigReader.loadConfig(configFile);

    logger.atFine().log("Loaded %s configurations", transferConfigs.length);

    // Get Google Credential
    Credential credential = Authorizer.authorize();
    logCredentialInfo(credential);

    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_THREADS);

    // Run all configs
    Stream.of(transferConfigs)
        .map(config -> new TransferRunner(config, credential, BigQueryFactory.getDefaultInstance(),
            StorageServiceFactory.getDefaultInstance()))
        .forEach(executor::execute);

    executor.shutdown();
  }

  private static void logCredentialInfo(Credential credential) {
    logger.atInfo()
        .log("access_token: %s%nExpires:%s%nrefreshTokenPresent: %s",
            credential.getAccessToken(),
            Instant.ofEpochMilli(credential.getExpirationTimeMilliseconds())
                .atZone(Clock.systemDefaultZone().getZone()),
            (credential.getRefreshToken() != null));
  }
}
