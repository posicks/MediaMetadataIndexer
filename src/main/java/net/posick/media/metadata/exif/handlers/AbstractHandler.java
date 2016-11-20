package net.posick.media.metadata.exif.handlers;

import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Logger;

import net.posick.media.metadata.Context;

/**
 * The AbstractHandler is the base class for the various Handlers used within the application. 
 * 
 * @author posicks
 */
public abstract class AbstractHandler implements Closeable
{
    protected final Logger logger = Logger.getLogger(getClass().getName());
    
    protected final String LOG_CLASS_NAME = getClass().getSimpleName();
    
    protected Context ctx;


    /**
     * Initializes the Handler with the Application Context.
     * 
     * @param ctx The Application Context
     */
    protected AbstractHandler(Context ctx)
    {
        this.ctx = ctx;
    }
    
    
    /* (non-Javadoc)
     * @see java.io.Closeable#close()
     */
    public void close()
    throws IOException
    {
    }
}
