package com.robot;

import java.util.Collection;

/**
 * Created by fernandinho on 6/24/14.
 */
public class StorableCollection<T> extends BaseCollection<T> {

    protected CollectionStorage<T> storage;

    /**
     * saves the collections objects using the {@link BaseCollection}'s storage
     */
    public void save() {
        storage.save(this, new Callback<Void>() {
            @Override
            public void onFinish(Void data) {
                notifySave();
            }
        });
    }

    /**
     * Loads and adds all elements obtained by the BaseCollection's storage.
     * A {@link ModelChangedEvent} is guaranteed to be thrown.
     */
    public void load() {
        load(new Callback<Collection<T>>() {
            @Override
            public void onFinish(Collection<T> data) {
            }
        });
    }

    public void load(final Callback<Collection<T>> cb) {
        storage.load(new Callback<Collection<T>>() {
            @Override
            public void onFinish(Collection<T> data) {
                data = afterLoad(data);
                addAll(data, true);
                cb.onFinish(data);
            }
        });
    }

    /**
     * Similar to {@link #load()} but is guaranteed to run synchronous.
     */
    public void loadSync() {
        Collection<T> data = storage.loadSync();
        data = afterLoad(data);
        addAll(data, true);
    }

    /**
     * Allows operations on a loaded element before it has been added. This method by default does not
     * do anything i.e it just returns the elements parameter but it allows subclasses to do
     * additional operations on the underlying collection.
     *
     * @param elements
     */
    public Collection<T> afterLoad(Collection<T> elements) {
        return elements;
    }

    /**
     * Publishes a {@link ModelStoredEvent}
     */
    public void notifySave() {
        notifyEvent(getModelStoredEventInstance());
    }

    /**
     * Modifies the storage medium for this {@link BaseCollection}
     *
     * @param storage
     */
    public void setStorage(CollectionStorage<T> storage) {
        this.storage = storage;
    }

    /**
     * Subclasses that want to publish more specific events can override this method and return a {@link ModelStoredEvent} subclass
     * i.e. a StringBaseCollection could override this method to return a StringCollectionStored event.
     *
     * @return return a new instance of ModelStoredEvent
     */
    public ModelStoredEvent getModelStoredEventInstance() {
        return new ModelStoredEvent();
    }

    /**
     * This event should be published when the collection is successfully stored in disk (actually in the {@link CollectionStorage}).
     *
     * @author fernandinho
     */
    public static class ModelStoredEvent{}

    /**
     * Interface that defines how this collection is persisted in disk. When implementing a custom {@link BaseCollection}
     * you can create your own 'storage' mediums. This also simplifies testing by allowing subclasses to have a RAM storage.
     *
     * @param <K>
     * @author fernandinho
     */
    public interface CollectionStorage<K> {

        public void save(BaseCollection<K> collection, Callback<Void> callback);

        public void load(Callback<Collection<K>> callback);

        public Collection<K> loadSync();
    }

    public interface Callback<K> {
        public void onFinish(K data);
    }
}
