package net.posick.media.metadata.exif.input;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import net.posick.media.metadata.Context;
import net.posick.media.metadata.exif.handlers.FileHandler;
import net.posick.media.metadata.exif.handlers.InputHandler;

/**
 * The S3BucketHandler is an InputHandler that retrieves the file listing and files from an S3 API data source
 * 
 * @author posicks
 */
@SuppressWarnings("rawtypes")
public class S3BucketHandler extends InputHandler
{
    /**
     * SAX Parser interface.
     * 
     * SAX is used so that as the document is parsed FileHandlers can be executed concurrently with each other 
     * and the parsing of the file listing.
     * 
     * @author posicks
     */
    private static class S3Parser implements ContentHandler
    {
        private static Logger logger = Logger.getLogger(S3Parser.class.getName());
        
        private List<Pattern> filters;
        
        private boolean processingKey = false;
        
        private ExecutorService executor;
        
        private List<Future<Boolean>> futures = new LinkedList<>();

        private String uri;

        private CloseableHttpClient httpClient;

        private FileHandler fileHandler;

        
        private S3Parser(Context ctx, CloseableHttpClient httpClient, String uri, List<Pattern> filters, FileHandler fileHandler)
        {
            this.httpClient = httpClient;
            executor = Executors.newWorkStealingPool(ctx.get(Context.MAX_THREADS));
            this.executor = new ForkJoinPool(ctx.get(Context.MAX_THREADS), ForkJoinPool.defaultForkJoinWorkerThreadFactory, (Thread t, Throwable e) -> 
            {
                logger.log(Level.WARNING, String.format("Error occurred while processing S3 bucket: %s - %s", e.getClass().getSimpleName(), e.getMessage()));
            }, true);
            this.uri = uri;
            this.filters = filters;
            this.fileHandler = fileHandler;
        }
        
        
        @Override
        public void setDocumentLocator(Locator locator)
        {
        }
        
        
        @Override
        public void startDocument()
        throws SAXException
        {
        }
        
        
        @Override
        public void endDocument()
        throws SAXException
        {
        }
        
        
        @Override
        public void startPrefixMapping(String prefix, String uri)
        throws SAXException
        {
        }
        
        
        @Override
        public void endPrefixMapping(String prefix)
        throws SAXException
        {
        }
        
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts)
        throws SAXException
        {
            switch (qName)
            {
                case "Key":
                    processingKey = true;
                    break;
            }
        }
        
        
        @Override
        public void endElement(String uri, String localName, String qName)
        throws SAXException
        {
            switch (qName)
            {
                case "Key":
                    processingKey = false;
                    break;
            }
        }
        
        
        @Override
        public void characters(char[] ch, int start, int length)
        throws SAXException
        {
            // Call the FileHandler for each file found in the file listings
            if (processingKey)
            {
                String key = new String(ch, start, length).trim();
                if (filters != null && filters.size() > 0)
                {
                    // If filers were provided, only process files that, match one of the specified filters 
                    for (Pattern p : filters)
                    {
                        Matcher m = p.matcher(key);
                        if (m.matches())
                        {
                            // Schedule the execution of the FileHandler in another Thread
                            futures.add(executor.submit(new S3FileRunner(httpClient, uri, key, fileHandler), true));
                        }
                    }
                } else
                {
                    // Schedule the execution of the FileHandler in another Thread
                    futures.add(executor.submit(new S3FileRunner(httpClient, uri, key, fileHandler), true));
                }
            }
        }
        
        
        @Override
        public void ignorableWhitespace(char[] ch, int start, int length)
        throws SAXException
        {
        }
        
        
        @Override
        public void processingInstruction(String target, String data)
        throws SAXException
        {
        }
        
        
        @Override
        public void skippedEntity(String name)
        throws SAXException
        {
        }
    }
    
    
    /**
     * The S3FileRunner is a Runnable to use to execute the file retrieval and parsing processes concurrently
     * 
     * @author posicks
     */
    private static class S3FileRunner implements Runnable
    {
        private static Logger logger = Logger.getLogger(S3FileRunner.class.getName());
        
        private CloseableHttpClient httpClient;
        
        private String uri;
        
        private String key;
        
        private FileHandler fileHandler;

        
        public S3FileRunner(CloseableHttpClient httpClient, String uri, String key, FileHandler fileHandler)
        {
            this.httpClient = httpClient;
            this.uri = uri;
            this.key = key;
            this.fileHandler = fileHandler;
        }
        
        
        @Override
        public void run()
        {
            String s3Uri = this.uri;
            s3Uri += s3Uri.endsWith("/") ? key : "/" + key;
            HttpGet httpGet = new HttpGet(s3Uri);
            CloseableHttpResponse response = null;
            try
            {
                response = httpClient.execute(httpGet);
                HttpEntity entity = response.getEntity();
                
                int respCode = response.getStatusLine().getStatusCode();
                if (respCode == 200)
                {
                    // If the file was successfully retrieved get its InputStream and send it to the FileProcessor.
                    InputStream in = entity.getContent();
                    try
                    {
                        fileHandler.process(s3Uri, in);
                    } catch (Exception e)
                    {
                        logger.log(Level.WARNING, String.format("Error reading file \"%s\": %s - %s", uri, e.getClass().getSimpleName(), e.getMessage()));
                    }
                } else
                {
                    // If the file is unreadable send the error to the File processor
                    logger.log(Level.WARNING, String.format("File unreadable %s: HTTP Request was not successful. Responce Code was %s", s3Uri, "" + respCode));
                }
            } catch (ClientProtocolException e)
            {
                logger.log(Level.WARNING, String.format("Error executing File handler %s: %s", e.getClass().getSimpleName(), e.getMessage()), e);
            } catch (IOException e)
            {
                logger.log(Level.WARNING, String.format("Error executing File handler %s: %s", e.getClass().getSimpleName(), e.getMessage()), e);
            } finally
            {
                if (response != null)
                {
                    try
                    {
                        response.close();
                    } catch (IOException e)
                    {
                        logger.log(Level.FINE, "Error closing HTTP response", e);
                    }
                }
            }
        }
    }
        
    
    private PoolingHttpClientConnectionManager httpConnectionManager;
    
    private CloseableHttpClient httpClient;
    
    private XMLReader xmlReader;

    private S3Parser s3Parser;
    
    private int maxThreads;
    
    
    public S3BucketHandler(Context ctx)
    throws ParserConfigurationException, SAXException
    {
        super(ctx);
        this.maxThreads = ctx.get(Context.MAX_THREADS);
        
        httpConnectionManager = new PoolingHttpClientConnectionManager();
        httpConnectionManager.setMaxTotal(maxThreads);
        this.httpClient = HttpClients.custom().setConnectionManager(httpConnectionManager).build();
        
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();
        this.xmlReader = xmlReader;
    }
    
    
    @Override
    public void process(String inputUri)
    throws IOException
    {
        // Retrieve the file listings and create a SAX Parser to parse the file keys out of the listings.
        // The SAX ContentHandler will launch the file processing operations concurrently as files are found.
        xmlReader.setContentHandler(this.s3Parser = new S3Parser(ctx, httpClient, inputUri, ctx.get(Context.FILE_FILTERS), fileHandler));
        
        HttpGet httpGet = new HttpGet(inputUri);
        httpGet.addHeader("Accept", "application/xml");
        CloseableHttpResponse response = httpClient.execute(httpGet);
        try
        {
            HttpEntity entity = response.getEntity();
            
            int respCode = response.getStatusLine().getStatusCode();
            if (respCode == 200)
            {
                // File listings returned
                InputStream in = entity.getContent();
                Header contentEncoding = entity.getContentType();
                // Verify the Content Type
                switch (contentEncoding.getValue())
                {
                    case "application/xml":
                        try
                        {
                            xmlReader.parse(new InputSource(in));
                        } catch (UnsupportedOperationException | SAXException e)
                        {
                            throw new IOException(e.getMessage(), e);
                        }
                        break;
                }
            } else
            {
                logger.log(Level.WARNING, "HTTP Request was not successful. Responce Code was \"" + respCode + "\"");
            }
        } finally
        {
            if (response != null)
            {
                response.close();
            }
        }
        
        // Wait for all file retrieval and parse tasks to complete.
        List<Future<Boolean>> futures = s3Parser.futures;
        if (futures != null && futures.size() > 0)
        {
            for (Future<Boolean> future : futures)
            {
                try
                {
                    // Discard the results, the return value just allows us to wait for the Futures to complete executing.
                    if (!future.isDone() && !future.isCancelled())
                    {
                        future.get();
                    }
                } catch (InterruptedException e)
                {
                    logger.log(Level.FINE, "File processor interrupted");
                } catch (ExecutionException e)
                {
                    logger.log(Level.FINE, String.format("Error executing File processor %s: %s", e.getClass().getSimpleName(), e.getMessage()));
                }
            }            
        } else
        {
            logger.logp(Level.INFO, getClass().getName(), "process", String.format("No files found from \"%s\"", inputUri));
        }
    }
    
    
    @Override
    public void close()
    throws IOException
    {
        if (httpClient != null)
        {
            httpClient.close();
        }
    }
}
