package net.posick.media.metadata.exif.output;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;

import net.posick.media.metadata.Context;
import net.posick.media.metadata.exif.handlers.OutputHandler;

public class CSQLOutputHandler extends OutputHandler<Map<String, Map<Integer, Map<String, Object>>>>
{
    private final String createExifTablespace = "CREATE KEYSPACE IF NOT EXISTS exif WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 3 } AND DURABLE_WRITES = true";
    
    private final String createExifMetadataTable = "CREATE TABLE IF NOT EXISTS exif.metadata (key varchar, directory varchar, tag int, tag_name varchar, value varchar, description varchar, PRIMARY KEY (key, directory))";
    
    private final String insertMetadata = "INSERT INTO exif.metadata (key, directory, tag, tag_name, value, description) VALUES (?, ?, ?, ?, ?, ?)";

    
    private Cluster cluster;
    
    private Session session;
    
    private PreparedStatement insertStatement;
    
    
    public CSQLOutputHandler(Context ctx)
    {
        super(ctx);
        cluster = Cluster.builder().addContactPoint(ctx.get(Context.DATASTORE_URI)).build();
        final Metadata metadata = cluster.getMetadata();
        System.out.printf("Connected to cluster: %s\n", metadata.getClusterName());
        for (final Host host : metadata.getAllHosts())
        {
            System.out.printf("Datacenter: %s; Host: %s; Rack: %s\n", host.getDatacenter(), host.getAddress(), host.getRack());
        }
        
//        cluster.getConfiguration().getCodecRegistry().register(TypeCodec.cint()).register(TypeCodec.varchar()).register(TypeCodec.smallInt()).register(TypeCodec.varint()).register(TypeCodec.decimal()).register(TypeCodec.bigint());

        Session session = cluster.connect();
        session.execute(createExifTablespace);
        session.execute(createExifMetadataTable);
        this.session = session;
        this.insertStatement = session.prepare(insertMetadata);
    }

    
    @Override
    public void output(String key, Map<String, Map<Integer, Map<String, Object>>> data)
    throws IOException
    {
        Set<Map.Entry<String, Map<Integer, Map<String, Object>>>> directories = data.entrySet();
        for (Map.Entry<String, Map<Integer, Map<String, Object>>> directoryEntry : directories)
        {
            String directoryName = directoryEntry.getKey();
            Map<Integer, Map<String, Object>> directory = directoryEntry.getValue();
            
            Set<Map.Entry<Integer, Map<String, Object>>> tagEntries = directory.entrySet();
            for (Map.Entry<Integer, Map<String, Object>> tagEntry : tagEntries)
            {
                Object value;
                Integer tag = tagEntry.getKey();
                Map<String, Object> valuesMap = tagEntry.getValue();
                
                Object obj = valuesMap.get("Value");
                if (obj != null)
                {
                    Class<?> clazz = obj.getClass();
                    if (clazz.isArray())
                    {
                        if (byte[].class.isAssignableFrom(clazz))
                        {
                            value = Arrays.toString((byte[]) obj);
                        } else if (short[].class.isAssignableFrom(clazz))
                        {
                            value = Arrays.toString((short[]) obj);
                        } else if (int[].class.isAssignableFrom(clazz))
                        {
                            value = Arrays.toString((int[]) obj);
                        } else if (long[].class.isAssignableFrom(clazz))
                        {
                            value = Arrays.toString((long[]) obj);
                        } else if (float[].class.isAssignableFrom(clazz))
                        {
                            value = Arrays.toString((float[]) obj);
                        } else if (double[].class.isAssignableFrom(clazz))
                        {
                            value = Arrays.toString((double[]) obj);
                        } else if (char[].class.isAssignableFrom(clazz))
                        {
                            value = Arrays.toString((char[]) obj);
                        } else
                        {
                            value = Arrays.toString((Object[]) obj);
                        }
                    } else
                    {
                        value = obj;
                    }
                } else
                {
                    value = null;
                }
                
                Session session = cluster.connect();
                try
                {
                    session.execute(insertStatement.bind().setString(0, key)
                                          .setString(1, directoryName)
                                          .setInt(2, tag)
                                          .setString(3, (String) valuesMap.get("TagName"))
                                          .setString(4, value.toString())
                                          .setString(5, (String) valuesMap.get("Description")));
                } finally
                {
                    session.close();
                }
                
                if (logger.isLoggable(Level.FINEST))
                {
                    logger.logp(Level.FINEST, LOG_CLASS_NAME, "output", String.format("Directory [%s] - %s (%s) = %s (%s)\n", directoryName, tag, valuesMap.get("TagName"), value, valuesMap.get("Description")));
                }
            }
        }
    }
    
    
    @Override
    public void close()
    throws IOException
    {
        if (session != null)
        {
            try
            {
                session.close();
            } catch (Exception e)
            {
                logger.log(Level.FINER, "Error closing Session");
            }
        }
        
        if (cluster != null)
        {
            try
            {
                cluster.close();
            } catch (Exception e)
            {
                logger.log(Level.FINER, "Error closing Cluster");
            }
        }
    }
}
