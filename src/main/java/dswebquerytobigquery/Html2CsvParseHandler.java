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

import static dswebquerytobigquery.WqToBqDataTypeMapper.translateWebQueryTypeToBigQueryType;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.GoogleLogger;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/** SAX parser for WebQuery HTML.
 *
 * Reads the WebQuery HTML and transforms into a CSV File.
 */
public class Html2CsvParseHandler extends DefaultHandler {

  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  private final File outputFile;
  private final String processingDateString =
      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
  private CSVPrinter csvPrinter;
  private ImmutableList.Builder<String> columnTypeInfo;
  private ImmutableList.Builder<String> columnNameInfo;
  private StringBuilder textAccumulator;
  private ImmutableList.Builder<String> rowAccumulator;
  private boolean bodyElementStarted = false;
  private int bodyRowCounter = 0;

  /**
   * Initializes the Parser with Output CSV File location.
   *
   * @param outputFile name and location of the output CSV File to store parsed output.
   */
  private Html2CsvParseHandler(File outputFile) {
    this.outputFile = outputFile;
  }

  /**
   * Factory Meethod to build the parser for a given output file.
   *
   * @param outputFile name and location of the output CSV File to store parsed output.
   * @return the Parser which will store the output to the given File.
   */
  public static Html2CsvParseHandler forFile(File outputFile) {
    return new Html2CsvParseHandler(outputFile);
  }

  private ImmutableList<ColumnHeader> getColumnHeaders() {

    if (bodyElementStarted) {
      List<String> columnTypes = columnTypeInfo.build();
      List<String> columnNames = columnNameInfo.build();

      ImmutableList.Builder<ColumnHeader> columnHeaderBuilder = ImmutableList.builder();

      for (int index = 0; index < columnNames.size(); index++) {
        columnHeaderBuilder
            .add(ColumnHeader.create(columnNames.get(index), columnTypes.get(index)));
      }
      return columnHeaderBuilder.build();
    }

    return ImmutableList.of();
  }

  private void createCsvWriter(String[] headers) throws SAXException {
    try {
      csvPrinter =
          new CSVPrinter(
              new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8),
              CSVFormat.DEFAULT.withHeader(headers));
    } catch (IOException ioexception) {
      throw new SAXException("error creating outputfile", ioexception);
    }
  }

  private void writeRowToCsv(ImmutableList<String> rowElements) throws SAXException {
    try {
      csvPrinter.printRecord(rowElements);
    } catch (IOException ioException) {
      throw new SAXException("error writing row " + rowElements, ioException);
    }
  }

  @Override
  public void startDocument() {
    columnTypeInfo = ImmutableList.builder();
    columnNameInfo = ImmutableList.builder();
  }

  @Override
  public void endDocument() throws SAXException {
    try {
      csvPrinter.flush();
      csvPrinter.close();
    } catch (IOException ioexception) {
      throw new SAXException("error closing file", ioexception);
    }
    logger.atFine().log("headers: %s", getColumnHeaders());
    logger.atInfo().log("parsed rows: %s", bodyRowCounter);
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) {

    // Start collecting Column Type Information
    if (qName.equals("col")) {
      String colClassValue = attributes.getValue("class");
      columnTypeInfo.add(
          (colClassValue == null)
              ? WqToBqDataTypeMapper.DEFAULT_TYPE
              : translateWebQueryTypeToBigQueryType(colClassValue));
    }

    if (qName.equals("th") || qName.equals("td")) {
      textAccumulator = new StringBuilder();
    }

    if (qName.equals("tbody")) {
      bodyElementStarted = true;
    }

    // New Datarow begins
    if (bodyElementStarted && qName.equals("tr")) {
      rowAccumulator = ImmutableList.builder();
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {

    // means colgroup and thead is complete.
    if (qName.equals("colgroup")) {
      // add timestamp col
      columnTypeInfo.add(WqToBqDataTypeMapper.TIMESTAMP);
    }

    if (qName.equals("th")) {
      columnNameInfo.add(formatHeader(textAccumulator.toString()));
    }

    // Start the CSV Writer
    if (qName.equals("thead")) {
      createCsvWriter(
          columnNameInfo
              .add(Constants.REPORT_PULL_TIMESTAMP_COLUMN_NAME) // add partition column
              .build()
              .toArray(new String[0]));
    }

    if (qName.equals("td")) {
      rowAccumulator.add(sanitizeForCsv(textAccumulator.toString()));
    }

    if (bodyElementStarted && qName.equals("tr")) {
      bodyRowCounter++;
      writeRowToCsv(
          rowAccumulator
              .add(processingDateString) // Add partition timestamp to each row
              .build());
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) {
    if (textAccumulator != null) {
      textAccumulator.append(ch, start, length);
    }
  }

  @Override
  public void fatalError(SAXParseException e) throws SAXException {

    try {
      if (csvPrinter != null) {
        csvPrinter.close();
      }
    } catch (IOException ioexecption) {
      System.err.println("error closing file" + ioexecption.getMessage());
    }
    super.fatalError(e);
  }

  private static String formatHeader(String headerValue) {

    if (headerValue == null) {
      return "";
    }

    return headerValue
        .replaceAll("[^A-Za-z0-9_]", "_") // Replace all non-Alphanumeric chars
        .replaceAll("_{2,}", "_") // Replace multiple underscrores with one
        .toLowerCase();
  }

  private static String sanitizeForCsv(String cellData) {
    if (cellData == null || cellData.equals("") || cellData.equals("\"\"")) {
      return "";
    }

    var resultBuilder = new StringBuilder(cellData);
    // Look for doublequotes, escape as necessary.
    var lastIndex = 0;
    while (resultBuilder.indexOf("\"", lastIndex) >= 0) {
      var quoteIndex = resultBuilder.indexOf("\"", lastIndex);
      resultBuilder.replace(quoteIndex, quoteIndex + 1, "\"\"");
      lastIndex = quoteIndex + 2;
    }

    var firstChar = cellData.charAt(0);
    var lastChar = cellData.charAt(cellData.length() - 1);

    if (cellData.contains(",") || // Check for commas
      cellData.contains("\n") ||  // Check for line breaks
      Character.isWhitespace(firstChar) || // Check for leading whitespace.
      Character.isWhitespace(lastChar)) { // Check for trailing whitespace
      resultBuilder.insert(0, "\"").append("\""); // Wrap in doublequotes.
    }
    return resultBuilder.toString();
  }
}
