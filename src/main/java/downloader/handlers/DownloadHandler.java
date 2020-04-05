package downloader.handlers;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DownloadHandler extends AbstractRequestHandler {

    public MyFile handle(MyFile file) {
        if (file.isDownloaded()) {
            return handleNext(file);
        }
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            CloseableHttpResponse response = client.execute(new HttpGet("http://localhost:8080/oldStorage/files/" + file.getFilename()));
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                File trueFile = new File(file.getFilename());
                FileWriter fw = new FileWriter(trueFile);
                fw.write(EntityUtils.toString(response.getEntity()));
                fw.close();
                if (trueFile.exists()) {
                    file.setRealFile(trueFile);
                    file.setDownloaded(true);
                } else return handleNext(file);
            } else return handleNext(file);
            response.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return handleNext(file);
    }
}
