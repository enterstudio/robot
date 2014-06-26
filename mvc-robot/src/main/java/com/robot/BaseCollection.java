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
 * By default a {@link BaseCollection} provides 1 types of events, the {@link ModelChangedEvent} <br>
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
 * @param <T> the type of data this collection contains
 * @author fernandinho
 */
public abstract class BaseCollection<T> implements Iterable<T> {

    private List<T> list;
    protected Bus bus;

    /**
     * Lock used to modify the content of {@link #list}. Any write operation
     * performed on the list should be synchronized on this lock.
     */
    protected final Object lock = new Object();

    /**
     * When creating an instance of BaseCollection be aware that you must configure the event bus to connect with your global event bus instance.
     * To do this simply call {@code baseCollection.setEventBus(youEventBus)}.
     * You may also configure the underlying data structure by calling {@code setList()}
     */
    public BaseCollection() {
        list = new ArrayList<T>();
        bus = new Bus();
    }

    /**
     * Shorthand method for removing the given data and then adding it again.
     *
     * Object equality comparison is done using the {@link #equals(Object)} method
     *
     * @param data a collection of T object that will be updated. If not present, it will be added.
     * @param notify if notifyEvent is true, a {@link ModelChangedEvent} will be published after all the data has been added to the BaseCollection.
     */
    public void updateAll(Collection<? extends T> data, boolean notify) {
        removeAll(data);
        addAll(data, notify);
    }

    /**
     * Removes a given element. If you wish to remove more than one the recommended way to do this is by using {@link #removeAll(java.util.Collection)}
     *
     * @param el the element to remove. Removal is based on the {@code equals} method.
     */
    public void remove(T el) {
        synchronized (lock){
            list.remove(el);
        }
    }

    /**
     * Removes a collection of elements and notifies changes.
     * This is a shorthand for {@link #removeAll(java.util.Collection, boolean)} with the second parameter set to true
     * @param els the collection of elements to remove
     * @see #removeAll(java.util.Collection, boolean)
     */
    public void removeAll(Collection<? extends T> els) {
        removeAll(els, true);
    }

    /**
     * Removes a collection of elements, optionally notifies changes
     * @param els the collection of elements to remove
     * @param notifyChanges if true, changes will be notified, if false, no changes will be notified.
     *                      Note that changes will only be notified if there are elements removed from the
     *                      BaseCollection, i.e. if {@code els} is an empty collection, then no changes
     *                      will be notified even if {@code notifyChanges} is true
     */
    public void removeAll(Collection<? extends T> els, boolean notifyChanges) {
        boolean changes;
        synchronized (lock) {
            changes = list.removeAll(els);
        }
        if(changes && notifyChanges) notifyChanges();
    }

    /**
     * Removes all data in the inner data structure. Notifies changes if there is a change in the number of elements in the colleciton
     * and {@code notifyChanges} is {@code true}
     */
    public void clear(boolean notifyChanges) {
        int size = size();
        synchronized (lock) {
            list.clear();
        }
        if(size() != size && notifyChanges) notifyChanges();
    }

    /**
     * Clears the collection and notifies if any changes
     * @see #clear(boolean)
     */
    public void clear(){
        clear(true);
    }

    /**
     * similar to {@link #add(Object, boolean)} but publishes an event. This is a shorthand for {@link #add(Object, boolean) add(someObject, true)}
     *
     * @param el the object that will be added
     */
    public void add(T el) {
        add(el, true);
    }

    /**
     * Iterates over the given {@link Iterable} and adds every element. Optionally publishes a {@link ModelChangedEvent}
     *
     * @param el the elements that will be added
     * @param notifyChanges if true, changes will be notified, if false, no changes will be notified.
     */
    public void addAll(Iterable<? extends T> el, boolean notifyChanges) {
        int originalSize = size();
        for (T t : el) {
            add(t, false);
        }
        if (notifyChanges && size() > originalSize) {
            notifyChanges();
        }
    }

    /**
     * Adds the given element to the BaseCollection. Optionally publishes a {@link ModelChangedEvent}
     *
     * @param el the element that will be added.
     * @param notifyChanges if true, changes will be notified, if false, no changes will be notified.
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
     * Maps every element contained in the collection using a {@link Mapper}
     *
     * @param mapper maps an element of type T to an element of type K
     * @return the list of mapped elements.
     */
    public <K> List<K> map(Mapper<T, K> mapper) {
        synchronized (lock){
            return map(list, mapper);
        }
    }

    /**
     * Runs an iterator through every element in the BaseModel. Order is not guaranteed.
     * @param iterator the iterator interface.
     */
    public void each(Iter<T> iterator) {
        for (T el : list) {
            iterator.item(el);
        }
    }

