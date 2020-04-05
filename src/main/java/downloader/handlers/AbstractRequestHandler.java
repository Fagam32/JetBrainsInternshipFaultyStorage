package downloader.handlers;

public abstract class AbstractRequestHandler {
    private AbstractRequestHandler next;

    public AbstractRequestHandler linkWith(AbstractRequestHandler next) {
        this.next = next;
        return next;
    }

    public abstract MyFile handle(MyFile file);

    protected MyFile handleNext(MyFile file) {
        if (next == null) {
            return file;
        } else
            return next.handle(file);
    }
}
