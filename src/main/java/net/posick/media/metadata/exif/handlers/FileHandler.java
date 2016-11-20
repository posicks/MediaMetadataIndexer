package net.posick.media.metadata.exif.handlers;

import java.io.IOException;
import java.io.InputStream;

import net.posick.media.metadata.Context;


/**
 * The FileHandler is the base Handler class, defining the API for FileHandlers.  
 *
 * @param <T> The data type to be exchanged between the FileHandler and the OutputHandler
 * 
 * @author posicks
 */
public abstract class FileHandler<T> extends AbstractHandler
{
    protected OutputHandler<T> outputHandler;
    
    
    /* (non-Javadoc)
     * @see net.posick.media.metadata.exif.handlers.AbstractHandler
     */
    protected FileHandler(Context ctx)
    {
        super(ctx);
    }


    /**
     * Sets the OutputHandler
     * 
     * @param outputHandler The OutputHandler
     */
    public void setOutputHandler(OutputHandler<T> outputHandler)
    {
        this.outputHandler = outputHandler;
    }
    
    
    /**
     * The process method is executed by the InputHandler for each input file/stream. This is where the magic happens 
     * and calls the OutputHandler's {@link OutputHandler#output(String, Object)} method.
     * 
     * @param key The unique key for the file. May be the file name or a UUID
     * @param in The InputStream containing the file contents
     * @throws IOException
     */
    public abstract void process(String key, InputStream in)
    throws IOException;
}
