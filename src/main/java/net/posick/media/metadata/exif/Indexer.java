package net.posick.media.metadata.exif;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.posick.media.metadata.EXIT_CODES;
import net.posick.media.metadata.exif.handlers.FileHandler;
import net.posick.media.metadata.exif.handlers.InputHandler;
import net.posick.media.metadata.exif.handlers.OutputHandler;

/**
 * @author posicks
 */
@SuppressWarnings("rawtypes")
public class Indexer implements Callable<Integer>
{
    private static Logger logger = Logger.getLogger(Indexer.class.getName());
    
    private Context ctx;
    
    private InputHandler inputHandler;
    
    private FileHandler fileHandler;
    
    private OutputHandler outputHandler;
    
    
    public Indexer(Context ctx, InputHandler inputHandler, FileHandler<?> fileHandler, OutputHandler<?> outputHandler)
    {
        this.ctx = ctx;
        this.inputHandler = inputHandler;
        this.fileHandler = fileHandler;
        this.outputHandler = outputHandler;
    }


    @SuppressWarnings("unchecked")
    public Integer call()
    {
        String inputUri = ctx.get(Context.INPUT_URI);
        
        if (inputHandler != null)
        {
            try
            {
                inputHandler.setFileHandler(fileHandler);
                fileHandler.setOutputHandler(outputHandler);
                inputHandler.process(inputUri);
            } catch (IllegalArgumentException e)
            {
                logger.log(Level.SEVERE, String.format("Error processing directory listings for \"%s\" - %s: %s", inputUri, e.getClass().getSimpleName(), e.getMessage()), e);
                return EXIT_CODES.CONFIG_ERROR;
            } catch (IOException e)
            {
                logger.log(Level.SEVERE, String.format("Error processing directory listings for \"%s\" - %s: %s", inputUri, e.getClass().getSimpleName(), e.getMessage()), e);
                return EXIT_CODES.INPUT_ERROR;
            } finally
            {
                try
                {
                    inputHandler.close();
                } catch (IOException e)
                {
                    logger.log(Level.WARNING, String.format("Error closing InputHandler - %s: %s",e.getClass().getSimpleName(), e.getMessage()), e);
                }
            }
        } else
        {
            logger.logp(Level.SEVERE, getClass().getName(), "call", "No Input Handler Specified");
            return EXIT_CODES.CONFIG_ERROR;
        }
        
        
        return EXIT_CODES.OK;
    }
}
