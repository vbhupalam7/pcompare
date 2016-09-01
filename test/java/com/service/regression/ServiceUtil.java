
package com.service.regression;


import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BadPdfFormatException;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


/**
 * Provides methods required to conduct functional tests
 * @author vbhupalam
 */
public final class ServiceUtil
{
    /**Address where the request is posted to */
    // The default app server we will test against - latest service
    private static final String REV_DATE_FIELD_ID = "FpsRevDate";
    public static final String BASE_URI;
    private static final String POST_URI;
    private static final Method WAIT_FOR_EMBEDDED_SERVER;
    private static final int BUFFERSIZE = 10000;
    private static ServiceFactory serviceFactory = null;

    //logging where-ever the log4j configuration has pointed to, probably to the root
    private static FPSLogger logger = FPSLogger.getLogger(ServiceUtil.class);

    static
    {
        String prefixUri = null;
        Method waitForEmbeddedServer = null;

        try
        {
            String rootDir = System.getProperty("user.dir") + "/../";
            String contentDir = System.getProperty("fps.root.dir");

            if (contentDir == null)
            {
                File pdfContent = new File(rootDir + "pdf-content");

                System.setProperty("fps.root.dir", pdfContent.getCanonicalPath());
            }

            // Create a URL object on the root of the directory
            // containing the RestService class file's package
            String classDir = rootDir + "webservice/WEB-INF/";
            File printClasses = new File(classDir + "classes");
            File libDir = new File(classDir + "lib");
            File[] jars = libDir.listFiles();
            URL[] printURLs = new URL[jars.length + 1];
            int i = 0;

            for (i = 0; i < jars.length; i++)
            {
                printURLs[i] = jars[i].toURI().toURL();
            }

            printURLs[i] = printClasses.toURI().toURL();

            // Create a new class loader with the directory
            ClassLoader loader = new URLClassLoader(printURLs);

            // Load in the RestService class and invoke the embedded server
            Class restService = loader.loadClass("com.RestService");
            Method startEmbeddedServer = restService.getDeclaredMethod("startEmbeddedServer");
            prefixUri = (String)startEmbeddedServer.invoke(null);
            waitForEmbeddedServer = restService.getDeclaredMethod("waitForEmbeddedServer");

            // Get the rest service's service factory
            Method getEmbeddedServiceFactory = restService.getDeclaredMethod("getEmbeddedServiceFactory");
            serviceFactory = (ServiceFactory)getEmbeddedServiceFactory.invoke(null);

            //System.out.println("Embedded Service Factory = " + serviceFactory);
        }
        catch (Exception e)
        {
            e.printStackTrace(System.out);
            System.exit(-1);
        }

        WAIT_FOR_EMBEDDED_SERVER = waitForEmbeddedServer;
        BASE_URI = prefixUri;
        // The following string literal is parameterized for multiple content versions
        POST_URI = prefixUri + "/v1/generator/pdf?authid=regression_tests&appid=TEST";
    }

    private ServiceUtil()
    {
        //don't instantiate
    }

    static void waitForEmbeddedServer() throws Exception
    {
        WAIT_FOR_EMBEDDED_SERVER.invoke(null);
    }

