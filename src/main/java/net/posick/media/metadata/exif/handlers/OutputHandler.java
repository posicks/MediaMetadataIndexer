package net.posick.media.metadata.exif.handlers;

import java.io.IOException;

import net.posick.media.metadata.Context;

/**
 * @param <T> The data type to be exchanged between the FileHandler and the OutputHandler
 * 
 * @author posicks
 */
public abstract class OutputHandler<T> extends AbstractHandler
{
    /* (non-Javadoc)
     * @see net.posick.media.metadata.exif.handlers.AbstractHandler
     */
    protected OutputHandler(Context ctx)
    {
        super(ctx);
    }
    
    
    /**
     * The process method is executed to initiate the reading of the input source and start
     * propagation of data through the application to the output.
     * 
     * The output method is executed by the FileHandler to send its data to the output, usually a datastore.
     * 
     * @param key The unique key for the file. May be the file name or a UUID
     * @param data The data produced by the FileHandler 
     * @throws IOException
     */
    public abstract void output(String key, T data)
    throws IOException;
}
