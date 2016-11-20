package net.posick.media.metadata.exif;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unchecked")
public class Context
{
    public static final String MAX_THREADS = "threads.max";

    public static final String INPUT_URI = "input.uri";
    
    public static final String DATASTORE_URI = "datastore.uri";
    
    public static final String INPUT_HANDLER_CLASS = "input.handler.class";
    
    public static final String FILE_HANDLER_CLASS = "file.handler.class";
    
    public static final String FILE_FILTERS = "filters.file";
    
    public static final String OUTPUT_HANDLER_CLASS = "output.handler.class";

    public static final String SESSION_PREFIX = "session.context.";
    
    public static final String SESSION = SESSION_PREFIX + "session";

    
    protected LinkedHashMap<String, Object> properties = new LinkedHashMap<String, Object>();
    
    
    public Context()
    {
    }
    
    
    public Context(Map<String, Object> properties)
    {
        this.properties.putAll(properties);
    }
    
    
    protected Context(Context ctx)
    {
        this.properties.putAll(ctx.properties);
    }


    public Set<String> keySet()
    {
        return properties.keySet();
    }


    public boolean containsKey(String key)
    {
        return properties.containsKey(key);
    }


    public <T> T remove(String key)
    {
        return (T) properties.remove(key);
    }


    public <T extends Object> T put(String key, T value)
    {
        return (T) properties.put(key, value);
    }


    public <T> T  putIfAbsent(String key, T value)
    {
        return (T) properties.putIfAbsent(key, value);
    }


    public <T extends Object> T get(String key)
    {
        return (T) properties.get(key);
    }
    
    
    public <T extends Object> T get(String key, T defaultValue)
    {
        if (properties.containsKey(key))
        {
            return (T) properties.get(key);
        }
        return defaultValue;
    }
    
    
    @Override
    public String toString()
    {
        return properties.toString();
    }
}
