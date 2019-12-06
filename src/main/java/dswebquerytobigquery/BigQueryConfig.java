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

import com.google.auto.value.AutoValue;

/** Configuration for BigQuery Table information. */
@AutoValue
public abstract class BigQueryConfig {

  public static Builder builder() {
    return new AutoValue_BigQueryConfig.Builder();
  }

  public abstract String getProjectId();

  public abstract String getDatasetId();

  public abstract String getTableId();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setProjectId(String newProjectId);

    public abstract Builder setDatasetId(String newDatasetId);

    public abstract Builder setTableId(String newTableId);

    public abstract BigQueryConfig build();
  }
}
