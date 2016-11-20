package net.posick.media.metadata.exif.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

import net.posick.media.metadata.exif.Context;

public class MetadataExtractorFileHandler extends FileHandler<Map<String, Map<Integer, Map<String, Object>>>>
{
    public MetadataExtractorFileHandler(Context ctx)
    {
        super(ctx);
    }
    
    
    @Override
    public void processFile(String key, InputStream in)
    throws IOException
    {
        String datastoreUri = ctx.get(Context.DATASTORE_URI);
        try
        {
            Map<String, Map<Integer, Map<String, Object>>> metadataMap = new LinkedHashMap<>();
            Metadata metadata = ImageMetadataReader.readMetadata(in);
    
            System.out.println("File: " + ctx.get(Context.INPUT_URI) + "/" + key);
            for (Directory directory : metadata.getDirectories())
            {
                Map<Integer, Map<String, Object>> directoryMap = new LinkedHashMap<>();
                metadataMap.put(directory.getName(), directoryMap);
                
                for (Tag tag : directory.getTags())
                {
                    LinkedHashMap<String, Object> valueMap = new LinkedHashMap<>();
                    valueMap.put("TagName", tag.getTagName());
                    valueMap.put("Description", tag.getDescription());
                    valueMap.put("Value", directory.getObject(tag.getTagType()));
                    directoryMap.put(tag.getTagType(), valueMap);
                }
                
                try
                {
                    outputHandler.output(key, metadataMap);
                } catch (Exception e)
                {
                    logger.log(Level.WARNING, String.format("Error writing metadata to output \"%s\": %s - %s", datastoreUri, e.getClass().getSimpleName(), e.getMessage()), e);
                }
                
                if (directory.hasErrors())
                {
                    for (String error : directory.getErrors())
                    {
                        logger.log(Level.WARNING, String.format("Error parsing %s: %s", key, error));
                    }
                }
            }
        } catch (ImageProcessingException e)
        {
            logger.log(Level.WARNING, String.format("Error processing file \"%s\":", key), e);
        }
    }
}
