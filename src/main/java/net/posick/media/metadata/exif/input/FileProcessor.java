package net.posick.media.metadata.exif.input;

import java.io.InputStream;

/**
 * The FileProcessor interface defines a common API that can be used for file handling within the Input Handler.
 * The FileProcessor interface is provided to assist in the development of InputHandlers that use multiple threads
 * for file processing. 
 * 
 * @author posicks
 */
public interface FileProcessor
{
    
    
    /**
     * Called when file contents has been successfully retrieved.
     * 
     * @param uri The absolute URI to the file to be processed  
     * @param key The unique key for the file. May be the file name or a UUID
     * @param in The file contents in the form of an InputStream
     */
    public void processFile(String s3Uri, String key, InputStream in);


    /**
     * Called when the file contents are unreadable. This method should only be called when
     * the file contents cannot be read for security or other accessibility issues and not for
     * protocol or IO errors. 
     * 
     * @param uri The absolute URI to the file to be processed  
     * @param message A message describing the type of error that occured
     */
    public void fileUnreadable(String s3Uri, String message);
    
    
    /**
     * Called when a protocol or IO error occurs. 
     * 
     * @param uri The absolute URI to the file to be processed  
     * @param e The error that was thrown while attempting to process the file.
     */
    public void error(String uri, Throwable e);
}
