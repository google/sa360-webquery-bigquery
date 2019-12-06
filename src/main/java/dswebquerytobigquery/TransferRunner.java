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

import static dswebquerytobigquery.Constants.CSV_FILE_PREFIX;

import com.google.api.client.auth.oauth2.Credential;
import com.google.common.flogger.FluentLogger;
import java.io.File;

/**
 * Provides an indempotent runner for each configuration.
 */
class TransferRunner implements Runnable {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final TransferConfig xferConfig;
  private final Credential credential;
  private final BigQueryFactory bigQueryFactory;
  private final StorageServiceFactory storageServiceFactory;

  public TransferRunner(TransferConfig xferConfig,
      Credential credential, BigQueryFactory bigQueryFactory,
      StorageServiceFactory storageServiceFactory) {
    this.xferConfig = xferConfig;
    this.credential = credential;
    this.bigQueryFactory = bigQueryFactory;
    this.storageServiceFactory = storageServiceFactory;
  }

  @Override
  public void run() {
    logger.atInfo().log("Processing: %s", xferConfig);

    WebQuery webQuery = new WebQuery(xferConfig.getWebQueryUrl());

    try {
      logger.atInfo()
          .log("[Report %s] starting: url: %s", webQuery.getReportId(), webQuery.getQueryUrl());

      // save to local file
      File tempCsvFile = File.createTempFile(CSV_FILE_PREFIX, ".csv");
      logger.atInfo()
          .log("[Report %s] localFile: %s", webQuery.getReportId(), tempCsvFile.getAbsolutePath());

      // Convert to CSV File
      webQuery
          .read(credential)
          .writeAsCsv(tempCsvFile);

      // Copy to GCS
      String gcsLink = new StorageController(storageServiceFactory.buildStorageService(credential))
          .uploadFile(tempCsvFile, xferConfig.getTempGcsBucketName(), "tmp");

      logger.atInfo().log("GCS Link: %s", gcsLink);

      // Issue BigQuery command to consume file into a table
      String bqJobId =
          new BigQueryProcessor(xferConfig.getBigQueryConfig(),
              bigQueryFactory.getBigQueryService(credential))
              .loadFileInTable(gcsLink);

      logger.atInfo().log("[Report %s] BQ Job Id: %s", webQuery.getReportId(), bqJobId);
      logger.atInfo().log("[Report %s] finished  %s", webQuery.getReportId(), xferConfig);
    } catch (Exception exception) {
      logger.atSevere().withCause(exception)
          .log("[Report %s] Error Processing", webQuery.getReportId());
    }
  }
}
