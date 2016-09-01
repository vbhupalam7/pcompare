package com.service.regression;

import java.io.File;

import org.pdfbox.pdmodel.PDPage;

/**
 * Compares two PDF files
 * @author vbhupalam
 */
public interface PdfToImageConverter
{
    public File convertSourcePageToImage(int pageNumber) throws Exception;
    public File convertCopyPageToImage(int pageNumber) throws Exception;
    public File getSourcePdfFile();
    public File getCopyPdfFile();
    public int getSourceNumPages();
    public int getCopyNumPages();
    public void close() throws Exception;
}