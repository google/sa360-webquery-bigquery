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

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FormatOptions;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.LoadJobConfiguration;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.storage.Blob;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

/**
 * BigQuery client to load the CSV file from Cloud Storage into a table.
 */
class BigQueryProcessor {

  private final BigQueryConfig outputTableInfo;
  private final BigQuery bigQueryService;

  public BigQueryProcessor(BigQueryConfig outputTableInfo,
                           BigQuery bigQueryService) {
    this.outputTableInfo = outputTableInfo;
    this.bigQueryService = bigQueryService;
  }

  /**
   * Creates a Load job on BigQuery for the provided CSV file on Cloud Bucket.
   *
   * @param gcsBlobs the list of GCS blobs for CSV files to be uploaded into the given table.
   *                 First file should contain headers.
   * @return BigQuery load job id.
   */
  public Job loadFileInTable(Blob... gcsBlobs) {
    var gcsFileNames =
      Stream.of(gcsBlobs)
        .map(blob -> String.format("gs://%s/%s", blob.getBucket(), blob.getName()))
        .collect(toImmutableList());

    return bigQueryService.create(
      JobInfo.newBuilder(
        LoadJobConfiguration.newBuilder(
          TableId.of(
            outputTableInfo.getProjectId(),
            outputTableInfo.getDatasetId(),
            outputTableInfo.getTableId() + "_" + getDateSuffix()),
          gcsFileNames,
          FormatOptions.csv()
            .toBuilder()
            .setSkipLeadingRows(1)
            .setAllowQuotedNewLines(true)
            .build())
          .setAutodetect(true)
          .setWriteDisposition(JobInfo.WriteDisposition.WRITE_TRUNCATE)
          .build())
        .build());
  }

  /**
   * Returns today's date at UTC in YYYYMMDD format to be used a suffix for table-name.
   */
  private static String getDateSuffix() {
    return DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now(ZoneOffset.UTC));
  }
}
