package thor.util;

/**
 * A task to be executed through the {@link LockService}.
 */

public interface LockedTask<T>
{
    /** 
     * Called by the {@link LockService} to execute the implemented task.
     * It is guaranteed that this method will not be called by multiple threads simultaneously for the same lock ID provided to the {@link LockService}.
     *
     * @return       the object you want to return to the caller of the {@link LockService}.
     */

    public T execute() throws Exception;
}