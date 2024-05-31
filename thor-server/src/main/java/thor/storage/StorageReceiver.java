package thor.storage;

import thor.Storable;

/**
 * Implementations of this interface receive a variable number of items from a source.
 */

public interface StorageReceiver<T extends Storable>
{
    /** 
     * Called when a source is giving the implementation an item.
     *
     * @param item       the item given
     */

    public void take(T item);
}