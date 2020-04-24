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

/** Model to denote a configuration to load WebQuery into BigQuery. */
@AutoValue
public abstract class TransferConfig {

  static Builder builder() {
    return new AutoValue_TransferConfig.Builder();
  }

  public abstract BigQueryConfig getBigQueryConfig();

  public abstract String getTempGcsBucketName();

  public abstract String getWebQueryUrl();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setBigQueryConfig(BigQueryConfig newBigQueryConfig);

    public abstract Builder setTempGcsBucketName(String newTempGcsBucketName);

    public abstract Builder setWebQueryUrl(String newWebQueryUrl);

    public abstract TransferConfig build();
  }
}
