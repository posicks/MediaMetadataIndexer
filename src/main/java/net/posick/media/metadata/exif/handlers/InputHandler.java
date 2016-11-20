package net.posick.media.metadata.exif.handlers;

import java.io.IOException;

import net.posick.media.metadata.Context;

/**
 * The InputHandler is the base Handler class, defining the API for InputHandler.
 * 
 * @author posicks
 */
@SuppressWarnings("rawtypes")
public abstract class InputHandler extends AbstractHandler
{
    protected FileHandler fileHandler;
    
    
    /* (non-Javadoc)
     * @see net.posick.media.metadata.exif.handlers.AbstractHandler
     */
    protected InputHandler(Context ctx)
    {
        super(ctx);
    }


    /**
     * Sets the FileHandler
     * 
     * @param fileHandler The FileHandler
     */
    public void setFileHandler(FileHandler fileHandler)
    {
        this.fileHandler = fileHandler;
    }
    
    
    /**
     * The process method is executed to initiate the reading of the input source and start
     * propagation of data through the application to the output. 
     * 
     * @param inputUri The URI to the input data source.
     * @throws IllegalArgumentException
     * @throws IOException
     */
    public abstract void process(String inputUri)
    throws IllegalArgumentException, IOException;
}
