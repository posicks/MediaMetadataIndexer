package net.posick.media.metadata;

/**
 * Interface that contains the exit code values returned by the application to indicate the exit status to executing scripts.
 * 
 * @author posicks
 */
public interface EXIT_CODES
{
    public static final int OK = 0;
    
    /**
     * A Configuration Error occurred
     */
    public static final int CONFIG_ERROR = -1;

    /**
     * An Initialization Error occurred
     */
    public static final int INITIALIZATION_ERROR = -2;
    
    /**
     * An Input Error occurred
     */
    public static final int INPUT_ERROR = -3;
    
    /**
     * An Output Error occurred
     */
    public static final int OUTPUT_ERROR = -4;
}
