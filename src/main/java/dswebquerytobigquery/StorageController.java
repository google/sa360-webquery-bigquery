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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.common.flogger.GoogleLogger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Service to store files into Google Cloud Storage.
 */
class StorageController {

  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  private final Storage storageService;

  public StorageController(Storage storageService) {
    this.storageService = storageService;
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
  public Blob uploadFile(File file, String gcsBucketName, String folder) throws IOException {
    checkNotNull(file, "Null/Empty file");
    checkNotNull(folder, "Null Folder name");
    checkArgument(gcsBucketName != null && !gcsBucketName.isEmpty(), "Null Bucket Name");

    var gcsObject =
        storageService
          .create(
            BlobInfo
              .newBuilder(BlobId.of(gcsBucketName, String.format("%s/%s", folder, file.getName())))
              .setContentType("text/csv")
              .build(),
            Files.readAllBytes(Paths.get(file.getAbsolutePath())));

    logger.atInfo().log("GCS File Id: %s", gcsObject.getBlobId());

    // create a gs link to the file
    return gcsObject;
  }
}
