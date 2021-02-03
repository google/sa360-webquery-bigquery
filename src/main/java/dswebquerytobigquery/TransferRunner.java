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

import com.google.auth.oauth2.UserCredentials;
import com.google.common.flogger.GoogleLogger;
import java.io.File;

/**
 * Provides an indempotent runner for each configuration.
 */
class TransferRunner implements Runnable {

  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  private final TransferConfig xferConfig;
  private final UserCredentials credential;
  private final BigQueryFactory bigQueryFactory;
  private final StorageServiceFactory storageServiceFactory;
  private final File csvFolder;

  public TransferRunner(TransferConfig xferConfig,
                        UserCredentials credential,
                        BigQueryFactory bigQueryFactory,
                        StorageServiceFactory storageServiceFactory,
                        File csvFolder) {
    this.xferConfig = xferConfig;
    this.credential = credential;
    this.bigQueryFactory = bigQueryFactory;
    this.storageServiceFactory = storageServiceFactory;
    this.csvFolder = csvFolder;
  }

  @Override
  public void run() {
    logger.atInfo().log("Processing: %s", xferConfig);

    var webQuery = new WebQuery(xferConfig.getWebQueryUrl(), credential);
    // local file to store the report as CSV
    var tempCsvFile = createCsvFile();

    try {
      logger.atInfo()
          .log("[Report %s] starting: url: %s", webQuery.getReportId(), webQuery.getQueryUrl());

      logger.atInfo()
          .log("[Report %s] localFile: %s", webQuery.getReportId(), tempCsvFile.getAbsolutePath());

      // Convert to CSV File
      webQuery.read().writeAsCsv(tempCsvFile);

      // Copy to GCS
      var gcsLink = new StorageController(storageServiceFactory.buildStorageService())
          .uploadFile(tempCsvFile, xferConfig.getTempGcsBucketName(), "sa360tmp");

      logger.atInfo().log("GCS Link: %s", gcsLink);

      //Delete local file
      logger.atInfo().log("marking file for deletion: %s", tempCsvFile.getAbsolutePath());

      // Issue BigQuery command to consume file into a table
      var bqJob =
          new BigQueryProcessor(xferConfig.getBigQueryConfig(),
              bigQueryFactory.getBigQueryService(xferConfig.getBigQueryConfig().getProjectId()))
              .loadFileInTable(gcsLink);

      logger.atInfo().log(
        "[Report %s] BQ JobId:%s%nBigquery Job link: https://console.cloud.google.com/bigquery?project=%s&page=jobs",
        webQuery.getReportId(),
        bqJob.getJobId().getJob(),
        bqJob.getJobId().getProject());
      logger.atInfo().log("[Report %s] finished  %s", webQuery.getReportId(), xferConfig);
    } catch (Exception exception) {
      logger.atSevere().withCause(exception)
          .log("[Report %s] Error Processing", webQuery.getReportId());
    }
    finally {
      tempCsvFile.deleteOnExit();
    }
  }

  private File createCsvFile() {
    return new File(
      csvFolder.getAbsolutePath() + "/" +
      String.format(
        "%s_%s_%s.csv",
        CSV_FILE_PREFIX,
        Thread.currentThread().getId(),
        new WebQuery(xferConfig.getWebQueryUrl(), credential).getReportId()));
  }
}
