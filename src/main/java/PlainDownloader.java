import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PlainDownloader {
    public void download() {
        String url = "http://localhost:8080/";
        Set<String> oldStorageFiles = getFilenameSet(url + "oldStorage/files");
        Set<String> newStorageFiles = getFilenameSet(url + "newStorage/files");

        for (String filename : newStorageFiles)
            if (oldStorageFiles.contains(filename))
                deleteFileFromServer(url + "oldStorage/files/" + filename);

        for (String filename : oldStorageFiles) {
            File file = getFileFromServer(url + "oldStorage/files/", filename);
            uploadFileToServer(url + "newStorage/files", file);
            deleteFileFromServer(url + "oldStorage/files/" + filename);
        }

    }

    private void deleteFileFromServer(String url) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpDelete httpDelete = new HttpDelete(url);
        int statusCode = 0;
        do
            try {
                CloseableHttpResponse response = httpClient.execute(httpDelete);
                statusCode = response.getStatusLine().getStatusCode();
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        while (statusCode != 200 && statusCode != 404);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void uploadFileToServer(String url, File file) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        entityBuilder.addBinaryBody("file", file);
        HttpEntity entity = entityBuilder.build();
        httpPost.setEntity(entity);
        int statusCode = 0;
        do {
            try {
                CloseableHttpResponse response = httpClient.execute(httpPost);
                statusCode = response.getStatusLine().getStatusCode();
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } while (statusCode != 200);
        file.delete();
    }

    private File getFileFromServer(String url, String filename) {
        String body = getResponseEntityText(url + filename);
        File file = null;
        try {
            file = new File(filename);
            FileWriter fw = new FileWriter(file);
            fw.write(body);
            fw.close();
        } catch (IOException ignored) {
        }
        return file;
    }

    private String getResponseEntityText(String url) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String responseText = null;
        int statusCode = 0;
        do
            try {
                CloseableHttpResponse httpResponse = httpClient.execute(new HttpGet(url));
                statusCode = httpResponse.getStatusLine().getStatusCode();
                responseText = EntityUtils.toString(httpResponse.getEntity());
                httpResponse.close();
            } catch (IOException ignored) {
            }
        while (statusCode != 200);
        return responseText;

    }

    private Set<String> getFilenameSet(String url) {
        String body = getResponseEntityText(url);
        Gson gson = new Gson();
        String[] fromJson = gson.fromJson(body, String[].class);
        return new HashSet<>(Arrays.asList(fromJson));
    }

}
