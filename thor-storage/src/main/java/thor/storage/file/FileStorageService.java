package thor.storage.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;

import thor.Storable;
import thor.storage.StorableNotFoundException;
import thor.storage.StorageReceiver;
import thor.util.LockService;
import thor.util.LockedTask;

/**
 * A service for storing and retrieving {@link thor.Storable} objects.
 * The service stores these objects on the file system using Java serialization.
 * 
 * Files are stored in the directory passed into the constructor. Each object is stored in its own file.
 * 
 * It is safe to use an instance of this class from multiple threads.
 * It is highly recommended that access to data within the directory specified is restricted to a single instance of this class, so that thread safety can be assured.
 * It is safe to manually manipulate the files and/or folder location while there is no application writing to it.
 */

public class FileStorageService<T extends Storable>
{
    private static final String STORABLE_ID_PATTERN = "[a-zA-Z0-9_\\-]+";

    private String basePath;
    private Path basePathP;
    private String tempPath;
    private LockService lockService;

    /** 
     * Initialise an instance with the specified storage directory.
     *
     * @param basePath       the directory in which to store {@link thor.Storable} objects
     * @param tempPath       the directory in which to story temporary files (used to temporarily store saved files before atmomically moving them into the basePath)
     */

    public FileStorageService(String basePath, String tempPath) throws Exception
    {
        this.basePath = basePath;
        this.tempPath = tempPath;
        this.lockService = new LockService();
        initialize();
    }

    private void initialize() throws Exception
    {
        File file = new File(basePath);
        
        if(!file.exists())
        {
            file.mkdirs();
        }
        
        basePathP = file.toPath();
        
        file = new File(tempPath);

        if(!file.exists())
        {
            file.mkdirs();
        }
    }

    private void validateId(String id) throws Exception
    {
        if(!id.matches(STORABLE_ID_PATTERN) || id.startsWith("_"))
        {
            throw new Exception("Storable IDs can only contain letters, numbers, hyphens, and underscores and must not start with an underscore.");
        }
    }

    /** 
     * Deletes the object stored with the specified ID from the data store.
     *
     * @param id       the ID used to store the object that should be deleted.
     */

    public void delete(String id) throws Exception
    {
        lockService.runLockedTask(id, new LockedTask<Void>(){
            public Void execute() throws Exception
            {
                File file = new File(basePath+"/"+id);
                file.delete();
                return null;
            }
        });
    }

    /** 
     * Stores an object with then given ID. The ID provided can be referenced later to retrieve or delete the object.
     * If an object already exists in the data store with the given ID, it will be overwritten.
     *
     * @param id       the ID of the object to be stored.
     */

    public void store(String id, T storable) throws Exception
    {
        validateId(id);

        lockService.runLockedTask(id, new LockedTask<Void>(){
            public Void execute() throws Exception
            {
                File tempFile = new File(tempPath+"/"+id);
                
                try(ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(tempFile)))
                {
                    out.writeObject(storable);
                    out.flush();
                }
                
                Files.move(tempFile.toPath(), new File(basePath+"/"+id).toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                return null;
            }
        });
    }

    /** 
     * Retrieves an object stored from the data store.
     *
     * @param id                             the ID of the object to be retrieved.
     * @return                               the object requested from the data store.
     * @throws StorableNotFoundException     if the requested object was not found in the data store.
     */

    public T load(String id) throws StorableNotFoundException, Exception
    {
        return lockService.runLockedTask(id, new LockedTask<T>(){
            @SuppressWarnings("unchecked")
            public T execute() throws Exception
            {
                File file = new File(basePath+"/"+id);

                if(!file.exists())
                {
                    throw new StorableNotFoundException("Storable '"+id+"' does not exist.");
                }

                try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(file)))
                {
                    return (T)in.readObject();
                }
            }
        });
    }

    /** 
     * Retrieves all objects from the data store from the position starting with the given starting index to a maximum of times specified.
     *
     * @param from       the starting index.
     * @param count      the maximum number of items to retrieve. A value of zero is counted as an unlimited number of retrieved items.
     * @return           a list of all items retrieved within the confines of the given parameters.
     */

    public List<T> load(int from, int count) throws Exception
    {
        List<T> items = new ArrayList<T>(count);
        load(from, count, items);
        return items;
    }

    /** 
     * Retrieves all objects from the data store from the position starting with the given starting index to a maximum of times specified.
     * Adds all objects retrieved to the specified list.
     *
     * @param from           the starting index
     * @param count          the maximum number of items to retrieve. A value of zero is counted as an unlimited number of retrieved items.
     * @param container      the list into which the items will be added.
     */

    public void load(int from, int count, List<T> container) throws Exception
    {
        load(from, count, new StorageReceiver<T>(){
            public void take(T item)
            {
                container.add(item);
            }
        });
    }

    /** 
     * Retrieves all objects from the data store from the position starting with the given starting index to a maximum of times specified.
     * Items retrieved will be given to the specified {@link thor.storage.StorageReceiver}.
     *
     * @param from           the starting index
     * @param count          the maximum number of items to retrieve. A value of zero is counted as an unlimited number of retrieved items.
     * @param receiver       the object receiving the items.
     */

    public void load(int from, int count, StorageReceiver<T> receiver) throws Exception
    {
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(basePathP))
        {
            int max = from+count;
            int i = 0;

            for(Path entry: stream)
            {
                if(from<=i)
                {
                    try
                    {
                        receiver.take(load(entry.getFileName().toString()));  
                    }
                    catch(StorableNotFoundException e){} // For cases where items may be deleted during this iteration
                }

                i++;

                if(count>0 && i>=max)
                {
                    return;
                }
            }
        }
    }

    /** 
     * Retrieves all objects from the data store.
     *
     * @return           a list of all items in the data store.
     */

    public List<T> loadAll() throws Exception
    {
        List<T> items = new ArrayList<T>(size());
        load(0, 0, items);
        return items;
    }

    /** 
     * Retrieves all objects from the data store.
     * Adds all objects retrieved to the specified list.
     *
     * @param container      the list into which the items will be added.
     */

    public void loadAll(List<T> container) throws Exception
    {
        load(0, 0, new StorageReceiver<T>(){
            public void take(T item)
            {
                container.add(item);
            }
        });
    }

    /** 
     * Retrieves all objects from the data store.
     * Items retrieved will be given to the specified {@link thor.storage.StorageReceiver}.
     *
     * @param receiver       the object receiving the items.
     */

    public void loadAll(StorageReceiver<T> receiver) throws Exception
    {
        load(0, 0, receiver);
    }

    /** 
     * Returns whether or not an item exists in the data store with the specified ID.
     *
     * @param id      the ID to check.
     * @return        true if the item is in the data store, or false if it is not.
     */

    public boolean has(String id) throws Exception
    {
        return lockService.runLockedTask(id, new LockedTask<Boolean>(){
            public Boolean execute() throws Exception
            {
                return new File(basePath+"/"+id).exists();
            }
        });
    }

    /** 
     * Returns the total number of items in the data store.
     *
     * @return        the total number of items in the data store.
     */

    @SuppressWarnings("unused")
    public int size() throws Exception
    {
        int count = 0;
        
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(basePathP))
        {
            for(Path entry: stream)
            {
                count++;
            }
        }

        return count;
    }
}
