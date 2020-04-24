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

import java.util.HashMap;

/** Mapper for WebQuery column type to BigQuery Column type. */
class WqToBqDataTypeMapper {

  static final String TIMESTAMP = "TIMESTAMP";
  static final String INTEGER = "INTEGER";
  static final String TEXT = "STRING";
  static final String DEFAULT_TYPE = "STRING";
  private static final HashMap<String, String> TYPE_MAPPER;

  static {
    TYPE_MAPPER = new HashMap<>(5);
    TYPE_MAPPER.put("date", "DATE");
    TYPE_MAPPER.put("text", "STRING");
    TYPE_MAPPER.put("integral", "INTEGER");
    TYPE_MAPPER.put("decimal", "FLOAT");
    TYPE_MAPPER.put("percent", "FLOAT");
  }

  /**
   * Translate Data-types from DS WebQuery type to BigQuery Column type If no mapping found, returns
   * {@code DEFAULT_TYPE}
   *
   * @param type WebQwuery type
   * @return BigQueryType
   */
  public static String translateWebQueryTypeToBigQueryType(String type) {

    String bqType = TYPE_MAPPER.get(type);
    return (bqType != null) ? bqType : DEFAULT_TYPE;
  }
}
