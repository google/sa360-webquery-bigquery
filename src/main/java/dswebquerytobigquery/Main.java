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

import com.google.api.services.bigquery.BigqueryScopes;
import com.google.api.services.storage.StorageScopes;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.collect.ImmutableList;
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

    var executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_THREADS);

    var serviceAccountCredentials = buildServiceAccountCredentials();
    serviceAccountCredentials.refresh();
    // Run all configs
    Stream.of(transferConfigs)
      .map(config ->
        new TransferRunner(
          config,
          serviceAccountCredentials,
          BigQueryFactory.getDefaultInstance(serviceAccountCredentials),
          StorageServiceFactory.getDefaultInstance(serviceAccountCredentials),
          tmpFolder))
      .forEach(executor::execute);

    executor.shutdown();
    logger.atInfo().log("Waiting for workers to complete.");
  }

  private static GoogleCredentials buildServiceAccountCredentials() throws IOException{
    return GoogleCredentials.getApplicationDefault()
      .createScoped(
        ImmutableList.<String>builder()
          .addAll(StorageScopes.all())
          .addAll(BigqueryScopes.all())
          .add("https://www.googleapis.com/auth/doubleclicksearch")
          .build());
  }
}
