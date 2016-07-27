package io.quckoo.examples;

import java.util.concurrent.Callable;

/**
 * Created by alonsodomin on 27/07/2016.
 */
public class HelloWorldJob implements Callable<Object> {

    @Override
    public Object call() throws Exception {
        System.out.println("Hello World!");
        return null;
    }

}
