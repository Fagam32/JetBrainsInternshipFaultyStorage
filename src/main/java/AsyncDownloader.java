import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

import static java.net.HttpURLConnection.HTTP_OK;

public class AsyncDownloader {
    private LinkedBlockingQueue<String> downloadQueue = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<String> uploadQueue = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<String> deleteQueue = new LinkedBlockingQueue<>();

    public void download() {
        downloadQueue.addAll(getFilenameSet());

        ExecutorService service = Executors.newFixedThreadPool(3);

        Future task1 = null;
        Future task2 = null;
        Future task3 = null;
        long start = System.currentTimeMillis();
        do {
            if (task1 == null || task1.isCancelled() || task1.isDone()) {
                task1 = service.submit(this::fileDownloader);
            }
            if (task2 == null || task2.isCancelled() || task2.isDone()) {
                task2 = service.submit(this::fileUploader);
            }
            if (task3 == null || task3.isCancelled() || task3.isDone()) {
                task3 = service.submit(this::fileDeleter);
            }
            if ((System.currentTimeMillis() - start) / 1000 > 3) {
                System.out.println("Download left: " + downloadQueue.size()
                        + "\nUpload left: " + uploadQueue.size()
                        + "\nDelete left: " + deleteQueue.size());
                start = System.currentTimeMillis();
            }
        } while (!deleteQueue.isEmpty() || !uploadQueue.isEmpty() || !downloadQueue.isEmpty());
        service.shutdown();
    }

    private void fileDeleter() {
        try (CloseableHttpAsyncClient client = HttpAsyncClients.createDefault()) {
            client.start();
            ArrayList<HttpDelete> links = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                if (!deleteQueue.isEmpty()) {
                    HttpDelete link = new HttpDelete("http://localhost:8080/oldStorage/files/" + deleteQueue.take());
                    links.add(link);
                } else break;
            }
            CountDownLatch latch = new CountDownLatch(links.size());
            for (HttpDelete request : links) {
                String filename = request.getURI().toString()
                        .substring(request.getURI().toString().lastIndexOf("/") + 1);
                client.execute(request, new DeleteHandler(latch, filename));
            }
            latch.await();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void fileUploader() {
        try (CloseableHttpAsyncClient client = HttpAsyncClients.createDefault()) {
            client.start();
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
            HttpPost link = new HttpPost();

            if (!uploadQueue.isEmpty()) {
                String filename = uploadQueue.take();
                HttpEntity fileEntity = entityBuilder.addBinaryBody("file", new File(filename)).build();
                link.setEntity(fileEntity);
                link.addHeader("filename", filename);
                link.setURI(URI.create("http://localhost:8080/newStorage/files/"));
            }

            CountDownLatch latch = new CountDownLatch(1);


            String filename = link.getFirstHeader("filename").getValue();
            client.execute(link, new UploadHandler(latch, filename));

            latch.await();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void fileDownloader() {
        try (CloseableHttpAsyncClient client = HttpAsyncClients.custom().build()) {
            client.start();
            ArrayList<HttpGet> links = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                if (!downloadQueue.isEmpty())
                    links.add(new HttpGet("http://localhost:8080/oldStorage/files/" + downloadQueue.take()));
                else break;
            }

            CountDownLatch latch = new CountDownLatch(links.size());
            for (HttpGet request : links) {
                String filename = request.getURI().toString()
                        .substring(request.getURI().toString().lastIndexOf("/") + 1);
                client.execute(request, new DownloadHandler(latch, filename));
            }
            latch.await();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private Set<String> getFilenameSet() {
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
        return Collections.synchronizedSet(new HashSet<>(Arrays.asList(fromJson)));
    }

    private class DownloadHandler implements FutureCallback<HttpResponse> {

        CountDownLatch latch;
        String filename;

        DownloadHandler(CountDownLatch latch, String filename) {
            this.latch = latch;
            this.filename = filename;
        }

        @Override
        public void completed(HttpResponse result) {
            latch.countDown();
            int statusCode = result.getStatusLine().getStatusCode();
            try {
                if (statusCode != HTTP_OK)
                    downloadQueue.put(filename);
                else {
                    File file = new File(filename);
                    FileWriter fw = new FileWriter(file);
                    fw.write(EntityUtils.toString(result.getEntity()));
                    fw.close();
                    if (!file.exists())
                        downloadQueue.put(filename);

                    uploadQueue.put(filename);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }


        @Override
        public void failed(Exception ex) {
            latch.countDown();
            try {
                downloadQueue.put(filename);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void cancelled() {
            latch.countDown();
            try {
                downloadQueue.put(filename);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class UploadHandler implements FutureCallback<HttpResponse> {
        CountDownLatch latch;
        String filename;

        public UploadHandler(CountDownLatch latch, String filename) {
            this.latch = latch;
            this.filename = filename;
        }


        @Override
        public void completed(HttpResponse result) {
            latch.countDown();
            try {
                int statusCode = result.getStatusLine().getStatusCode();
                if (statusCode != HTTP_OK)
                    uploadQueue.put(filename);
                else {
                    deleteQueue.put(filename);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void failed(Exception ex) {
            latch.countDown();
            try {
                uploadQueue.put(filename);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void cancelled() {
            latch.countDown();
            try {
                uploadQueue.put(filename);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private class DeleteHandler implements FutureCallback<HttpResponse> {
        CountDownLatch latch;
        String filename;

        public DeleteHandler(CountDownLatch latch, String filename) {
            this.latch = latch;
            this.filename = filename;
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        @Override
        public void completed(HttpResponse result) {
            latch.countDown();
            int statusCode = result.getStatusLine().getStatusCode();
            try {
                if (statusCode != HTTP_OK)
                    deleteQueue.put(filename);
                else {
                    File file = new File(filename);
                    file.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void failed(Exception ex) {
            latch.countDown();
            try {
                deleteQueue.put(filename);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void cancelled() {
            latch.countDown();
            try {
                deleteQueue.put(filename);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
