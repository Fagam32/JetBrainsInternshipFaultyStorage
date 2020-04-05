package downloader;

import com.google.gson.Gson;
import downloader.handlers.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

import static java.net.HttpURLConnection.HTTP_OK;

public class Downloader {
    private Queue<MyFile> myFilesQueue;
    private AbstractRequestHandler handler;

    public Downloader() {
        init();
    }

    public void start() {
        while (!myFilesQueue.isEmpty()) {
            MyFile file = handler.handle(myFilesQueue.poll());
            if (file.isDownloaded() && file.isUploaded() && file.isDeleted())
                myFilesQueue.remove(file);
            else {
                myFilesQueue.add(file);
                System.out.println("Getting back: " + file);
            }
            if (myFilesQueue.size() % 100 == 0)
                System.out.println(myFilesQueue.size());
        }
    }

    private void init() {
        myFilesQueue = new ArrayDeque<>();
        getFilenameSet();
        handler = new DownloadHandler();
        handler.linkWith(new UploadHandler()).linkWith(new DeleteHandler());
    }

    private void getFilenameSet() {
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
        for (String s : fromJson)
            myFilesQueue.add(new MyFile(s));
    }
}
