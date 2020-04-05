import com.google.gson.Gson;
import downloader.Downloader;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.Assert.assertTrue;

public class MainDownloaderTest {
    Set<String> oldStorageFiles;
    Set<String> newStorageFiles;

    @Before
    public void setup() {
        String url = "http://localhost:8080/oldStorage/files";
        String body = null;
        int statusCode;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            do {
                HttpResponse response = httpClient.execute(new HttpGet(url));
                statusCode = response.getStatusLine().getStatusCode();
                body = EntityUtils.toString(response.getEntity());
            } while (statusCode != HTTP_OK);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Gson gson = new Gson();
        String[] tmp = gson.fromJson(body, String[].class);
        oldStorageFiles = new HashSet<>(Arrays.asList(tmp));
    }

    @Test
    public void DownloadTest() {
        Downloader downloader = new Downloader();
        downloader.start();

        String url = "http://localhost:8080/newStorage/files";
        String body = null;
        int statusCode;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            do {
                HttpResponse response = httpClient.execute(new HttpGet(url));
                statusCode = response.getStatusLine().getStatusCode();
                body = EntityUtils.toString(response.getEntity());
            } while (statusCode != HTTP_OK);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Gson gson = new Gson();
        String[] tmp = gson.fromJson(body, String[].class);
        newStorageFiles = new HashSet<>(Arrays.asList(tmp));

        for (String oldName : oldStorageFiles) {
            assertTrue("New storage doesn't contain" + oldName, newStorageFiles.contains(oldName));
        }
    }
}
