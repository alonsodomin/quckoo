package io.kairos.examples.parameters;

import java.util.concurrent.Callable;

/**
 * Created by aalonsodominguez on 24/07/15.
 */
public class PowerOfNJob implements Callable<String> {

    public int n = 0;

    @Override
    public String call() throws Exception {
        int res = n * n;
        return String.format("n * n = %d", res);
    }

}
