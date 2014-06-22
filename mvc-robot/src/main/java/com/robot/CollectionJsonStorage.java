package com.robot;

import android.content.Context;

import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class provides a CollectionStorage based on the {@link JsonSerializerStorage}.
 *
 * @param <T>
 * @author fernandinho
 */
public class CollectionJsonStorage<T> extends JsonSerializerStorage<List<T>> implements BaseCollection.CollectionStorage<T> {

    public CollectionJsonStorage(Context context, TypeToken<List<T>> typeToken, String storageLocation) {
        super(context, typeToken, storageLocation);
    }

    @Override
    public void save(BaseCollection<T> collection, BaseCollection.Callback<Void> callback) {
        save(collection.toList());
        callback.onFinish(null);
    }

    @Override
    public List<T> load() {
        List<T> result = super.load();
        if (result == null) {
            return new ArrayList<T>();
        }
        return result;
    }

    @Override
    public void load(BaseCollection.Callback<Collection<T>> callback) {
        Collection<T> data = load();
        callback.onFinish(data);
    }

    @Override
    public Collection<T> loadSync() {
        return load();
    }

}

