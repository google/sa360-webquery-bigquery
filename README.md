[![Total alerts](https://img.shields.io/lgtm/alerts/g/google/sa360-webquery-bigquery.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/google/sa360-webquery-bigquery/alerts/) [![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/google/sa360-webquery-bigquery.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/google/sa360-webquery-bigquery/context:java)

# SA360 WebQuery to BigQuery Exporter

## Background
Large SA360 customers want to build custom reports by combining their 1st party data with paid-search data.

The Easiest way to achieve that is by combining the data in BigQuery.
There are two ways to programmatically import SA360 data into BigQuery
1. [API](https://developers.google.com/search-ads/v2/how-tos/reporting)
2. [Web Query](https://support.google.com/searchads/answer/2870738?hl=en)

WebQuery makes reporting easier compared to the API (with less steps), as it allows adding additional entity data (e.g. Business data) in the report, which makes the report simple as compared to an API where this stitching has to be done in a user's program.

### How does it work?
The tool uses [Service account](https://cloud.google.com/iam/docs/service-accounts) credentials to extract Reports from SA360 and also send data into BigQuery.
First the system extracts the Report (in XML format) from SA360 and converts it into CSV on the fly (using SAX parsing), this file is then staged (copied) to GCS, followed by calling BQ API to `load` the data into a separate table.

> Support for [User account](https://cloud.google.com/docs/authentication/end-user) credentials has been dropped due to security issues arising from storing user credentials locally.

## Video Tutorial
 [![Part-1](https://img.youtube.com/vi/xEMe5CRy6BQ/0.jpg)](https://www.youtube.com/watch?v=xEMe5CRy6BQ)

## Usage

### Create Google Cloud Project
1.  Create a Google Cloud Project and enable billing.
1.  [Enable APIs](https://console.cloud.google.com/flows/enableapi?apiid=doubleclicksearch,bigquery.googleapis.com,storage.googleapis.com).
1.  [Create a Service Account](https://cloud.google.com/iam/docs/creating-managing-service-accounts#creating) to be used for running this project.
1.  Grant the service account permissions
    1. SA360 permission to retrieve webquery report
    1. BigQuery and Storage permission
1.  Set Variables
    ```shell
    export PROJECT_ID="<google-cloud-project-id>"
    export GCS_BUCKET_NAME="<name-of-cloud-storage-bucket>"
    export BIGQUERY_DATASET_NAME="<name-of-dataset>"
    export SERVICE_ACCOUNT_NAME="<service-account-name>"
    export COMPUTE_ENGINE_INSTANCE_NAME="<compute-instance-name>"    
    ```

### Create Resources
1.  Create Cloud Storage bucket to stage the reports.
      ```shell      
      gsutil mb -p ${PROJECT_ID} "gs://${GCS_BUCKET_NAME}" 
      ```

1.  Crate BigQuery dataset to store the reports
    ```shell
    bq mk --project_id="${PROJECT_ID}" ${BIGQUERY_DATASET_NAME}
    ```

1.  Create Service account
    ```shell
    gcloud iam service-accounts create ${SERVICE_ACCOUNT_NAME} \
    --description="Service account to run SA360 webquery to BigQuery"    
    ```

1.  Create [Compute Engine](https://cloud.google.com/compute) instance, [Set default zone and region](https://cloud.google.com/compute/docs/instances/create-start-instance#before-you-begin)
    ```shell
    gcloud compute instances create ${COMPUTE_ENGINE_INSTANCE_NAME} \
    --service-account="${SERVICE_ACCOUNT_NAME}@${PROJECT_ID}.iam.gserviceaccount.com" \
    --scopes=https://www.googleapis.com/auth/cloud-platform,https://www.googleapis.com/auth/doubleclicksearch \
    --image-project debian-cloud \
    --image-family debian-10
    ```
    
    > Ensure that the user/serviceAccount has at least **READ** permissions for SA360 and **EDITOR** Permissions for BigQuery.        

### Compile and run
1.  Create a [Configuration file (csv)](#csv-file-format) with specified headers. (consider `sample-config.csv` as a reference)

1.  Compile and package source code into an executable JAR. 
    ```shell
    ./gradlew clean fatJar
    ```
1.  Copy the fatJar to the Compute Engine instance.
    ```shell
    gcloud compute scp build/libs/dswqtobq-1.1-all.jar ${COMPUTE_ENGINE_INSTANCE_NAME}:~/
    gcloud compute scp <configuration-csv-file> ${COMPUTE_ENGINE_INSTANCE_NAME}:~/        
    ```

1.  SSH into the Compute Engine instance 
    ```shell
    gcloud compute ssh ${COMPUTE_ENGINE_INSTANCE_NAME}
    ```
    >  Install Java 11 on the VM if required: `sudo apt install -y openjdk-11-jdk`

1.  Run the jar file
    ```shell
    # run the JAR file by specifying the configuraiton file as first parameter
    java -jar dswqtobq-1.1-all.jar <location of configuration CSV file> <local-report-temp-folder>
    ```
    > You can schedule to run it automatically using cron, after this step.

### CSV File Format
The CSV configuration file must contain following headers, The order does not matter.
*  `gcsBucketName` - The GCS Bucket to be used for staging CSV file for BQ upload.
*  `projectId` - GCP Project Id to use for billing as well as for BQ Table location.
*  `datasetId` - BQ Dataset id/name belonging to given _projectId_.
*  `tableId` - Prefix to be used for the BigQuery Table
*  `webQueryUrl` - SearchAds 360 WebQuery link
