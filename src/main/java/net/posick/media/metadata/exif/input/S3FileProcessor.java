package net.posick.media.metadata.exif.input;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.posick.media.metadata.exif.Context;
import net.posick.media.metadata.exif.handlers.FileHandler;

@SuppressWarnings("rawtypes")
public class S3FileProcessor implements FileProcessor
{
    private static Logger logger = Logger.getLogger(S3FileProcessor.class.getName());
    
    private FileHandler fileHandler;


    public S3FileProcessor(Context ctx)
    {
    }


    @Override
    public void setFileHandler(FileHandler fileHandler)
    {
        this.fileHandler = fileHandler;
    }
    
    
    @Override
    public void processFile(String s3Uri, String key, InputStream in)
    {
        try
        {
            fileHandler.processFile(key, in);
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
}
