
package com.service.regression;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Compares two PDF files
 * @author vbhupalam
 */
public final class PdfComparator
{
    private static FPSLogger logger = FPSLogger.getLogger(PdfComparator.class);
    private static String sourceFileName = null;
    private static String copyFileName = null;
    private static final String OUTPUT_PATH = "." + File.separator;
    // ImageMagick's compare executable
    private static final String COMPARE_PROG = "compare";
    PdfToImageConverter imageConverter;
    
    public PdfComparator(PdfToImageConverter imageConverter) throws Exception
    {
        this.imageConverter = imageConverter;
    }

    /**
     * Compares two PDF file and specifies whether or not they are equivalent
     * @param sourcePdfFile the source pdf file to compare
     * @param copyPdfFile the copy pdf file to compare
     * @return boolean that specifies whether or not the two pdf files are equivalent
     * @throws Exception than may occur
     */
    public boolean compare()
        throws Exception
    {
        boolean equivalent = true;

        sourceFileName = imageConverter.getSourcePdfFile().getPath();
        copyFileName = imageConverter.getCopyPdfFile().getPath();

        if (logger.isInfoEnabled())
        {
            logger.info("Start comparison of " + sourceFileName + " to " + copyFileName + ".");
        }
        
        try
        {
            if (imageConverter.getSourceNumPages() != imageConverter.getCopyNumPages())
            {
                logger.error("The number of pages contained in the compared PDFs are different." +
                        "\tSource total pages:\n\t\t" + imageConverter.getSourceNumPages() +
                        "\n\tCopy total pages:\n\t\t" + imageConverter.getCopyNumPages());
                return false;
            }

            for (int refPage = 0; refPage < imageConverter.getSourceNumPages(); refPage++)
            {
                int currentPage = refPage + 1;
                if (comparePages(refPage))
                {
                    if (logger.isInfoEnabled())
                    {
                        logger.info("Page " + (currentPage) + " is equivalent between " +
                                "Source File: " + sourceFileName +
                                " and Copy File: " + copyFileName);
                    }
                }
                else
                {
                    equivalent = false;
                    logger.error("Page " + (currentPage) + " is NOT equivalent between " +
                            "Source File: " + sourceFileName +
                            " and Copy File: " + copyFileName);
                }
            }
        }
        catch (Exception ex)
        {
            logger.error("A problem has occured in comparing the PDF Files", ex);
            throw ex;
        }
        finally
        {
            imageConverter.close();
        }

        return equivalent;
    }

    private boolean comparePages(int pageNumber) throws Exception
    {
        boolean equivalent = true;

        File sourceImageFile = imageConverter.convertSourcePageToImage(pageNumber);
        File copyImageFile = imageConverter.convertCopyPageToImage(pageNumber);
                
        if (!compareBytes(sourceImageFile, copyImageFile))
        {
            equivalent = false;
            createCompositeImage(sourceImageFile, copyImageFile);
        }
        
        return equivalent;
    }
    
    private boolean compareBytes(File sourceImageFile, File copyImageFile) throws Exception
    {
        boolean response = true;
        
        BufferedInputStream source = new BufferedInputStream(new FileInputStream(sourceImageFile));
        BufferedInputStream copy = new BufferedInputStream(new FileInputStream(copyImageFile));

        int sourceByte = source.read();
        int copyByte = copy.read();
        while ((sourceByte != -1) || (copyByte != -1))
        {
            if (sourceByte != copyByte)
            {
                response = false;
                logger.debug("There is a difference between the files: " +
                        sourceImageFile.getPath() + " and " + copyImageFile.getPath());
                System.out.println("There is a difference between the files: " +
                        sourceImageFile.getPath() + " and " + copyImageFile.getPath());
                break;
            }

            sourceByte = source.read();
            copyByte = copy.read();
        }
        
        source.close();
        copy.close();

        return response;
    }

    private void createCompositeImage(File sourceImageFile, File copyImageFile) throws Exception
    {
        String destName = sourceImageFile.getPath().replace(".png", ".diff.png");
        final long startTime = System.currentTimeMillis();
        boolean success = compare(sourceImageFile, copyImageFile, new File(destName));
        final long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("Creating image diff took " + elapsedTime + " ms");
        if (!success)
        {
            System.out.println("Could not create diff image file: " + destName);
            System.out.println("Continuing...");
        }
    }

    private static boolean compare(File src1, File src2, File dest)
    {
        return compare(src1, src2, dest, 75);
    }

    private static boolean compare(File src1, File src2, File dest, int quality)
    {
        //System.out.println("compare(" + src1.getPath() + ", " + src2.getPath() +
        //        ", " + dest + ", " + quality + ")");

        if (quality < 0 || quality > 100)
        {
            quality = 75;
        }

        ArrayList command = new ArrayList();

        command.add(COMPARE_PROG);
        command.add("-quality");
        command.add("" + quality);
        command.add(src1.getPath());
        command.add(src2.getPath());
        command.add(dest.getPath());

        //System.out.println(command);

        return exec((String[])command.toArray(new String[1]));
    }

    /**
     * Tries to exec the command, waits for it to finsih, logs errors if exit
     * status is nonzero, and returns true if exit status is 0 (success).
     *
     * @param command Description of the Parameter
     * @return Description of the Return Value
     */
    private static boolean exec(String[] command)
    {
        Process proc;

        try
        {
            proc = Runtime.getRuntime().exec(command);
        }
        catch (IOException e)
        {
            System.out.println("IOException while trying to execute " + command);
            return false;
        }

        System.out.println("Got process object, waiting to return.");

        int exitStatus;

        while (true)
        {
            try
            {
                exitStatus = proc.waitFor();
                break;
            }
            catch (java.lang.InterruptedException e)
            {
                System.out.println("Interrupted: Ignoring and waiting");
            }
        }

        if (exitStatus != 0)
        {
            System.out.println("Error executing command: " + exitStatus);
        }

        return (exitStatus == 0);
    }
}