    /**
     * @param initialValue the initial value to be reduced
     * @param reducer      an interface used to reduce the list
     * @return the calculated reductions
     */
    public <K> K reduce(K initialValue, Reducer<T, K> reducer) {
        synchronized (lock){
            return reduce(initialValue, reducer, list);
        }
    }

    /**
     * @param filters an array of filters
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
     * Same behaviour to calling {@link #filterFirst(java.util.Collection, com.robot.BaseCollection.Filter[])} on the contents of this BaseCollection
     * @see #filterFirst(java.util.Collection, com.robot.BaseCollection.Filter[])
     */
    public T filterFirst(Filter<T>... filters) {
        return filterFirst(list, filters);
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
     * Publishes a {@link ModelChangedEvent}
     */
    public void notifyChanges() {
        notifyEvent(getModelChangeEventInstance());
    }

    /**
     * publishes an {@link ErrorCapturedEvent}
     *
     * @param error the event to publish
     */
    public void notifyError(Throwable error) {
        notifyEvent(new ErrorCapturedEvent(error));
    }

    /**
     * Posts an event to the event bus. This can be any event.
     *
     * @param object the event.
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
     * @param bus the bus where events will be posted
     */
    public void setEventBus(Bus bus) {
        this.bus = bus;
    }

    /**
     * Sets the underlying data structure.
     * @param list the replacement for the default list.
     */
    public void setList(List<T> list){
        this.list = list;
    }


    /**
     * @param el the element
     * @return Returns true if the underlying collection contains the given element.
     */
    public boolean contains(T el) {
        return list.contains(el);
    }

    /**
     * When listening to changes from this collection using the event bus it is necessary to override this method and return
     * a suitable ModelChangedEvent subclass. As a simple example, consider you have a {@code UsersBaseCollection} and after adding
     * a user a ModelChangedEvent is published. you could listen to the {@code ModelChangedEvent} but this will be called for every
     * BaseCollection, it would instead be better to override this method and return a {@code UsersChangedEvent}, this way you can register
     * to changes of only this {@code UsersBaseCollection}
     *
     * @return returns a new instance of ModelChangedEvent
     */
    public ModelChangedEvent getModelChangeEventInstance() {
        return new ModelChangedEvent();
    }


    @Override
    public String toString() {
        return "[" + BaseCollection.class.getSimpleName() + " (" + size() + "): " + list + "]";
    }

    //======================================================================
    // Static methods
    //======================================================================

    /**
     * Maps every single object in a collection to another object and returns the mapped collection.
     *
     * This method takes every element in {@code collection} and applies the {@code mapper} function.
     *
     * @param collection the input collection
     * @param mapper the mapping function that takes an element and applies a transformation to the object
     * @param <T> the type of the elements in the input collection
     * @param <K> the type of the elements in the output collection
     * @return the mapped collection
     */
    public static <T, K> List<K> map(Collection<T> collection, Mapper<T, K> mapper) {
        List<K> result = new ArrayList<K>();
        for (T el : collection) {
            K mapped = mapper.map(el);
            result.add(mapped);
        }
        return result;
    }

    /**
     * Similar to {@link #filter(Filter...)} but it only returns the first element that matches all filters
     *
     * @param filters a list of filters
     * @return returns the first element that matches every single filter. The notion of 'first' depends on the {@code collection}'s iterator.
     */
    public static <T> T filterFirst(Collection<T> collection, Filter<T>... filters) {
        for (T el : collection) {
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
     * @param initialValue the initial value to be reduced
     * @param reducer      an interface used to reduce the list
     * @return the calculated reductions
     */
    public static <T,K> K reduce(K initialValue, Reducer<T, K> reducer, Collection<T> list) {
        K reduction = initialValue;
        for (T el : list) {
            reduction = reducer.reduce(reduction, el);
        }
        return reduction;
    }

    //======================================================================
    // Inner classes and interfaces
    // Unless absolutely necessary all inner classes must be static to
    // avoid accidental coupling
    //======================================================================

    /**
     * Interface used to map a collection to another
     * @param <T> the map's input type
     * @param <K> the map's output type
     */
    public interface Mapper<T, K> {
        public K map(T el);
    }

    public interface Reducer<T, K> {
        public K reduce(K acum, T element);
    }

    public interface Iter<T> {
        public void item(T el);
    }

    /**
     * Interface used for filtering elements inside the {@link BaseCollection}
     *
     * @param <T> the type of object to filter
     * @author fernandinho
     */
    public interface Filter<T> {
        public boolean include(T el);
    }

    /**
     * Minimal implementation for {@link Filter}, subclasses should compare the given constructor argument
     * with some field/method if the include(T el) param
     *
     * @author fernandinho
     */
    public static abstract class DefFilter<T> implements Filter<T> {

        protected Object value;

        public DefFilter(Object value) {
            this.value = value;
        }
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
     * @author fernandinho
     */
    public static class ModelChangedEvent {}
}
