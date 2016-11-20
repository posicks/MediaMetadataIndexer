package net.posick.media.metadata;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.posick.media.metadata.exif.handlers.FileHandler;
import net.posick.media.metadata.exif.handlers.InputHandler;
import net.posick.media.metadata.exif.handlers.OutputHandler;

/**
 * The Indexer is the main component of the Metadata indexing application. It connects the 
 * Input, File, and Output Handlers together and initiates the metadata parsing process.
 * 
 * The Indexer is distinct from the application Main application class so that it may be used
 * within another application or initiated by a different application entry point, e.g, RESTful endpoint.
 * 
 * The Indexer implements the java.util.concurrent.Callable interface so that multiple Indexers
 * can be concurrently executed against different inputs.
 * 
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
    
    
    /**
     * Initializes the Indexer.
     * 
     * @param ctx The application Context
     * @param inputHandler The InputHandler
     * @param fileHandler The FileHandler
     * @param outputHandler The OutputHandler
     */
    public Indexer(Context ctx, InputHandler inputHandler, FileHandler<?> fileHandler, OutputHandler<?> outputHandler)
    {
        this.ctx = ctx;
        this.inputHandler = inputHandler;
        this.fileHandler = fileHandler;
        this.outputHandler = outputHandler;
    }


    /* (non-Javadoc)
     * @see java.util.concurrent.Callable#call()
     */
    @SuppressWarnings("unchecked")
    public Integer call()
    {
        String inputUri = ctx.get(Context.INPUT_URI);
        
        if (inputHandler != null)
        {
            try
            {
                // Connect the Handlers together
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
