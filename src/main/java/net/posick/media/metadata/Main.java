package net.posick.media.metadata;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import net.posick.media.metadata.exif.Context;
import net.posick.media.metadata.exif.Indexer;
import net.posick.media.metadata.exif.handlers.FileHandler;
import net.posick.media.metadata.exif.handlers.InputHandler;
import net.posick.media.metadata.exif.handlers.MetadataExtractorFileHandler;
import net.posick.media.metadata.exif.handlers.OutputHandler;
import net.posick.media.metadata.exif.input.S3DirectoryHandler;
import net.posick.media.metadata.exif.output.CSQLOutputHandler;

/**
 * @author posicks
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class Main
{
    private static final int DEFAULT_PROCESSING_THREADS = 10;
    
    private static final Class<? extends FileHandler<?>> DEFAULT_FILE_HANDLER = MetadataExtractorFileHandler.class;
    
    private static final Class<? extends InputHandler> DEFAULT_INPUT_HANDLER = S3DirectoryHandler.class;
    
    private static final Class<? extends OutputHandler<?>> DEFAULT_OUTPUT_HANDLER = CSQLOutputHandler.class;
    
    private static Logger logger = Logger.getLogger(Main.class.getName());
    
    private static CommandLineParser cliParser = new DefaultParser();
    
    private static HelpFormatter formatter = new HelpFormatter();
    
    private static Options cliOptions = new Options();
    
    static
    {
        cliOptions.addOption(Option.builder("i").longOpt("input").hasArg().required().argName("Input URI").desc("Specifies the URI for the input directory listing").build());
        cliOptions.addOption(Option.builder("o").longOpt("output").hasArg().required().argName("Datasource URI").desc("Specifies the output URI to send EXIF metadata to").build());
        cliOptions.addOption(Option.builder("t").longOpt("threads").hasArg().argName("Processing Threads").desc("Specifies the maximum number of processing threads").build());
        cliOptions.addOption(Option.builder("f").longOpt("filter").hasArg().argName("Input URI").desc("Specifies a regular expression filter for selecting input files").build());
        cliOptions.addOption(Option.builder().longOpt("file_handler").hasArg().argName("File Handler").desc("Specifies the File Handler to be used to extract the EXIF metadata from the input media files").build());
        cliOptions.addOption(Option.builder().longOpt("input_handler").hasArg().argName("Input Handler").desc("Specifies the Input Handler to be used to read the input media files").build());
        cliOptions.addOption(Option.builder().longOpt("output_handler").hasArg().argName("Output Handler").desc("Specifies the Output Handler to be used to send the processed data to").build());
    }
    
    
    private static <T extends Object> T newHandlerInstance(Class<T> clazz, Context ctx)
    throws SecurityException, InstantiationException, IllegalAccessException, 
           IllegalArgumentException, InvocationTargetException
    {
        if (clazz != null)
        {
            try
            {
                Constructor<FileHandler> constructor = (Constructor<FileHandler>) clazz.getConstructor(ctx.getClass());
                return (T) constructor.newInstance(ctx);
            } catch (NoSuchMethodException e)
            {
                throw new InstantiationException(String.format("Handler class %s does not contain a proper handler constructor mathcing \'<init>(Context)\'", clazz.getName()));
            }
        } else
        {
            throw new IllegalArgumentException("No handler class specified");
        }
    }

    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        Context ctx = new Context();
        int exitCode = EXIT_CODES.OK;
        
        InputHandler inputHandler = null;
        FileHandler fileHandler = null;
        OutputHandler<?> outputHandler = null;
        
        try
        {
            int threads = DEFAULT_PROCESSING_THREADS;
            String inputUri = null;
            String outputUri = null;
            Class<? extends FileHandler> fileHandlerClass = DEFAULT_FILE_HANDLER;
            Class<? extends InputHandler> inputHandlerClass = DEFAULT_INPUT_HANDLER;
            Class<? extends OutputHandler<?>> outputHandlerClass = DEFAULT_OUTPUT_HANDLER;
            List<Pattern> filters = new ArrayList<>();
            
            CommandLine cmdLine = cliParser.parse(cliOptions, args, true);
            Option[] cmdOptions = cmdLine.getOptions();
            String temp;
            String error = "Invalid handler argument \"%s\" - %s";
            String stdError;
            String ifaceError;
            for (Option option : cmdOptions)
            {
                switch (option.getLongOpt())
                {
                    case "input":
                        inputUri = option.getValue();
                        if (inputUri == null || inputUri.length() == 0)
                        {
                            throw new ParseException(String.format("Invalid input argument \"%s\" - input cannot be empty", inputUri));
                        }
                        break;
                    case "output":
                        outputUri = option.getValue();
                        if (outputUri == null || outputUri.length() == 0)
                        {
                            throw new ParseException(String.format("Invalid output argument \"%s\" - output cannot be empty", outputUri));
                        }
                        break;
                    case "threads":
                        temp = option.getValue();
                        try
                        {
                            threads = Integer.parseInt(temp);
                        } catch (NumberFormatException e)
                        {
                            threads = -1;
                        }
                        if (threads < 1)
                        {
                            throw new ParseException(String.format("Invalid threads argument \"%s\" - threads must be a positive whole number greater than 0", temp));
                        }
                        break;
                    case "filter":
                        temp = option.getValue();
                        if (temp == null || temp.length() == 0)
                        {
                            throw new ParseException(String.format("Invalid filter argument \"%s\" - filter must be a valid Regular Expression", temp));
                        } else
                        {
                            try
                            {
                                filters.add(Pattern.compile(temp));
                            } catch (PatternSyntaxException e)
                            {
                                throw new PatternSyntaxException("Input filter pattern contains an error at index " + e.getIndex() + " - " + e.getMessage(), e.getPattern(), e.getIndex());
                            }
                        }
                        break;
                    case "file_handler":
                        temp = option.getValue();
                        ifaceError = "handler must be the name of a Java class that extends the FileHandler";
                        stdError = String.format(error, temp, ifaceError);
                        
                        if (temp == null || temp.length() == 0)
                        {
                            throw new ParseException(stdError);
                        } else
                        {
                            try
                            {
                                Class<?> clazz = Class.forName(temp);
                                
                                if (FileHandler.class.isAssignableFrom(clazz))
                                {
                                    fileHandlerClass = (Class<FileHandler>) clazz;
                                } else
                                {
                                    throw new ParseException(stdError);
                                }
                            } catch (ClassNotFoundException e)
                            {
                                throw new ParseException(String.format(error, e.getClass().getSimpleName(), e.getMessage()));
                            }
                        }
                        break;
                    case "input_handler":
                        temp = option.getValue();
                        ifaceError = "handler must be the name of a Java class that extends the InputHandler";
                        stdError = String.format(error, temp, ifaceError);
                        
                        if (temp == null || temp.length() == 0)
                        {
                            throw new ParseException(stdError);
                        } else
                        {
                            try
                            {
                                Class<?> clazz = Class.forName(temp);
                                
                                if (InputHandler.class.isAssignableFrom(clazz))
                                {
                                    inputHandlerClass = (Class<InputHandler>) clazz;
                                } else
                                {
                                    throw new ParseException(stdError);
                                }
                            } catch (ClassNotFoundException e)
                            {
                                throw new ParseException(String.format(error, e.getClass().getSimpleName(), e.getMessage()));
                            }
                        }
                        break;
                    case "output_handler":
                        temp = option.getValue();
                        ifaceError = "handler must be the name of a Java class that extends the OutputHandler";
                        stdError = String.format(error, temp, ifaceError);
                        
                        if (temp == null || temp.length() == 0)
                        {
                            throw new ParseException(stdError);
                        } else
                        {
                            try
                            {
                                Class<?> clazz = Class.forName(temp);
                                
                                if (InputHandler.class.isAssignableFrom(clazz))
                                {
                                    inputHandlerClass = (Class<InputHandler>) clazz;
                                } else
                                {
                                    throw new ParseException(stdError);
                                }
                            } catch (ClassNotFoundException e)
                            {
                                throw new ParseException(String.format(error, e.getClass().getSimpleName(), e.getMessage()));
                            }
                        }
                        break;
                }
            }
            
            ctx.put(Context.MAX_THREADS, threads);
            ctx.put(Context.INPUT_URI, inputUri);
            ctx.put(Context.DATASTORE_URI, outputUri);
            ctx.put(Context.INPUT_HANDLER_CLASS, fileHandlerClass);
            ctx.put(Context.FILE_HANDLER_CLASS, fileHandlerClass);
            ctx.put(Context.FILE_FILTERS, filters);
            ctx.put(Context.OUTPUT_HANDLER_CLASS, fileHandlerClass);
            
            if (outputHandlerClass != null)
            {
                try
                {
                    outputHandler = newHandlerInstance(outputHandlerClass, ctx);
                } catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException e)
                {
                    throw new ParseException(String.format("Invalid Handler \"%s\": %s", outputHandlerClass.getName(), e.getMessage()));
                }
            } else
            {
                throw new ParseException(String.format("Invalid Output Handler specified. The Output Handler must extend \"%s\" and implement a constructor that accepts a single Context argument", OutputHandler.class.getName()));
            }
            
            if (fileHandlerClass != null)
            {
                try
                {
                    fileHandler = newHandlerInstance(fileHandlerClass, ctx);
                } catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException e)
                {
                    throw new ParseException(String.format("Invalid Handler \"%s\": %s", fileHandlerClass.getName(), e.getMessage()));
                }
            } else
            {
                throw new ParseException(String.format("Invalid File Handler specified. The File Handler must extend \"%s\" and implement a constructor that accepts a single Context argument", FileHandler.class.getName()));
            }
            
            if (inputHandlerClass != null)
            {
                try
                {
                    inputHandler = newHandlerInstance(inputHandlerClass, ctx);
                } catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException e)
                {
                    throw new ParseException(String.format("Invalid Input Handler \"%s\": %s", inputHandlerClass.getName(), e.getMessage()));
                }
            } else
            {
                throw new ParseException(String.format("Invalid Input Handler specified. The Input Handler must extend \"%s\" and implement a constructor that accepts a single Context argument", InputHandler.class.getName()));
            }
        } catch (ParseException | PatternSyntaxException e)
        {
            logger.log(Level.SEVERE, "Error parsing command line: " + e.getMessage(), e);
            formatter.printHelp("Indexer", cliOptions);
            System.exit(EXIT_CODES.CONFIG_ERROR);
        } catch (InvocationTargetException ie)
        {
            Throwable e = ie.getTargetException();
            logger.log(Level.SEVERE, "Error occurred: " + e.getMessage(), e);
            System.exit(EXIT_CODES.INITIALIZATION_ERROR);
        }
        
        Indexer indexer = new Indexer(ctx, inputHandler, fileHandler, outputHandler);
        exitCode = indexer.call();
        
        System.exit(exitCode);
    }
}
