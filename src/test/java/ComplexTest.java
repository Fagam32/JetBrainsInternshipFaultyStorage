import com.google.gson.Gson;
import downloader.handlers.DeleteHandler;
import downloader.handlers.DownloadHandler;
import downloader.handlers.MyFile;
import downloader.handlers.UploadHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.Assert.*;

public class ComplexTest {
    @Test
    public void uploadTest() {
        for (int i = 0; i < 10; i++) {
            String filename = ((int) (Math.random() * 100_000 % 5000) + 1) + ".txt";

            File downloaded = getFile(filename);

            Set<String> before = getNewFilenameSet();
            before.add(filename);
            MyFile myFile = new MyFile(filename);
            myFile.setDownloaded(true);
            myFile.setRealFile(downloaded);

            UploadHandler handler = new UploadHandler();
            MyFile testFile = handler.handle(myFile);
            if (!testFile.isUploaded()) {
                i--;
                continue;
            }

            Set<String> after = getNewFilenameSet();
            assertEquals(before, after);
            downloaded.delete();
        }
    }

    @Test
    public void downloadTest() {
        for (int i = 0; i < 10; i++) {
            Set<String> files = getOldFilenameSet();
            String filename;
            do {
                filename = ((int) (Math.random() * 100_000 % 5000) + 1) + ".txt";
            } while (!files.contains(filename));

            HttpGet request = new HttpGet("http://localhost:8080/oldStorage/files/" + filename);
            File baseFile = new File(filename);
            int statusCode = 0;
            do {
                try (CloseableHttpClient client = HttpClients.createDefault()) {
                    CloseableHttpResponse response = client.execute(request);
                    statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == 200) {
                        FileWriter fw = new FileWriter(baseFile);
                        fw.write(EntityUtils.toString(response.getEntity()));
                        fw.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } while (statusCode != 200);

            DownloadHandler handler = new DownloadHandler();
            MyFile myFile = handler.handle(new MyFile(filename));
            if (!myFile.isDownloaded())
                continue;
            File testFile = myFile.getRealFile();
            assertEquals(baseFile, testFile);
            baseFile.delete();
        }
    }

    @Test
    public void deleteTest() {
        for (int i = 0; i < 10; i++) {
            Set<String> files = getOldFilenameSet();
            String filename;
            do {
                filename = ((int) (Math.random() * 100_000 % 5000) + 1) + ".txt";
            } while (!files.contains(filename));

            File file = getFile(filename);
            MyFile myFile = new MyFile(filename);
            myFile.setDownloaded(true);
            myFile.setUploaded(true);
            myFile.setRealFile(file);

            Set<String> before = getNewFilenameSet();
            before.remove(filename);

            DeleteHandler handler = new DeleteHandler();
            MyFile afterFile = new MyFile(filename);

            for (; ; )
                if (afterFile.isDeleted())
                    break;
                else
                    afterFile = handler.handle(myFile);

            Set<String> after = getNewFilenameSet();

            assertFalse((new File(filename)).exists());
            assertNull(myFile.getRealFile());
            assertEquals(before, after);
        }
    }

    private Set<String> getNewFilenameSet() {
        String url = "http://localhost:8080/newStorage/files";
        String body = null;
        int statusCode;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            do {
                CloseableHttpResponse response = httpClient.execute(new HttpGet(url));
                statusCode = response.getStatusLine().getStatusCode();
                body = EntityUtils.toString(response.getEntity());
                response.close();
            } while (statusCode != HTTP_OK);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Gson gson = new Gson();
        String[] fromJson = gson.fromJson(body, String[].class);
        return new HashSet<>(Arrays.asList(fromJson));
    }

    private Set<String> getOldFilenameSet() {
        String url = "http://localhost:8080/oldStorage/files";
        String body = null;
        int statusCode;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            do {
                CloseableHttpResponse response = httpClient.execute(new HttpGet(url));
                statusCode = response.getStatusLine().getStatusCode();
                body = EntityUtils.toString(response.getEntity());
                response.close();
            } while (statusCode != HTTP_OK);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Gson gson = new Gson();
        String[] fromJson = gson.fromJson(body, String[].class);
        return new HashSet<>(Arrays.asList(fromJson));
    }

    private File getFile(String filename) {
        HttpGet request = new HttpGet("http://localhost:8080/oldStorage/files/" + filename);
        File downloaded = new File(filename);
        int statusCode = 0;
        do {
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                CloseableHttpResponse response = client.execute(request);
                statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    FileWriter fw = new FileWriter(downloaded);
                    fw.write(EntityUtils.toString(response.getEntity()));
                    fw.close();
                }
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } while (statusCode != 200);
        return downloaded;
    }

}