    /**
     * @param xml some FormML with exactly one formset and one form
     * @return String[0] = the compatibilityVersion;
     *         String[1] = the formset id;
     *         String[2] = the form id ... null on error
     **/
    public static String[] getIds(String xml)
    {
        String[] retval = new String[3];

        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));
            NodeList formmls = doc.getElementsByTagName("FormML");
            int numFormmls = formmls.getLength();

            if (numFormmls != 1)
            {
                throw new IllegalArgumentException("Must have exactly one FormML. numFormmls=" + numFormmls);
            }

            Element formml = (Element)formmls.item(0);

            retval[0] = ContentVersion.determineCompatibilityVersion(formml.getAttribute("compatibilityVersion"));

            NodeList formsets = formml.getElementsByTagName("formset");
            int numFormsets = formsets.getLength();

            if (numFormsets != 1)
            {
                throw new IllegalArgumentException("Must have exactly one formset. numFormsets=" + numFormsets);
            }

            Element formset = (Element)formsets.item(0);

            retval[1] = formset.getAttribute("id");

            NodeList forms = formset.getElementsByTagName("form");
            int numForms = forms.getLength();

            if (numForms != 1)
            {
                throw new IllegalArgumentException("Must have exactly one form. numForms=" + numForms);
            }

            Element form = (Element)forms.item(0);

            retval[2] = form.getAttribute("id");
        }
        catch (Exception ex)
        {
            retval = null;
            ex.printStackTrace(System.out);
        }

        return retval;
    }

    public static Map<String, Date> loadExclusions(File rootDir)
        throws Exception
    {
        Map<String, Date> retval = new HashMap<String, Date>();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        File exclusions = new File(rootDir, "exclusions.xml");
        FileInputStream exclusionStream = new FileInputStream(exclusions);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(exclusionStream);
        NodeList excludes = doc.getElementsByTagName("exclude");
        int numExcludes = excludes.getLength();

        for (int excludeIndex = 0; excludeIndex < numExcludes; excludeIndex++)
        {
            Element exclude = (Element)excludes.item(excludeIndex);
            String fileName = exclude.getAttribute("file");
            String excludeUntil = exclude.getAttribute("excludeUntil");

            if (fileName == null || fileName.length() == 0 ||
                excludeUntil == null || excludeUntil.length() == 0)
            {
                throw new Exception("Exlusions file: " + exclusions.getAbsolutePath() +
                                    " <exclude> #" + (excludeIndex + 1) +
                                    " missing 'file' and/or 'excludeUntil' attribute");
            }

            retval.put(fileName, format.parse(excludeUntil));
        }

        return retval;
    }

    /**
     * Returns a WebResponse from the WebService using the Post Method
     * @param formML to generate a response from the web service
     * @return WebResponse from the web service
     * @throws Exception thrown by webservice
     */
    public static WebResponse postMethodWebServiceResponse(byte[] xml) throws Exception
    {
        WebResponse response = null;
        ByteArrayInputStream bis = new ByteArrayInputStream(xml);
        WebRequest request = new PostMethodWebRequest(POST_URI, bis, "application/xml");

        WebConversation conversation = new WebConversation();
        conversation.setExceptionsThrownOnErrorStatus(false);
        response = conversation.getResponse(request);

        // Sleep for half a sec to give the embedded server time to respond
        Thread.sleep(500);

        return response;
    }

    /**
     * Returns a WebResponse from the WebService using the Get Method
     * @param uri to generate a response from the web service
     * @return WebResponse from the web service
     * @throws Exception thrown by webservice
     */
    public static WebResponse getMethodWebServiceResponse(String uri) throws Exception
    {
        WebResponse response = null;
        WebRequest request = new GetMethodWebRequest(uri);

        WebConversation conversation = new WebConversation();
        conversation.setExceptionsThrownOnErrorStatus(false);
        response = conversation.getResponse(request);

        return response;
    }

    private static synchronized Service getService(String compatibilityVersion)
        throws Exception
    {
        if (serviceFactory == null)
        {
            System.out.println("Service Factory is null ... making a new one");
            serviceFactory = new ServiceFactory(System.getProperty("fps.root.dir"), null);
        }

        return serviceFactory.getService(compatibilityVersion);
    }

    /**
     * Creates and returns a PDF File from the passed in InputStream
     * @param is Input Stream to be converted to a pdf
     * @param filename to be created... input FILENAME creates FILENAME.pdf
     * @return the created PDF File or null if unable to create
     */
    public static  File createPdfFile(InputStream is, String filename)
    {
        File pdfFile = null;

        try
        {
            String pdfFileName = filename;

            if (!filename.endsWith(".pdf"))
            {
                pdfFileName += ".pdf";
            }

            pdfFile = new File(pdfFileName);

            byte[] buf = new byte[BUFFERSIZE];
            FileOutputStream fos = new FileOutputStream(pdfFile);
            BufferedInputStream bis = new BufferedInputStream(is);
            int numRead = -1;
            boolean didWriteFile = false;

            while ((numRead = bis.read(buf)) != -1)
            {
                fos.write(buf, 0, numRead);
                didWriteFile = true;
            }

            fos.close();
            bis.close();

            if (!didWriteFile)
            {
                System.out.println("Nothing to read in response entity-body in Uri: " + POST_URI + " for file: " + filename);
                pdfFile = null;
            }
        }
        catch (Exception ex)
        {
            logger.error("Caught exception for the uri: " + POST_URI + " for file: " + filename + "\n", ex);
            ex.printStackTrace(System.out);
            pdfFile = null;
        }

        return pdfFile;
    }
    
    /**
     * Creates and returns a PDF File from the passed in InputStream with FpsRevDate cleared
     * @param filename of existing PDF file to get FpsRevDate field removed
     * @param compatibilityVersion of the PDF being created
     * @param formsetId of the PDF being created
     * @param formId of the PDF being created
     * @return the created PDF File or null if unable to create
     */
    public static File createNoRevDatePdf(String filename, String compatibilityVersion, String formsetId, String formId)
    {
        File retval = null;
        try
        {
            Service service = getService(compatibilityVersion);
            FormTemplate formTemplate = service.getFormTemplate(formsetId, formId);
            String noRevDateFilename = filename.replace(".pdf", ".norevdate.pdf");
            RevDateVisitor visitor = new RevDateVisitor(formTemplate, filename, noRevDateFilename);
            FieldInfoRoot fieldInfo = formTemplate.getFieldInfo();
            fieldInfo.accept(visitor);
            retval = new File(noRevDateFilename);
        }
        catch (Exception e)
        {
            e.printStackTrace(System.out);
        }
        return retval;
    }

    private static class RevDateVisitor extends AbstractFieldInfoVisitor
    {
        private final FormTemplate formTemplate;
        private final PdfReader reader;
        private final String inFileName;
        private final String outFileName;
        
        private File pdfFile;
        private com.lowagie.text.Document pdfDoc;
        private PdfCopy writer;

        private PdfImportedPage[] page;
        private PdfCopy.PageStamp[] pageStamp;

        public RevDateVisitor(FormTemplate formTemplate, String inFileName, String outFileName)
        {
            PdfReader temp = null;
            this.formTemplate = formTemplate;
            try
            {
                temp = new PdfReader(inFileName);
            }
            catch (Exception e)
            {
                System.out.println(e.getMessage() + ". Cound not create PdfReader from " + inFileName);
            }
            this.reader = temp;
            this.inFileName = inFileName;
            this.outFileName = outFileName;
        }

        @Override
        public boolean enterForm(FieldInfoForm form)
        {
            int pageCount = form.getPageCount();
            this.pdfFile = new File(outFileName);
            this.pdfDoc = new com.lowagie.text.Document();
            try
            {
                this.writer = new PdfCopy(pdfDoc, new FileOutputStream(pdfFile));
                pdfDoc.open();
            }
            catch (FileNotFoundException exception)
            {
                logger.error("Caught exception for the uri: " + POST_URI + " for file: " + inFileName + "\n",
                    exception);
                exception.printStackTrace(System.out);
                pdfFile = null;
                return false;
            }
            catch (DocumentException exception)
            {
                logger.error("Caught exception for the uri: " + POST_URI + " for file: " + inFileName + "\n",
                    exception);
                exception.printStackTrace(System.out);
                pdfFile = null;
                return false;
            }

            page = new PdfImportedPage[pageCount + 1];
            pageStamp = new PdfCopy.PageStamp[pageCount + 1];
            for (int pageNumber = 1; pageNumber <= form.getPageCount(); pageNumber++)
            {
                page[pageNumber] = writer.getImportedPage(reader, pageNumber);
                pageStamp[pageNumber] = writer.createPageStamp(page[pageNumber]);
            }

            return true;
        }

        @Override
        public boolean exitForm(FieldInfoForm form)
        {
            for (int pageNumber = 1; pageNumber <= form.getPageCount(); pageNumber++)
            {
                try
                {
                    pageStamp[pageNumber].alterContents();
                    writer.addPage(page[pageNumber]);
                }
                catch (IOException exception)
                {
                    logger.error("Caught exception for the uri: " + POST_URI + " for file: " + inFileName + "\n",
                        exception);
                    exception.printStackTrace(System.out);
                    pdfFile = null;
                    return false;
                }
                catch (BadPdfFormatException exception)
                {
                    logger.error("Caught exception for the uri: " + POST_URI + " for file: " + inFileName + "\n",
                        exception);
                    exception.printStackTrace(System.out);
                    pdfFile = null;
                    return false;
                }
            }
            pdfDoc.close();
            return true;
        }

        @Override
        public boolean visitField(FieldInfoField field)
        {
            String fieldId = field.getFullId().getValue();
            if (REV_DATE_FIELD_ID.equals(fieldId))
            {
                int pageNumber = field.getPage();
                PdfContentByte content = pageStamp[pageNumber].getOverContent();

                float fieldX = formTemplate.getPageOriginX(pageNumber) + field.getX();
                float fieldY = formTemplate.getPageOriginY(pageNumber) + field.getY();
                Rectangle rect = new Rectangle(fieldX - 1, fieldY - 1, fieldX + field.getWidth()+ 2, fieldY + field.getHeight() + 2);

                rect.setBackgroundColor(Color.white);
                content.rectangle(rect);
            }
            return true;
        }

    }

    /**
     * returns the filepath as a String
     * @param filePath location of the file you want to read
     * @return the file as a String
     */
    public static String readFileAsString(String filePath)
    {
        String retVal = "";
        try
        {
            StringBuilder fileData = new StringBuilder(BUFFERSIZE);
            BufferedReader reader = new BufferedReader(new FileReader(getFile(filePath)));
            char[] buf = new char[BUFFERSIZE];
            int numRead = 0;
            while ((numRead = reader.read(buf)) != -1)
            {
                fileData.append(buf, 0, numRead);
            }
            reader.close();
            retVal = fileData.toString();
        }
        catch (IOException ex)
        {
            logger.error("unable to read File", ex);
        }
        return retVal;
    }

    /**
     * returns the filepath as a String
     * @param fileAbsolutePath the absolute filePath location of the file you want to read
     * @return the file as a String
     */
    public static String readFileAsStringUsingAbsolutePath(String fileAbsolutePath)
    {
        String retVal = "";
        try

        {
            StringBuilder fileData = new StringBuilder(BUFFERSIZE);
            BufferedReader reader = new BufferedReader(new FileReader(new File(fileAbsolutePath)));
            char[] buf = new char[BUFFERSIZE];
            int numRead = 0;
            while ((numRead = reader.read(buf)) != -1)
            {
                fileData.append(buf, 0, numRead);
            }
            reader.close();
            retVal = fileData.toString();
        }
        catch (IOException ex)
        {
            logger.error("unable to read File", ex);
        }
        return retVal;
    }

    /**
     * returns the filepath as a file from the class loader
     * @param filePath location of the file you want
     * @return the file
     */
    public static File getFile(String filePath)
    {
        File retVal;

        retVal = new File(ServiceUtil.class.getResource(filePath).getFile());

        return retVal;
    }

}
