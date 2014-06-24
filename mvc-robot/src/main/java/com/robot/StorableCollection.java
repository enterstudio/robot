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
     * Modifies the storage medium for this {@link BaseCollection}
     *
     * @param storage
     */
    public void setStorage(CollectionStorage<T> storage) {
        this.storage = storage;
    }
}
