package net.posick.media.metadata.exif.input;

import java.io.InputStream;

import net.posick.media.metadata.exif.handlers.FileHandler;

public interface FileProcessor
{
    public void setFileHandler(FileHandler fileHandler);
    
    
    public void processFile(String uri, String key, InputStream in);


    public void fileUnreadable(String s3Uri, String message);
    
    
    public void error(String uri, Throwable e);
}
