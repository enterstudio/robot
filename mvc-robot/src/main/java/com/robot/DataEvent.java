package com.robot;

public class DataEvent<T> {

    private T data;

    public DataEvent(T data) {
        this.data = data;
    }

    public T getData() {
        return data;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + data;
    }
}
