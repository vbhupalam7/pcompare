package com.service.regression;

import com.intuit.cg.services.fs.common.DomUtil;
import com.intuit.service.fps.ml.form.FmlElement;
import com.intuit.service.fps.ml.form.FmlForm;
import com.intuit.service.fps.ml.form.FmlFormset;
import com.intuit.service.fps.ml.form.FmlRoot;

import junit.framework.TestCase;

import com.meterware.httpunit.WebResponse;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Test filling the forms of a PDF file from a map of values.
 * @author vbhupalam
 */
public class AllFormsRegressionTest extends TestCase
{
    private static final int EXPECTED_RESPONSE_CODE = 200;
    private static FPSLogger logger = FPSLogger.getLogger(AllFormsRegressionTest.class);

    private static String getNewestCVTYcontent() throws Exception
    {
        String retval = null;
        String url = ServiceUtil.BASE_URI + "/versions";
        WebResponse response = ServiceUtil.getMethodWebServiceResponse(url);
        assertEquals("Invalid HTTP response code when getting versions",
                EXPECTED_RESPONSE_CODE, response.getResponseCode());
        Document versionsDoc = DomUtil.getDocument(response.getInputStream());
        Element versionsElement = versionsDoc.getDocumentElement();
        
        for (Node child = versionsElement.getFirstChild(); child != null; child = child.getNextSibling())
        {
            if ("content".equals(child.getNodeName()))
            {
                Element content = (Element)child;
                String compatibilityVersion = content.getAttribute("compatibilityVersion");
                
                if (compatibilityVersion.startsWith("CVTY") &&
                    (retval == null || retval.compareTo(compatibilityVersion) < 0))
                {
                    retval = compatibilityVersion;
                }
            }
        }
        
        return retval;
    }
    
    /*
     * Generates  pdf's while loooping over all forms of the formsets
     */
    
    public void testGenerateAllPdfs() throws Exception
    {
        String cvty = getNewestCVTYcontent();
        assertNotNull("Could not get latest CVTY version", cvty);
        String formsetsURL = ServiceUtil.BASE_URI + "/v2/" + cvty + "/formsets";
        WebResponse response = ServiceUtil.getMethodWebServiceResponse(formsetsURL);
        assertEquals("Invalid HTTP response code when getting formsets for " + cvty,
                EXPECTED_RESPONSE_CODE, response.getResponseCode());
        FmlRoot formML = FmlRoot.create(response.getInputStream());
        response.getInputStream().close();
        
        // loop over all formsets
        for (FmlFormset formset : formML.getFormsets())
        {
            String formsURL = formsetsURL + "/" + formset.getId() + "/forms";
            
            response = ServiceUtil.getMethodWebServiceResponse(formsURL);
            assertEquals("Invalid HTTP response code when getting forms for " + formset.getIdentity(),
                    EXPECTED_RESPONSE_CODE, response.getResponseCode());
            formML = FmlRoot.create(response.getInputStream());
            response.getInputStream().close();
            
            // loop over all forms
            for (FmlElement child : formML.getFormsets()[0].getChildren())
            {
                if ("form".equals(child.getTagName()))
                {
                    FmlForm form = (FmlForm)child;
                    String testClientURL = formsURL + "/" + form.getId() + "/test-client";
                    
                    response = ServiceUtil.getMethodWebServiceResponse(testClientURL);
                    assertEquals("Invalid HTTP response code when getting test client for " + form.getIdentity(),
                            EXPECTED_RESPONSE_CODE, response.getResponseCode());
                    assertFalse("Could not get XML test client for " + form.getIdentity(), response.isHTML());
                    byte[] xml = toByteArray(response.getInputStream());
                    response.getInputStream().close();
                    
                    // finally, get the PDF for each form
                    response = ServiceUtil.postMethodWebServiceResponse(xml);
                    assertEquals("Invalid HTTP response code when generating PDF for " + form.getIdentity(),
                            EXPECTED_RESPONSE_CODE, response.getResponseCode());
                    response.getInputStream().close();
                }
            }
        }
    }
    
    //converts input stream to byte array
    private byte[] toByteArray(InputStream inStream) throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final int bufferSize = 8192; //same as Tomcat
        final byte[] buf = new byte[bufferSize];
        int numRead = 0;
        while ((numRead = inStream.read(buf)) != -1)
        {
            baos.write(buf, 0, numRead);
        }

        return baos.toByteArray();
    }
}
