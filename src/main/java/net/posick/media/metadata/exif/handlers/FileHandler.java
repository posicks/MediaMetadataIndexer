package net.posick.media.metadata.exif.handlers;

import java.io.IOException;
import java.io.InputStream;

import net.posick.media.metadata.exif.Context;


public abstract class FileHandler<T> extends AbstractHandler
{
    protected OutputHandler<T> outputHandler;
    
    
    protected FileHandler(Context ctx)
    {
        super(ctx);
    }


    public void setOutputHandler(OutputHandler<T> outputHandler)
    {
        this.outputHandler = outputHandler;
    }
    
    
    public abstract void processFile(String key, InputStream in)
    throws IOException;
}
