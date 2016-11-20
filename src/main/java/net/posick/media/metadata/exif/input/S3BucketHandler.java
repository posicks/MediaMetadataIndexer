package net.posick.media.metadata.exif.input;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import net.posick.media.metadata.exif.handlers.InputHandler;

/**
 * @author posicks
 */
public class S3BucketHandler extends InputHandler implements FileProcessor
{
    private static class S3Parser implements ContentHandler
    {
        private List<Pattern> filters;
        
        private boolean processingKey = false;
        
        private List<S3FileRunner> fileRunners = new LinkedList<>();

        private String uri;

        private CloseableHttpClient httpClient;

        private FileProcessor fileProcessor;

        
        private S3Parser(CloseableHttpClient httpClient, String uri, List<Pattern> filters, FileProcessor fileProcessor)
        {
            this.httpClient = httpClient;
            this.uri = uri;
            this.filters = filters;
            this.fileProcessor = fileProcessor;
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
            if (processingKey)
            {
                String key = new String(ch, start, length).trim();
                if (filters != null && filters.size() > 0)
                {
                    for (Pattern p : filters)
                    {
                        Matcher m = p.matcher(key);
                        if (m.matches())
                        {
                            fileRunners.add(new S3FileRunner(httpClient, uri, key, fileProcessor));
                        }
                    }
                } else
                {
                    fileRunners.add(new S3FileRunner(httpClient, uri, key, fileProcessor));
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
    
    
    private static class S3FileRunner implements Runnable
    {
        private static Logger logger = Logger.getLogger(S3FileRunner.class.getName());
        
        private CloseableHttpClient httpClient;
        
        private String uri;
        
        private String key;
        
        private FileProcessor fileProcessor;

        
        public S3FileRunner( CloseableHttpClient httpClient, String uri, String key, FileProcessor fileProcessor)
        {
            this.httpClient = httpClient;
            this.uri = uri;
            this.key = key;
            this.fileProcessor = fileProcessor;
        }
        
        
        @Override
        public void run()
        {
            CloseableHttpClient client = httpClient; // put httpClient into method stack, reducing ops needed to access
            
            String s3Uri = this.uri;
            s3Uri += s3Uri.endsWith("/") ? key : "/" + key;
            HttpGet httpGet = new HttpGet(s3Uri);
            CloseableHttpResponse response = null;
            try
            {
                response = client.execute(httpGet);
                HttpEntity entity = response.getEntity();
                
                int respCode = response.getStatusLine().getStatusCode();
                if (respCode == 200)
                {
                    InputStream in = entity.getContent();
                    fileProcessor.processFile(s3Uri, key, in);
                } else
                {
                    fileProcessor.fileUnreadable(s3Uri, "HTTP Request was not successful. Responce Code was \"" + respCode + "\"");
                }
            } catch (ClientProtocolException e)
            {
                fileProcessor.error(s3Uri, e);
            } catch (IOException e)
            {
                fileProcessor.error(s3Uri, e);
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
        xmlReader.setContentHandler(this.s3Parser = new S3Parser(httpClient, inputUri, ctx.get(Context.FILE_FILTERS), this));
        
        HttpGet httpGet = new HttpGet(inputUri);
        httpGet.addHeader("Accept", "application/xml");
        CloseableHttpResponse response = httpClient.execute(httpGet);
        try
        {
            HttpEntity entity = response.getEntity();
            
            int respCode = response.getStatusLine().getStatusCode();
            if (respCode == 200)
            {
                InputStream in = entity.getContent();
                Header contentEncoding = entity.getContentType();
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
        
        List<S3FileRunner> fileRunners = s3Parser.fileRunners;
        if (fileRunners != null && fileRunners.size() > 0)
        {
            // Launch file retrieval operations in parallel 
            ExecutorService executor = Executors.newWorkStealingPool(maxThreads);
            List<Future<Boolean>> futures = new LinkedList<>();
            for (Runnable fileRunner : fileRunners)
            {
                futures.add(executor.submit(fileRunner, true));
            }
            
            // Wait for tasks to complete.
            for (Future<Boolean> future : futures)
            {
                try
                {
                    future.get();
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
    public void processFile(String s3Uri, String key, InputStream in)
    {
        try
        {
            fileHandler.process(key, in);
        } catch (Exception e)
        {
            logger.log(Level.WARNING, String.format("Error reading file \"%s\": %s - %s", s3Uri, e.getClass().getSimpleName(), e.getMessage()));
        }
    }


    @Override
    public void fileUnreadable(String s3Uri, String message)
    {
        logger.log(Level.WARNING, String.format("File unreadable %s: %s", s3Uri, message));
    }
    
    
    @Override
    public void error(String uri, Throwable e)
    {
        logger.log(Level.WARNING, String.format("Error executing File processor %s: %s", e.getClass().getSimpleName(), e.getMessage()));
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
