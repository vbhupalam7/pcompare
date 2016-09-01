
package com.intuit.service.fps.regression;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.lowagie.text.pdf.PdfReader;

/**
 * Compares two PDF files
  * @author vbhupalam
 */
public final class GhostscriptPdfToImageConverter implements PdfToImageConverter
{
    private static final FPSLogger logger =
        FPSLogger.getLogger(GhostscriptPdfToImageConverter.class);
    private static final String GS_PROG = "gs";
    private static final String OUTPUT_PATH = "." + File.separator;
    private static final String IMAGE_TYPE = "png";
    private static final String PAGENUM_SUFFIX = "_Page_%d";

    private final File sourcePdfFile;
    private final File copyPdfFile;
    private final int sourcePdfFileNumPages;
    private final int copyPdfFileNumPages;

    public GhostscriptPdfToImageConverter(File sourcePdfFile, File
            copyPdfFile) throws IOException
    {
        this.sourcePdfFile = sourcePdfFile;
        this.copyPdfFile = copyPdfFile;

        PdfReader sourcePdfReader = new PdfReader(sourcePdfFile.getPath());
        sourcePdfFileNumPages = sourcePdfReader.getNumberOfPages();
        sourcePdfReader.close();

        PdfReader copyPdfReader = new PdfReader(copyPdfFile.getPath());
        copyPdfFileNumPages = copyPdfReader.getNumberOfPages();
        copyPdfReader.close();

        generateImage(sourcePdfFile);
        generateImage(copyPdfFile);
    }

    public File getSourcePdfFile()
    {
        return sourcePdfFile;
    }

    public File getCopyPdfFile()
    {
        return copyPdfFile;
    }

    public int getSourceNumPages()
    {
        return sourcePdfFileNumPages;
    }

    public int getCopyNumPages()
    {
        return copyPdfFileNumPages;
    }

    public File convertSourcePageToImage(int refPageNumber) throws Exception
    {
        return getImageFileForPage(sourcePdfFile.getPath(), refPageNumber + 1);
    }

    public File convertCopyPageToImage(int refPageNumber) throws Exception
    {
        return getImageFileForPage(copyPdfFile.getPath(), refPageNumber + 1);
    }

    public void close() throws Exception
    {
        //if (sourceDoc != null)
        //{
        //    sourceDoc.close();
        //}
        //if (copyDoc != null)
        //{
        //    copyDoc.close();
        //}
    }

    private static File getImageFileForPage(String inFileName, int currentPageNumber) throws Exception
    {
        String fileName = getImageFilenameRoot(inFileName) + "_Page_" +
            Integer.toString(currentPageNumber) + "." + IMAGE_TYPE;

        return new File(fileName);
    }

    private static String getImageFilenameRoot(String name)
    {
        return name.substring(0, name.lastIndexOf('.'));
    }

    private static String getImageFilename(String name)
    {
        return getImageFilenameRoot(name) + PAGENUM_SUFFIX + "." + IMAGE_TYPE;
    }

    private static boolean generateImage(File src)
    {
        final String dest_fileName = getImageFilename(src.getPath());

        final ArrayList<String> command = new ArrayList<String>();
        command.add(GS_PROG);
        command.add("-q");
        command.add("-dSAFER");
        command.add("-dBATCH");
        command.add("-dNOPAUSE");
        command.add("-sDEVICE=png16m");
        command.add("-r144");
        command.add("-dTextAlphaBits=4");
        command.add("-dGraphicsAlphaBits=4");
        command.add("-dMaxStripSize=8192");
        command.add("-dUseCropBox");
        command.add("-sOutputFile=" + dest_fileName);
        command.add(src.getPath());

        //System.out.println("command=" + command);

        final long starttime = System.currentTimeMillis();
        final boolean retval = ExecUtil.exec((String[])command.toArray(new String[1]));
        final long duration = System.currentTimeMillis() - starttime;
        System.out.println("Generating " + dest_fileName + " took " + duration + " ms");
        return retval;
    }

}
