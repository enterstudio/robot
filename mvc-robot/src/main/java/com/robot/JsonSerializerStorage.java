package com.robot;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link JsonSerializerStorage} is a wrapper on top of {@link android.content.SharedPreferences} to save and load simple objects.
 * Bear in mind that because objects are serialized to and from JSON when saved this can be an expensive process.
 *
 * @param <T> the type of the object to be stored
 * @author fernandohur
 */
public class JsonSerializerStorage<T> implements OnSharedPreferenceChangeListener, Storage<T> {

    public final static String KEY_VERSION = "key_version";

    private Context context;
    private SharedPreferences sharedPrefs;
    private Gson gson;
    private String key;
    private TypeToken<T> clazz;
    private OnDataChangedListener onDataChangedListener;
    private List<UpdateTask> updateTasks;

    /**
     * A reference to the cached object.
     */
    private T cached;

    public JsonSerializerStorage(Context context, TypeToken<T> clazz, String name) {
        this.context = context;
        this.sharedPrefs = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.key = name + ".key";
        this.clazz = clazz;
        this.sharedPrefs.registerOnSharedPreferenceChangeListener(this);
        this.updateTasks = getUpdateTasks();
    }

    public List<UpdateTask> getUpdateTasks(){
        return new ArrayList<UpdateTask>(0);
    }

    @Override
    public boolean isStored() {
        return sharedPrefs.contains(key);
    }

    /**
     * @return returns the version stored in disk.
     */
    public int getStoredVersion(){
        return this.sharedPrefs.getInt(KEY_VERSION, -1);
    }

    /**
     * Updates the stored version with the parameter. This is stored in disk.
     * @param version the version to be saved.
     */
    public void setStoredVersion(int version){
        this.sharedPrefs.edit().putInt(KEY_VERSION, version).commit();
    }

    /**
     * saves a object of type T as a serialized JSON string in a {@link android.content.Context#MODE_PRIVATE private} shared preferences.
     * This overrides any previously saved data.
     * @param data the object to be saved
     */
    public void save(T data) {
        invalidateCache();
        String json = gson.toJson(data);
        Editor editor = sharedPrefs.edit();
        editor.putString(key, json);
        editor.commit();
    }

    /**
     * Can return null if unable to find or parse the persisted json
     *
     * @return returns the persisted serialized object.
     */
    @SuppressWarnings("unchecked")
    public T load() {
        if(shouldUpdate()){
            runUpdates(getStoredVersion(), getCurrentVersion());
        }

        if(isCacheValid()){
            return cached;
        }
        String json = sharedPrefs.getString(key, null);
        T data = (T) gson.fromJson(json, clazz.getType());
        return data;
    }

    /**
     * Clears anything stored in this storage.
     */
    public void clear() {
        invalidateCache();
        sharedPrefs.edit().clear().commit();
    }

    public boolean shouldUpdate(){
        int storedVersion = getStoredVersion();
        if(storedVersion == -1 || storedVersion < getCurrentVersion()){
            return true;
        }
        return false;
    }

    /**
     * Runs all updates where storedVersion <= update.version() <= currentVersion
     * @param storedVersion
     */
    public void runUpdates(int storedVersion, int currentVersion){
        for(UpdateTask task: updateTasks){
            int version = task.version();
            if(storedVersion <= version && version < currentVersion){
                task.runUpdate();
            }
        }
        setStoredVersion(getCurrentVersion());
    }

    /**
     * Removes the cache, forcing calls of load to load from disk and create the serialized object
     */
    protected void invalidateCache(){
        cached = null;
    }

    /**
     * @return Returns true if the cache is valid. A cache is considered valid when only read operations have been performed on the data.
     */
    protected boolean isCacheValid(){
        return cached != null;
    }

    /**
     * Sets a listener to be notified when changes occur to the underlying data
     * @param listener
     */
    public void setOnDataChangedListener(OnDataChangedListener listener) {
        this.onDataChangedListener = listener;
    }

    /**
     * Removes the listener. You should always call this method when ana ctivity is finished to avoid notifying a 'dead' activity.
     */
    public void removeDataChangedListener() {
        this.onDataChangedListener = null;
    }


    /**
     * @return returns the current version
     */
    private int getCurrentVersion() {
        PackageManager manager = context.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (this.key.equals(key) && onDataChangedListener != null) {
            T data = load();
            onDataChangedListener.onDataChanged(data, this);
        }
    }

    /**
     * Simple listener class that is notified when changes occur to the underlying data structure.
     */
    public interface OnDataChangedListener {
        public <T> void onDataChanged(T changedData, JsonSerializerStorage<T> storageSource);
    }


    public interface UpdateTask{

        /**
         * @return the version this update should run in.
         */
        public int version();

        /**
         * Update logic should go here.
         */
        public void runUpdate();
    }

}
