/*
 *   $Id$
 *
 *   Copyright 2009 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.services.blitz.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ResultHolder<U> {

    private final static Log log = LogFactory.getLog(ResultHolder.class);

    private final CountDownLatch c = new CountDownLatch(1);

    private final long timeout;

    private volatile U rv = null;

    public ResultHolder(long timeoutMillis) {
        timeout = timeoutMillis;
    }

    public void set(U obj) {
        if (obj != null) {
            rv = obj;
            c.countDown();
        }
    }

    public U get() {
        try {
            c.await(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // ignore
        }
        if (rv == null) {
            log.debug("Nothing found.");
        }
        return rv;
    }
}