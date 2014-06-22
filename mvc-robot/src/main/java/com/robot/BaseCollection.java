package com.robot;

import com.squareup.otto.Bus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * The {@link BaseCollection} class is an abstraction for a model that can be stored locally and sync'd remotely.
 * It provides several methods to make asynchronous operations easier by depending on an event bus.<br>
 * <br>
 * By default a {@link BaseCollection} provides 2 types of events, {@link ModelChangedEvent} and {@link ModelStoredEvent}<br>
 * <br>
 * The {@link ModelChangedEvent} is published whenever the underlying data structure is modified. To subscribe to this event you
 * must do 2 things. The first one is override the {@link #getModelChangeEventInstance()} to return a subclass of ModelChangedEvent,
 * i.e. a CarsModelChangedEvent. then on the listener class you only have to write.<br>
 * <br>
 * <code>
 * Subscribe<br>
 * public void onCarsChanged(CarsModelChangedEvent event){<br>
 * //Do stuff when the cars model is changed, i.e. repaint the view.<br>
 * }<br>
 * </code>
 * <br>
 * Make sure the class that is subscribing to the ModelChangedEvent is registered in the event bus via {@link Bus#register(Object)} and
 * make sure to unregister ( {@link Bus#unregister(Object)}) when not using it. <br>
 * <br>
 * Models are compared via their equals method so it is likely that you should override the {@link #equals(Object)} method for your T models. If you
 * are going to override the {@link #equals(Object)} it is a best practice to also override the {@link #hashCode()} method, don't worry
 * eclipse can do this for you by clicking Source -> Generate hashCode() and equals()
 *
 * @param <T>
 * @author fernandinho
 */
public abstract class BaseCollection<T> implements Iterable<T> {

    private List<T> list;
    protected Bus bus;
    protected CollectionStorage<T> storage;

    /**
     * Lock used to modify the content of {@link #list}. Any write operation
     * performed on the list should be synchronized on this lock.
     */
    protected final Object lock = new Object();

    public BaseCollection() {
        list = new ArrayList<T>();
    }

    /**
     * <p>
     * Override this method to specify the way your Collection synchronizes. By default
     * All elements in the current BaseCollection that match those in {@code data} will be removed and
     * then re-added with the elements in data (using {@link #updateAll(java.util.Collection, boolean)}
     * </p>
     * <p>
     *  Matching between elements is done using the equals operator.
     * </p>
     * @param data
     */
    protected void onSyncFinish(Collection<T> data) {
        updateAll(data, true);
    }

    /**
     * Shorthand method for removing the given data and then adding it again.
     *
     * Object equality comparison is done using the {@link #equals(Object)} method
     *
     * @param data
     * @param notify if notifyEvent is true, a {@link ModelChangedEvent} will be published after all the data has been added to the BaseCollection.
     */
    public void updateAll(Collection<? extends T> data, boolean notify) {
        removeAll(data);
        addAll(data, notify);
    }

    /**
     * Removes a given element. If you wish to remove more than one the recommended way to do this is by using {@link #removeAll(java.util.Collection)}
     *
     * @param el
     */
    public void remove(T el) {
        synchronized (lock){
            list.remove(el);
        }
    }

    /**
     * Removes a collection of elements
     *
     * @param els
     */
    public void removeAll(Collection<? extends T> els) {
        synchronized (lock) {
            list.removeAll(els);
        }
    }

    /**
     * Removes all data in the inner data structure
     */
    public void clear() {
        synchronized (lock) {
            list.clear();
        }
    }

    /**
     * similar to {@link #add(Object, boolean)} but does not publish an event
     *
     * @param el
     */
    public void add(T el) {
        add(el, true);
    }

    /**
     * Iterates over the given {@link Iterable} and adds every element. Optionally publishes a {@link ModelChangedEvent}
     *
     * @param el
     * @param notifyChanges
     */
    public void addAll(Iterable<? extends T> el, boolean notifyChanges) {
        for (T t : el) {
            add(t, false);
        }
        if (notifyChanges) {
            notifyChanges();
        }
    }

    /**
     * Adds the given element to the BaseCollection. Optionally publishes a {@link ModelChangedEvent}
     *
     * @param el
     * @param notifyChanges
     */
    public void add(T el, boolean notifyChanges) {
        synchronized (lock) {
            list.add(el);
        }
        if (notifyChanges) {
            notifyChanges();
        }
    }

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
     * Maps every element contained in the collection using a {@link Mapper}
     *
     * @param mapper maps an element of type T to an element of type K
     * @return the list of mapped elements.
     */
    public <K> List<K> map(Mapper<T, K> mapper) {
        return map(list, mapper);
    }

    /**
     * Runs an iterator through every element in the BaseModel
     *
     * @param iterator
     */
    public void each(Iter<T> iterator) {
        for (T el : list) {
            iterator.item(el);
        }
    }

    /**
     * When calling this method from a subclass it is recommended that you create a new method <code>sort()</code>
     * that calls {@link #sort(java.util.Comparator)} with your {@link BaseCollection} subclass' default {@link java.util.Comparator}
     *
     * @param comparator the comparator used to sort the inner collection
     */
    public void sort(Comparator<T> comparator) {
        Collections.sort(list, comparator);
    }

    /**
     * this means that the BaseCollection is iterable, i.e. one can write
     * <code>
     * for(T t: baseCollection){
     * //do stuff
     * }
     * </code>
     */
    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    /**
     * @param initialValue the initial value to be reduced
     * @param reducer      an interface used to reduce the list
     * @return the calculated reductions
     */
    public <K> K reduce(K initialValue, Reducer<T, K> reducer) {
        K reduction = initialValue;
        for (T el : list) {
            reduction = reducer.reduce(reduction, el);
        }
        return reduction;
    }

    /**
     * @param filters
     * @return let [a1, a2, ..., an]  be the list of all elements in this {@link BaseCollection}'s underlying data structure.
     * <p>
     * this method will return { |a| s.t. "a returns true on every filter" }
     * This means that every element will be compared agains every filter and it must pass all filters in order
     * for it to be added to the result set.
     * </p>
     */
    public List<T> filter(Filter<T>... filters) {
        synchronized (lock) {
            return filter(list, filters);
        }
    }

    /**
     * @param filters a list of filters to be a applied to each element in the collection
     * @param collection the collection to be filtered upon
     * @return let [a1, a2, ..., an]  be the list of all elements in this {@link BaseCollection}'s underlying data structure.
     * <p>
     * this method will return { |a| s.t. "a returns true on every filter" }
     * This means that every element will be compared agains every filter and it must pass all filters in order
     * for it to be added to the result set.
     * </p>
     */
    public static <T> List<T> filter(Collection<T> collection, Filter<T>... filters) {
        List<T> result = new ArrayList<T>();
        for (T el : collection) {
            boolean passedAllFilters = true;
            for (Filter<T> filter : filters) {
                if (!filter.include(el)) {
                    passedAllFilters = false;
                    break;
                }
            }
            if (passedAllFilters) {
                result.add(el);
            }
        }
        return result;
    }

    /**
     * Similar to {@link #filter(Filter...)} but it only returns the first element that matches all filters
     *
     * @param filters
     * @return
     */
    public T filterFirst(Filter<T>... filters) {
        for (T el : list) {
            boolean passedAllFilters = true;
            for (Filter<T> filter : filters) {
                if (!filter.include(el)) {
                    passedAllFilters = false;
                    break;
                }
            }
            if (passedAllFilters) {
                return el;
            }
        }
        return null;
    }

    /**
     * Publishes a {@link ModelChangedEvent}
     */
    public void notifyChanges() {
        notifyEvent(getModelChangeEventInstance());
    }

    /**
     * Publishes a {@link ModelStoredEvent}
     */
    public void notifySave() {
        notifyEvent(getModelStoredEventInstance());
    }

    /**
     * publishes an {@link ErrorCapturedEvent}
     *
     * @param error
     */
    public void notifyError(Throwable error) {
        notifyEvent(new ErrorCapturedEvent(error));
    }

    /**
     * Posts an event to the event bus. This can be any event.
     *
     * @param object
     */
    public void notifyEvent(Object object) {
        bus.post(object);
    }

    /**
     * @return a {@link java.util.List} representation of this {@link BaseCollection}'s elements. Order is not guaranteed.
     */
    public List<T> toList() {
        return list;
    }

    /**
     * @return the number of elements in this collection
     */
    public int size() {
        return list.size();
    }

    /**
     * @return true if the underlying collection is empty. See {@link java.util.Collection#isEmpty()} for more details.
     */
    public boolean isEmpty(){
        return list.isEmpty();
    }

    /**
     * Sets the event bus used by this {@link BaseCollection} to publish events.
     *
     * @param bus
     */
    public void setEventBus(Bus bus) {
        this.bus = bus;
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
     * @param el
     * @return Returns true if the underlying collection contains the given element.
     */
    public boolean contains(T el) {
        return list.contains(el);
    }

    /**
     * @see #getModelStoredEventInstance()
     */
    public ModelChangedEvent<T, BaseCollection<T>> getModelChangeEventInstance() {
        return new ModelChangedEvent<T, BaseCollection<T>>(this);
    }

    /**
     * Subclasses that want to publish more specific events can override this method and return a {@link ModelStoredEvent} subclass
     * i.e. a StringBaseCollection could override this method to return a StringCollectionStored event.
     *
     * @return return a new instance of ModelStoredEvent
     */
    public ModelStoredEvent<T, BaseCollection<T>> getModelStoredEventInstance() {
        return new ModelStoredEvent<T, BaseCollection<T>>(this);
    }

    @Override
    public String toString() {
        return "[" + BaseCollection.class.getSimpleName() + " (" + size() + "): " + list + "]";
    }

    /**
     * This event should be published whenever the collection captures an error, i.e. a disk error or network error when
     * saving the collection remotely or locally.
     *
     * @author fernandinho
     */
    public static class ErrorCapturedEvent extends DataEvent<Throwable> {

        public ErrorCapturedEvent(Throwable data) {
            super(data);
        }

    }

    /**
     * This event should be published when the collection's underlying data structure is modified.
     *
     * @param <T>
     * @param <K>
     * @author fernandinho
     */
    public static class ModelChangedEvent<T, K extends BaseCollection<T>> extends DataEvent<K> {

        public ModelChangedEvent(K data) {
            super(data);
        }
    }

    /**
     * This event should be published when the collection is successfully stored in disk (actually in the {@link CollectionStorage}).
     *
     * @param <T>
     * @param <K>
     * @author fernandinho
     */
    public static class ModelStoredEvent<T, K extends BaseCollection<T>> extends DataEvent<K> {

        public ModelStoredEvent(K data) {
            super(data);
        }
    }

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

    /**
     * Interface used for filtering elements inside the {@link BaseCollection}
     *
     * @param <T>
     * @author fernandinho
     */
    public interface Filter<T> {
        public boolean include(T el);
    }

    public interface Mapper<T, K> {
        public K map(T el);
    }

    public static <T, K> List<K> map(Collection<T> collection, Mapper<T, K> mapper) {
        List<K> result = new ArrayList<K>();
        for (T el : collection) {
            K mapped = mapper.map(el);
            result.add(mapped);
        }
        return result;
    }

    public static <T, K> List<K> filter(Collection<T> collection, Mapper<T, K> mapper) {
        List<K> result = new ArrayList<K>();
        for (T el : collection) {
            K mapped = mapper.map(el);
            result.add(mapped);
        }
        return result;
    }

    public interface Reducer<T, K> {
        public K reduce(K acum, T element);
    }

    public interface Iter<T> {
        public void item(T el);
    }

    /**
     * Minimal implementation for {@link Filter}, subclasses should compare the given constructor argument
     * with some field/method if the include(T el) param
     *
     * @author fernandinho
     */
    public abstract class DefFilter implements Filter<T> {

        protected Object value;

        public DefFilter(Object value) {
            this.value = value;
        }
    }
}
