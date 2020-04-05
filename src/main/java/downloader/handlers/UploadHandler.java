package downloader.handlers;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

public class UploadHandler extends AbstractRequestHandler {

    public MyFile handle(MyFile file) {
        if (file.isDownloaded() && !file.isUploaded())
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpPost request = new HttpPost("http://localhost:8080/newStorage/files");
                MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
                entityBuilder.addBinaryBody("file", file.getRealFile());
                request.setEntity(entityBuilder.build());
                CloseableHttpResponse response = client.execute(request);
                int statusCode = response.getStatusLine().getStatusCode();
                response.close();
                if (statusCode == 200 || statusCode == 409) {
                    file.setUploaded(true);
                    return handleNext(file);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        return handleNext(file);
    }
}
