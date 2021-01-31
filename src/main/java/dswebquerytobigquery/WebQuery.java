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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.auth.oauth2.UserCredentials;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A Service to denote and read SA360's WebQuery.
 */
public class WebQuery {

  private final String queryUrl;
  private final String reportId;
  private final UserCredentials credential;

  public WebQuery(String queryUrl, UserCredentials credential) {
    this.queryUrl = checkNotNull(queryUrl);
    this.reportId = extractReportId(queryUrl);
    this.credential = credential;
  }

  /**
   * Returns a reader to the given WebQuery by opening a Socket connection and creating an {@link
   * InputStream}.
   *
   */
  public WebQueryReader read() {
    return new WebQueryReader(credential);
  }

  public String getReportId() {
    return reportId;
  }

  public String getQueryUrl() {
    return queryUrl;
  }

  private static String extractReportId(String queryUrl) {
    Matcher matcher = Pattern.compile("rid=(\\d+)").matcher(queryUrl);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  /**
   * A service to open a connection to SA360 for reading the webquery.
   */
  public class WebQueryReader {

    private final UserCredentials credential;

    private WebQueryReader(UserCredentials credential) {
      this.credential = checkNotNull(credential);
    }

    /**
     * Returns a stream by opening a connection to the WebQuery URL by providing OAuth token in the
     * header.
     *
     * @throws IOException if credentials or URL is incorrect.
     */
    InputStream getStream() throws IOException {

      HttpURLConnection conn = (HttpURLConnection) new URL(queryUrl).openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Authorization", "Bearer " + credential.getAccessToken().getTokenValue());

      return conn.getInputStream();
    }

    /**
     * Parses and outputs the WebQuery HTML to a given CSV File.
     */
    public void writeAsCsv(File outputFile) throws IOException, SAXException {
      try (Reader htmlFileReader = new InputStreamReader(getStream(), StandardCharsets.UTF_8)) {
        Parser xmlParser = new Parser();
        xmlParser.setContentHandler(Html2CsvParseHandler.forFile(outputFile));
        xmlParser
            .parse(new InputSource(htmlFileReader));
      }
    }
  }
}
