import downloader.Downloader;

public class Main {

    public static void main(final String[] args) {
//        for (int i = 0; i < 5000; i++) {
//            File file = new File(i + ".txt");
//            file.delete();
//        }
//        AsyncDownloader asyncdownloader = new AsyncDownloader();
//        asyncdownloader.download();
        
        Downloader plainDownloader = new Downloader();
        plainDownloader.start(); //ATTENTION works very slow
    }
}
