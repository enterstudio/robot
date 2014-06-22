package com.robot;

/**
 * General interface for a storage/store
 *
 * @param <T> the type of data the storage will store
 * @author fernandohur
 */
public interface Storage<T> {

    /**
     * Saves a data object in the store.
     *
     * @param data the object to be saved
     */
    public void save(T data);

    /**
     * Loads the data object stored in this store. This can return null.
     *
     * @return the stored data object.
     */
    public T load();

    /**
     * removes all data associated with this store.
     * A typical subsequent call of {@link #load()} should return null, although this behavior
     * is not necessary.
     */
    public void clear();

    /**
     * @return returns true if the stored object is null, false otherwise
     */
    public boolean isStored();
}
