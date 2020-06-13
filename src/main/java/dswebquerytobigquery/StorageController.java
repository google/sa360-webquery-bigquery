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

import static com.google.common.base.Verify.verify;

import com.google.api.client.http.InputStreamContent;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.StorageObject;
import com.google.common.flogger.FluentLogger;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Service to store files into Google Cloud Storage.
 */
class StorageController {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final Storage storageService;

  public StorageController(Storage storageService) {
    this.storageService = storageService;
  }


  private static InputStreamContent buildStorageContent(File file) throws IOException {
    try (BufferedInputStream contentStream = new BufferedInputStream(new FileInputStream(file))) {
      InputStreamContent content = new InputStreamContent("text/csv", contentStream);
      content.setLength(file.length());
      return content;
    }
  }

  /**
   * Stores the provided local file to Google Cloud Storage.
   *
   * @param file          the local file to be uploaded.
   * @param gcsBucketName the Cloud Storage Bucket name.
   * @param folder        the name of the folder to store the file on GCS.
   * @return the URI of the stored object.
   * @throws IOException in-case there is error uploading the file.
   */
  public String uploadFile(File file, String gcsBucketName, String folder) throws IOException {
    verify(file != null, "Null/Empty file");
    verify(gcsBucketName != null && !gcsBucketName.isEmpty(), "Null Bucket Name");
    verify(folder != null, "Null Folder name");

    String objectId =
        storageService
            .objects()
            .insert(
                gcsBucketName,
                new StorageObject()
                    .setContentType("text/csv")
                    .setName(folder + "/" + file.getName())
                    .setBucket(gcsBucketName),
                buildStorageContent(file))
            .execute()
            .getId();

    logger.atInfo().log("GCS File Id: " + objectId);

    // create a gs link to the file
    return "gs://" + gcsBucketName + "/" + folder + "/" + file.getName();
  }
}
