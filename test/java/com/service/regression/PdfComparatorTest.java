
package com.service.regression;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.meterware.httpunit.WebResponse;

/**
 * Test filling the forms of a PDF file from a map of values.
 * @author vbhupalam
 */
public class PdfComparatorTest extends TestCase
{
    private static List<File> testPdfFiles = new ArrayList<File>();
    private static final String TEST_PDF_CONTENT_ROOT_DIR = "/content/test_forms";
    private static final String GENERATED_FILENAME_PREFIX = "generated_";
    private static final int EXPECTED_RESPONSE_CODE = 200;
    private static FPSLogger logger = FPSLogger.getLogger(PdfComparatorTest.class);

    static
    {
        loadTestPdfFiles(ServiceUtil.getFile(TEST_PDF_CONTENT_ROOT_DIR));
    }

    private static void loadTestPdfFiles(File directory)
    {
        if (directory.isDirectory())
        {
            File[] files = directory.listFiles();

            for (File file : files)
                
            {
                if (file.isDirectory())
                {
                    loadTestPdfFiles(file);
                }
                else if (file.isFile() && file.getName().endsWith(".pdf"))
                {
                    testPdfFiles.add(file);
                    System.out.println("Added " + file);;
                }
            }
        }
    }
    
    /**
     * Test that verifies equivalence algorithm of PdfComparator is working - Positive
     * @throws Exception when exception occurs
     */
    public void testPdfComparePositive() throws Exception
    {
        String exclude = "Negative";
        boolean success = true;
        List<String> failedApprovedPdfs = new ArrayList<String>();
        List<String> invalidFormML = new ArrayList<String>();
        
        for (File approvedPdf : testPdfFiles)
        {
            if (approvedPdf.getName().contains(exclude))
            {
                continue;
            }
            //Setup generated PDF
            String formMLFilePath = approvedPdf.getPath().replace(".pdf", ".xml");
            String xml = ServiceUtil.readFileAsStringUsingAbsolutePath(formMLFilePath);
            String generatedPdfFileName = GENERATED_FILENAME_PREFIX + approvedPdf.getName();
            String[] ids = ServiceUtil.getIds(xml);
            if (ids == null)
            {
                continue;
            }
            WebResponse response = ServiceUtil.postMethodWebServiceResponse(xml.getBytes());            
            assertNotNull("Did not receive a response from the service.", response);

            //verify valid response
            if ((ids == null) || (response.getResponseCode() != EXPECTED_RESPONSE_CODE))
            {
                success = false;
                invalidFormML.add(approvedPdf.getName().replace(".pdf", ".xml"));
            }
            else
            {
                InputStream is = response.getInputStream();
                assertNotNull("Response did not contain an Input Stream", is);
                File generatedPdf = ServiceUtil.createPdfFile(is, generatedPdfFileName);
    
                //Test equivalence
                PdfToImageConverter imageConverter = new GhostscriptPdfToImageConverter(approvedPdf, generatedPdf);
                boolean equivalent = new PdfComparator(imageConverter).compare();
                
                if (!equivalent)
                {
                    success = false;
                    failedApprovedPdfs.add(approvedPdf.getName());
                }
                
                is.close();
            }
        }
        
        assertTrue("\nThe service was unable to process the following formML files: \n\t" + invalidFormML.toString(), invalidFormML.size() == 0);
        
        assertTrue("\nThe rendered images of the following approved PDFs have changed: \n\t" + failedApprovedPdfs.toString(), success);

    }
    
    /**
     * Test that verifies equivalence algorithm of PdfComparator is working - Negative
     * @throws Exception when exception occurs
     */
    public void testPdfCompareNegative() throws Exception
    {
        String exclude = "Positive";
        boolean success = true;
        List<String> failedApprovedPdfs = new ArrayList<String>();
        List<String> invalidFormML = new ArrayList<String>();
        
        logger.info("\n------------------------- Disregard error messages because they are expected -------------------------------");
                
        for (File approvedPdf : testPdfFiles)
        {
            if (approvedPdf.getName().contains(exclude))
            {
                continue;
            }
            //Setup generated PDF
            String formMLFilePath = approvedPdf.getPath().replace(".pdf", ".xml");
            String xml = ServiceUtil.readFileAsStringUsingAbsolutePath(formMLFilePath);
            String generatedPdfFileName = GENERATED_FILENAME_PREFIX + approvedPdf.getName();
            String[] ids = ServiceUtil.getIds(xml);
            if (ids == null)
            {
                continue;
            }
            WebResponse response = ServiceUtil.postMethodWebServiceResponse(xml.getBytes());
            assertNotNull("Did not receive a response from the service.", response);


            //verify valid response
            if ((ids == null) || (response.getResponseCode() != EXPECTED_RESPONSE_CODE))
            {
                success = false;
                invalidFormML.add(approvedPdf.getName().replace(".pdf", ".xml"));
                System.out.println("X-FPS-Error header: " +
                        response.getHeaderField("X-FPS-Error"));
            }
            else
            {
                InputStream is = response.getInputStream();
                assertNotNull("Response did not contain an Input Stream", is);
                File generatedPdf = ServiceUtil.createPdfFile(is, generatedPdfFileName);
    
                //Test equivalence
                PdfToImageConverter imageConverter = new GhostscriptPdfToImageConverter(approvedPdf, generatedPdf);
                boolean equivalent = new PdfComparator(imageConverter).compare();
                
                if (!equivalent)
                {
                    success = false;
                    failedApprovedPdfs.add(approvedPdf.getName());
                }
                
                is.close();
            }
        }
        
        logger.info("\n-------------------------------------------------------------------------------------------------------------\n");
        
        assertTrue("\nThe service was unable to process the following formML files: \n\t" + invalidFormML.toString(), invalidFormML.size() == 0);
        
        assertFalse("\nThe rendered images of the following approved PDFs have not changed as expected: \n\t" + failedApprovedPdfs.toString(), success);

    }
}

