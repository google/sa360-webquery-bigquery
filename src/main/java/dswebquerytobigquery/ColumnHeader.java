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

/** Model to store information on the WebQuery's columns. */
@AutoValue
public abstract class ColumnHeader {

  public abstract String name();
  public abstract String bqType();

  public static ColumnHeader create(String name, String bqType) {
    return new AutoValue_ColumnHeader(name, bqType);
  }
}
