package net.posick.media.metadata.exif.handlers;

import java.io.IOException;

import net.posick.media.metadata.exif.Context;

/**
 * @author posicks
 */
public abstract class InputHandler extends AbstractHandler
{
    protected FileHandler fileHandler;
    
    
    protected InputHandler(Context ctx)
    {
        super(ctx);
    }


    public void setFileHandler(FileHandler fileHandler)
    {
        this.fileHandler = fileHandler;
    }
    
    
    public abstract void process(String inputUri)
    throws IllegalArgumentException, IOException;
}
