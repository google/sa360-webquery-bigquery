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

import com.google.api.services.bigquery.Bigquery;
import com.google.api.services.bigquery.model.Job;
import com.google.api.services.bigquery.model.JobConfiguration;
import com.google.api.services.bigquery.model.JobConfigurationLoad;
import com.google.api.services.bigquery.model.TableReference;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * BigQuery client to load the CSV file from Cloud Storage into a table.
 */
class BigQueryProcessor {

  private final BigQueryConfig outputTableInfo;
  private final Bigquery bigQueryService;

  public BigQueryProcessor(BigQueryConfig outputTableInfo,
      Bigquery bigQueryService) {
    this.outputTableInfo = outputTableInfo;
    this.bigQueryService = bigQueryService;
  }

  /**
   * Creates a Load job on BigQuery for the provided CSV file on Cloud Bucket.
   *
   * @param gcsFileNames the list of CSV files to be uploaded into the given table. First file
   *                     should contain headers.
   * @return BigQuery load job id.
   */
  public String loadFileInTable(String... gcsFileNames) throws IOException {
    return bigQueryService.jobs().insert(outputTableInfo.getProjectId(), buildLoadJob(gcsFileNames))
        .execute().getId();
  }

  /** Returns a BQ Load job configuration. */
  private Job buildLoadJob(String[] gcsFileNames) {
    return new Job()
        .setConfiguration(
            new JobConfiguration()
                .setLoad(
                    new JobConfigurationLoad()
                        .setDestinationTable(
                            new TableReference()
                                .setProjectId(outputTableInfo.getProjectId())
                                .setDatasetId(outputTableInfo.getDatasetId())
                                .setTableId(outputTableInfo.getTableId() + "_" + getDateSuffix()))
                        .setWriteDisposition("WRITE_TRUNCATE")
                        .setAutodetect(true)
                        .setSkipLeadingRows(1)
                        .setSourceFormat("CSV")
                        .setSourceUris(Arrays.asList(gcsFileNames))
                )
        );
  }

  /** Returns today's date at UTC in YYYYMMDD format to be used a suffix for table-name.*/
  private static String getDateSuffix() {
    return DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now(ZoneOffset.UTC));
  }
}
