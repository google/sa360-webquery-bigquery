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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.apache.commons.csv.CSVFormat;

/** Service to read CSV Configurations for loading multiple WebQuery to BQ jobs. */
class ConfigReader {

  /**
   * Retrieve configuration from CSV File.
   *
   * @return Array of WebQuery to BigQuery Transfer configuration items
   */
  public static TransferConfig[] loadConfig(File configFile) throws IOException {
    return CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new FileReader(configFile))
        .getRecords()
        .stream()
        .map(
            // Build TransferConfig objects from CSV rows
            record ->
                TransferConfig.builder()
                    .setBigQueryConfig(
                        BigQueryConfig.builder()
                            .setProjectId(record.get("projectId"))
                            .setDatasetId(record.get("datasetId"))
                            .setTableId(record.get("tableId"))
                            .build())
                    .setWebQueryUrl(record.get("webQueryUrl"))
                    .setTempGcsBucketName(record.get("gcsBucketName"))
                    .build()
        )
        .toArray(TransferConfig[]::new);
  }
}
