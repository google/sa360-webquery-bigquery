# SA360 WebQuery to BigQuery Exporter

## Background
Large SA360 customers want to build custom reports by combining their 1st party data with paid-search data.

Easiest way to achieve that is by combining the data in BigQuery.
There are two ways to programmatically import SA360 data into BigQuery
1. [API](https://developers.google.com/search-ads/v2/how-tos/reporting)
2. [Web Query](https://support.google.com/searchads/answer/2870738?hl=en)

WebQuery makes reporting easier compared to the API (with less steps), as it allows adding additional entity data (e.g. Business data) in the report, which makes the report simple as compared to an API where this stitching has to be done in a user's program.

### How does it work?
The tool requires the user's credential to extract Reports from SA360 and also send data into BigQuery.

First the system extracts the Report (in XML format) from SA360 and converts it into CSV on the fly (using SAX parsing), this file is then staged (copied) to GCS, followed by calling BQ API to `load` the data into a separate table.

## Usage
1.  Login to your Google Cloud project and generate credentials for an Installed Application ([Details](https://developers.google.com/identity/protocols/OAuth2#installed))
2.  Update `src/resources/client_secrets.json` with Client-Id and Client secret generated from your Cloud Project.
3.  Compile and package source code into an executable JAR. <br>`mvn clean compile assembly:single`
4.  Create a [Configuration file (csv)](#csv-file-format) with specified headers. (consider `sample-config.csv` as a reference)
5.  Copy the jar file to the final executable location `cp target/dswqtobq-1.0-SNAPSHOT-jar-with-dependencies.jar ~/`
6.  Run the jar file as
```
# go to your executable location
cd ~ 
# run the JAR file by specifying the configuraiton file as first parameter
java -jar dswqtobq-1.0-SNAPSHOT-jar-with-dependencies.jar <location of configuration CSV file>
```

7.  This will prompt you to Login using your browser and store the credentials for next time, 
  If you are running this application on a VM/terminal, you can copy/paste the URL to your browser and paste the code back on the command line prompt.

After this step you can schedule to run it automatically using cron.

> Ensure that the user has at least **READ** permissions for SA360 and **EDITOR** Permissions for BigQuery.

<br>

> _Running on Google AppEngine or Google Cloud Functions is discouraged as WebQuery files can be quite large._

### CSV File Format
The CSV configuration file must contain following headers, The order does not matter.
*  `gcsBucketName` - The GCS Bucket to be used for staging CSV file for BQ upload.
*  `projectId` - GCP Project Id to use for billing as well as for BQ Table location.
*  `datasetId` - BQ Dataset id/name belonging to given _projectId_.
*  `tableId` - Prefix to be used for the BigQuery Table
*  `webQueryUrl` - SearchAds 360 WebQuery link
