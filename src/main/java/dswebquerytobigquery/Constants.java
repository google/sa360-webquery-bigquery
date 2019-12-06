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

import com.google.api.services.bigquery.BigqueryScopes;
import com.google.api.services.storage.StorageScopes;
import com.google.common.collect.ImmutableSet;

/** Application Configuration Properties.*/
final class Constants {

  public static final String CSV_FILE_PREFIX = "dswq_";

  public static final int MAX_THREADS = 10;
  public static final String REPORT_PULL_TIMESTAMP_COLUMN_NAME = "reporting_date";

  public static final ImmutableSet<String> REQUIRED_SCOPES =
      ImmutableSet.of(
          BigqueryScopes.BIGQUERY_INSERTDATA,
          BigqueryScopes.BIGQUERY,
          StorageScopes.DEVSTORAGE_READ_WRITE,
          "https://www.googleapis.com/auth/doubleclicksearch");

  public static final String APPLICATION_NAME = "WebQueryToBigQueryv1";
  public static final String CREDENTIAL_DATASTORE_FOLDER = "datastore";

  private Constants() {}
}
