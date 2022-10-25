package br.usp.each.saeg.jaguar.core.cli;

import java.io.IOException;
import java.net.URISyntaxException;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

/**
 * 
 * @author Mario Concilio
 *
 */
public class JaguarRunner4Cli {
	
	public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {
		System.out.println("Welcome to Jaguar CLI");
		
		final JaguarRunnerCliOptions options = new JaguarRunnerCliOptions();
		final CmdLineParser parser = new CmdLineParser(options);
		
        try {
            parser.parseArgument(args);
        } 
        catch (final CmdLineException e) {
            System.err.println(e.getLocalizedMessage());
            parser.printUsage(System.err);
            System.exit(1);
        }

        if (options.isHelp()){
			parser.printUsage(System.err);
			System.exit(0);	
        }
        
        final String classpath = String.join(System.getProperty("path.separator"), 
        		".",
        		System.getProperty("java.class.path"),
        		options.getClasspath());
        
        final String agentPath = "jacocoagent.jar";
        final String agentArguments = new StringBuilder("-javaagent:")
        		.append(agentPath)
        		.append("=output=tcpserver,includes=")
        		.append(options.getIncludes())
        		.append(",dataflow=")
        		.append(options.isDataFlow()? "true" : "false")
        		.toString();
		
        ProcessBuilder builder;
        if (options.isDataFlow()) {
        	builder = new ProcessBuilder("java", 
    				agentArguments, 
    				"-Xmx1024m",
    				"-cp",
    				 classpath,
    				 "br.usp.each.saeg.jaguar.core.cli.JaguarRunner",
    				 "--dataflow",
    				 "--outputType", options.getOutputType(), 
    				 "--logLevel", options.getLogLevel(), 
    				 "--projectDir", options.getProjectPath().getCanonicalPath(), 
    				 "--classesDir", options.getSourcePath().getCanonicalPath(),
    				 "--testsDir", options.getTestPath().getCanonicalPath(),
    				 "--output", options.getOutputFileName(),
    				 "--heuristic", options.getHeuristic());
        }
        else {
        	builder = new ProcessBuilder("java", 
    				agentArguments, 
    				"-Xmx1024m",
    				"-cp",
    				 classpath,
    				 "br.usp.each.saeg.jaguar.core.cli.JaguarRunner",
    				 "--outputType", options.getOutputType(), 
    				 "--logLevel", options.getLogLevel(), 
    				 "--projectDir", options.getProjectPath().getCanonicalPath(), 
    				 "--classesDir", options.getSourcePath().getCanonicalPath(),
    				 "--testsDir", options.getTestPath().getCanonicalPath(),
    				 "--output", options.getOutputFileName(),
    				 "--heuristic", options.getHeuristic());
        }
		
		builder.redirectErrorStream(true);
		builder.inheritIO();
		
		final Process process = builder.start();
		process.waitFor();
	}

}
