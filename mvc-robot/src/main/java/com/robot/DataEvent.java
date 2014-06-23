package com.robot;

/**
 * Simple class used for sending event with data attached to them. This is meant to be usd with an event bus in the following way:
 *
 * <code>
 *     // subclass DataEvent with the type of Data your event will carry
 *     class NewUserEvent extends DataEvent<User> {
 *
 *         public NewUserEvent(User user){
 *             super(user);
 *         }
 *     }
 *
 *     // when your user is created, notify subscribers like this
 *     User user = getNewUser();
 *     bus.post(new NewUserEvent(user));
 *
 *     // on your subscribe method
 *     @Subscribe
 *     public void onNewUser(NewUserEvent event){
 *         User user = event.getData();
 *         doSomethingWithNewUser(user);
 *     }
 *
 * </code>
 *
 * {@code bus.post(new )}
 * @param <T> the type of data this event carried with
 */
public class DataEvent<T> {

    private T data;

    /**
     * Created a new instance of DataEvent and sets the data that this event will carry.
     * @param data the data to carry
     * @see #getData()
     */
    public DataEvent(T data) {
        this.data = data;
    }

    /**
     * @return returns the {@link T} object set in {@link #DataEvent(Object)}
     */
    public T getData() {
        return data;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + data;
    }
}
