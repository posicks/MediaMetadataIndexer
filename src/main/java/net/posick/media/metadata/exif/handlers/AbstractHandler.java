package net.posick.media.metadata.exif.handlers;

import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Logger;

import net.posick.media.metadata.exif.Context;

public abstract class AbstractHandler implements Closeable
{
    protected final Logger logger = Logger.getLogger(getClass().getName());
    
    protected final String LOG_CLASS_NAME = getClass().getSimpleName();
    
    protected Context ctx;


    protected AbstractHandler(Context ctx)
    {
        this.ctx = ctx;
    }
    
    /*
    public SessionContext newSessionContext()
    {
        return new SessionContext(ctx);
    }
    */
    
    public void close()
    throws IOException
    {
    }
}
