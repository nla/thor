package thor.storage;

public class StorableNotFoundException extends Exception
{
    private static final long serialVersionUID = 1L;

    /**
     * Thrown by {@link thor.storage.file.FileStorageService} when the requested {@link thor.Storable} does not exist in the data store.
     */

    public StorableNotFoundException()
    {
        super();
    }

    public StorableNotFoundException(String message)
    {
        super(message);
    }
}
