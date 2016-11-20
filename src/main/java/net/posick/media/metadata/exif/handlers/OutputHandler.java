package net.posick.media.metadata.exif.handlers;

import java.io.IOException;

import net.posick.media.metadata.exif.Context;

/**
 * @author posicks
 */
public abstract class OutputHandler<T> extends AbstractHandler
{
    protected OutputHandler(Context ctx)
    {
        super(ctx);
    }
    
    
    public abstract void output(String uri, T metadata)
    throws IOException;
}
