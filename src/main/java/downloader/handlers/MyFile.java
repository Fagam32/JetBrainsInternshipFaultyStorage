package downloader.handlers;

import java.io.File;

public class MyFile {
    private String filename;
    private boolean isDownloaded, isUploaded, isDeleted;
    private File realFile;

    public MyFile(String filename) {
        isDownloaded = false;
        isUploaded = false;
        isDeleted = false;
        realFile = null;
        this.filename = filename;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public String getFilename() {
        return filename;
    }

    public File getRealFile() {
        return realFile;
    }

    public void setRealFile(File realFile) {
        this.realFile = realFile;
    }

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean downloaded) {
        isDownloaded = downloaded;
    }

    public boolean isUploaded() {
        return isUploaded;
    }

    @Override
    public String toString() {
        return "MyFile{" +
                "filename='" + filename + '\'' +
                ", isDownloaded=" + isDownloaded +
                ", isUploaded=" + isUploaded +
                ", isDeleted=" + isDeleted +
                ", realFile=" + realFile +
                '}';
    }

    public void setUploaded(boolean uploaded) {
        isUploaded = uploaded;
    }
}
