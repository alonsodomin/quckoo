package io.chronos.resolver;

import java.util.concurrent.Callable;

/**
 * Created by aalonsodominguez on 26/07/15.
 */
public class DummyJavaJob implements Callable<Void> {

    public int value = 0;

    @Override
    public Void call() throws Exception {
        System.out.println("DummyJavaJob invoked! value=" + value);
        return null;
    }

}
