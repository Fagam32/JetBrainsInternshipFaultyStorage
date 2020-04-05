package downloader.handlers;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

public class DeleteHandler extends AbstractRequestHandler {

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public MyFile handle(MyFile file) {
        if (file.isDownloaded() && file.isUploaded() && !file.isDeleted()) {
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpDelete request = new HttpDelete("http://localhost:8080/oldStorage/files/" + file.getFilename());
                CloseableHttpResponse response = client.execute(request);
                int statusCode = response.getStatusLine().getStatusCode();
                response.close();
                if (statusCode == 200) {
                    file.getRealFile().delete();
                    file.setRealFile(null);
                    file.setDeleted(true);
                    return handleNext(file);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return handleNext(file);
    }
}
