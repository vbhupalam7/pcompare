
package com.service.regression;

import java.io.File;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Date;

import junit.framework.TestCase;

import com.meterware.httpunit.WebResponse;

/**
 * Test filling the forms of a PDF file from a map of values.
  * @author vbhupalam
 */
public class ApprovedFormsRegressionTest extends TestCase
{
    private static final Date TODAY = new Date();
    private static Map<String, Date> exclusions = null;
    private static List<File> approvedPdfFiles = new ArrayList<File>();
    private static final String APPROVED_PDF_CONTENT_ROOT_DIR = "/content/approved_forms";
    private static final int EXPECTED_RESPONSE_CODE = 200;
    private static FPSLogger logger = FPSLogger.getLogger(ApprovedFormsRegressionTest.class);

    static
    {
        try
        {
            File rootDir = ServiceUtil.getFile(APPROVED_PDF_CONTENT_ROOT_DIR);

            exclusions = ServiceUtil.loadExclusions(rootDir);
            loadApprovedPdfFiles(rootDir);
        }
        catch (Exception e)
        {
            e.printStackTrace(System.out);
            System.exit(-1);
        }
    }

    private static void loadApprovedPdfFiles(File directory)
    {
        if (directory.isDirectory())
        {
            File[] files = directory.listFiles();

            for (File file : files)
            {
                if (file.isDirectory())
                {
                    loadApprovedPdfFiles(file);
                }
                else if (file.isFile() && file.getName().endsWith(".pdf"))
                {
                    Date excludeUntil = exclusions.get(file.getName());

                    if (excludeUntil == null || TODAY.compareTo(excludeUntil) > 0)
                    {
                        approvedPdfFiles.add(file);
                        System.out.println("Added " + file);
                    }
                }
            }
        }
    }

    /**
     * Test that verifies that the generated pdfs are equivalent to the approved
     * pdfs
     * 
     * @throws Exception
     *             when exception occurs
     */
    public void testGeneratedPdfsEquivalentToApprovedPdfs() throws Exception
    {
        boolean success = true;
        List<String> failedApprovedPdfs = new ArrayList<String>();
        List<String> invalidFormML = new ArrayList<String>();

        for (File approvedPdf : approvedPdfFiles)
        {
            // Setup generated PDF
            String pdfPathName = approvedPdf.getPath();
            if (pdfPathName.contains("__skip__"))
            {
                continue;
            }
            System.out.println("\n\nregression testing " + pdfPathName);
            String formMLFilePath = pdfPathName.replace(".pdf", ".xml");
            String xml = ServiceUtil.readFileAsStringUsingAbsolutePath(formMLFilePath);
            String generatedPdfPathName = pdfPathName.replace(".pdf", ".generated.pdf");
            WebResponse response = ServiceUtil.postMethodWebServiceResponse(xml.getBytes());
            assertNotNull("Did not receive a response from the service.", response);

            // verify valid response
            if (response.getResponseCode() != EXPECTED_RESPONSE_CODE)
            {
                invalidFormML.add(formMLFilePath);
                System.out.println("X-FPS-Error header: " + response.getHeaderField("X-FPS-Error"));
                continue;
            }

            InputStream is = response.getInputStream();
            assertNotNull("Response did not contain an Input Stream", is);
            File generatedPdf = ServiceUtil.createPdfFile(is, generatedPdfPathName);

            String[] ids = ServiceUtil.getIds(xml);
            if (ids == null)
            {
                invalidFormML.add(formMLFilePath);
                continue;
            }

            // Blank out FpsRevDate
            File noRevDateOriginalPdf = ServiceUtil.createNoRevDatePdf(pdfPathName, ids[0], ids[1], ids[2]);
            File noRevDateGeneratedPdf = ServiceUtil.createNoRevDatePdf(generatedPdfPathName, ids[0], ids[1], ids[2]);

            // Test equivalence
            boolean equivalent = false;

            if (noRevDateOriginalPdf != null && noRevDateGeneratedPdf != null)
            {
                PdfToImageConverter imageConverter = new GhostscriptPdfToImageConverter(noRevDateOriginalPdf,
                        noRevDateGeneratedPdf);

                equivalent = new PdfComparator(imageConverter).compare();
            }

            if (!equivalent)
            {
                success = false;
                failedApprovedPdfs.add(pdfPathName);
            }

            is.close();
        }

        System.out.println(
            buildMessage("TEST FAILED: The rendered images of the following approved PDFs have changed:", failedApprovedPdfs) +
            buildMessage("TEST FAILED: The service was unable to process the following FormML files:", invalidFormML));
    }

    private String buildMessage(String header, List<String> errorList)
    {
        StringBuilder message = new StringBuilder();

        message.append("\n\n-------------------------------------------------------------------------------------");
        message.append("\n\t\t").append(header);

        for (String error : errorList)
        {
            message.append("\n\t\t\t").append(error);
        }

        message.append("\n-------------------------------------------------------------------------------------\n\n");

        return message.toString();
    }
}
