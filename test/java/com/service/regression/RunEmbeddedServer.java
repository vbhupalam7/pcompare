package com.service.regression;

import com.intuit.service.fps.Service;
import junit.framework.TestCase;

/**
 * Utility class used in functional tests
 * @author vbhupalam
 *
 * WARNING!!! DO NOT ADD ANY TESTS TO THIS CLASS

 */
public class RunEmbeddedServer extends TestCase
{
    /**
     * This "test" just waits until the exitEmbeddedServer resource
     * is called. It allows developers to run the embedded server
     * so that localhost can be accessed via a browser to test
     * new resources before they are deployed.
     * @throws Exception if an exception occurs.
     */
    public void testRunEmbeddedServer() throws Exception
    {
        // Only run the embedded server test if it was explicitly called for
        // This avoids having "normal" builds that run tests waiting on
        // this test.
        if (RunEmbeddedServer.class.getName().equals("com.service.regression." + System.getProperty("test")))
        {
            ServiceUtil.waitForEmbeddedServer();
        }
    }
}
