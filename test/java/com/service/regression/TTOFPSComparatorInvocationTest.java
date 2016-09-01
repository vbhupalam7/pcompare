package com.service.regression;


import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 * Test to run regression of TBO vs EFConv
 * @author vbhupalam
 *
 */
public class TTOFPSComparatorInvocationTest extends TestCase
{
    private static final String TEST_PDF_CONTENT_ROOT_DIR = "target/test-classes/content/test_forms";
    private static String ttoServer = labserver;
    private static String fpsServer = labserver2;
  
    private static String TTO_Server_Start = "labserver_start";
    private static String FPS_Server_Start = "labserver_start2";
    private static List<String> failedApprovedPdfs = new ArrayList<String>();

    
    /**
     *Set Up to move/delete files before starting the test run
     */

    public void setUp()
    {
        
    	try
        {
    		File[] filesOriginal = new File(System.getProperty("PDFFilesPath")).listFiles(); 
    		File[] filesLatest = new File(System.getProperty("LatestFolder")).listFiles();
    		File[] filesBaseline = new File(System.getProperty("BaselineFolder")).listFiles();
    		
    		//moving files from one folder to another
		for (int i=0; i<filesOriginal.length-1; i++) 
    		{
    			String fileString = filesOriginal[i].getPath();
    			if(fileString.contains(".pdf"))
    			{	
    				boolean success = filesOriginal[i].renameTo(filesLatest[i]);
    				if (!success) 
    				{
    					System.out.println("The file\t" +fileString+ "\tcould not be moved");
    				}
    			}
    		}
    		
    		
		for (File fileLatest:filesLatest)
		{
            	String fileString = fileLatest.getPath();
			if(fileString.contains(".png") || fileString.contains(".ini"))
			{
            		fileLatest.delete();
				if (fileLatest.exists())
				{
					System.out.println("The file\t" +fileString+ "\tcould not be deleted");
				}
			}
		}
    		for (File fileBaseline:filesBaseline)
		{
            	String fileString = fileBaseline.getPath();
			if(fileString.contains(".png") || fileString.contains(".ini"))
			{
            		fileBaseline.delete();
				if (fileBaseline.exists())
				{
            			System.out.println("The file\t" +fileString+ "\tcould not be deleted");
				}
			}
		}
	}	
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    /*
     * Test to compare the output of desktop generated pdf's one run from another
     *

    public void testGetTTOFPSOutput()
    {
        boolean success = true;

        try
        {
		File[] filesLatest = new File(System.getProperty("LatestFolder")).listFiles();
       	File[] filesBaseline = new File(System.getProperty("BaselineFolder")).listFiles();
		System.out.println("The total number of files that would be compared is = "+Integer.toString(filesLatest.length-1));
			for (int i=0; i<filesLatest.length-1; i++) 
			{
				//Test equivalence
				PdfToImageConverter imageConverter = new GhostscriptPdfToImageConverter(filesLatest[i], filesBaseline[i]);
				boolean equivalent = new PdfComparator(imageConverter).compare();
				if (!equivalent)
				{
					success = false;
					failedApprovedPdfs.add(filesLatest[i].getName());
				}
				System.out.println("Current file being processed: " + filesLatest[i].getName());
			}	

            System.out.println("The comparison is over");
            assertTrue("\nThe rendered images of the following approved PDFs have changed as expected: \n\t"
                    + failedApprovedPdfs.toString(), success);
		}
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
	/
	/*
     * Test to compare the output of desktop generated pdf's one run from another
     */

    public void testGetTTORenameFPSOutput()
    {
        boolean success = true;

        try
        {
		File[] filesLatest = new File(System.getProperty("TYLatestPDF")).listFiles();
       	File[] filesBaseline = new File(System.getProperty("TYBaselinePDF")).listFiles();
		System.out.println("The total number of files that would be compared is = "+Integer.toString(filesLatest.length-1));
			for (int i=0; i<filesLatest.length-1; i++) 
			{
				//Test equivalence
				PdfToImageConverter imageConverter = new GhostscriptPdfToImageConverter(filesLatest[i], filesBaseline[i]);
				boolean equivalent = new PdfComparator(imageConverter).compare();
				if (!equivalent)
				{
					success = false;
					failedApprovedPdfs.add(filesLatest[i].getName());
				}
				System.out.println("Current file being processed: " + filesLatest[i].getName());
			}	

            System.out.println("The comparison is over");
            assertTrue("\nThe rendered images of the following approved PDFs have changed as expected: \n\t"
                    + failedApprovedPdfs.toString(), success);
		}
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}