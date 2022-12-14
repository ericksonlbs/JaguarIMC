package br.usp.each.saeg.jaguar.core.cli;

import java.io.File;
import java.util.Arrays;

import org.junit.runner.JUnitCore;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.usp.each.saeg.jaguar.core.JaCoCoClient;
import br.usp.each.saeg.jaguar.core.Jaguar;
import br.usp.each.saeg.jaguar.core.heuristic.Heuristic;
import br.usp.each.saeg.jaguar.core.runner.JaguarRunListener;
import br.usp.each.saeg.jaguar.core.utils.FileUtils;

/**
 * @author Henrique Ribeiro
 * 
 */
public class JaguarRunner {

	private final JUnitCore junit = new JUnitCore();
	private static Logger logger = LoggerFactory.getLogger("JaguarLogger");
	
	private final Heuristic heuristic;
	private final File projectDir;
	private final File sourceDir;
	private final File testDir;
	private final Boolean isDataFlow;
	private final String outputFile;
	private final String outputExtension;
	private final String outputType;
	
	public JaguarRunner(Heuristic heuristic, File projectDir, File sourceDir,
			File testDir, Boolean isDataFlow, String outputFile, String outputExtension, String outputType) {
		super();
		this.heuristic = heuristic;
		this.projectDir = projectDir;
		this.sourceDir = sourceDir;
		this.testDir = testDir;
		this.isDataFlow = isDataFlow;
		this.outputFile = outputFile;
		this.outputExtension = outputExtension;
		this.outputType = outputType;
	}

	private void run() throws Exception {
		final Class<?>[] classes = FileUtils.findTestClasses(testDir);

		final Jaguar jaguar = new Jaguar(sourceDir);
		final JaCoCoClient client = new JaCoCoClient(isDataFlow);
		client.connect();

		junit.addListener(new JaguarRunListener(jaguar, client));
		junit.run(classes);

		client.close();
		
		generateOutputExtension(jaguar);
	}
	
	private void generateOutputExtension(Jaguar jaguar) throws Exception{
		if(outputExtension.equals("HTML")){
			jaguar.generateHtml(heuristic, projectDir, outputFile);
		}
		/*XML generation options*/
		else if (outputType.equals("H")) {
			jaguar.generateHierarchicalXML(heuristic, projectDir, outputFile);
		} else {
			jaguar.generateFlatXML(heuristic, projectDir, outputFile);
		}
	}
	
	public static void main(String[] args){
		final JaguarRunnerOptions options = new JaguarRunnerOptions();
		final CmdLineParser parser = new CmdLineParser(options);
		
        try {
        	logger.info("Command:" + Arrays.toString(args));
            	parser.parseArgument(args);
        } catch (final CmdLineException e) {
		System.err.println(e.getLocalizedMessage());
		parser.printUsage(System.err);
		System.exit(1);
        }

        if (options.isHelp()){
		parser.printUsage(System.err);
		System.exit(0);	
        }
        
	try {
		logger.info(options.toString());
		new JaguarRunner(options.getHeuristic(), options.getProjectPath(),
				options.getSourcePath(), options.getTestPath(),
				options.getDataFlow(), options.getOutputFileName(),
				options.getOutputExtension(), options.getOutputType()
				).run();
	} catch (Exception e) {
		logger.error("Exception :" + e.toString());
		logger.error("Exception Message : " + e.getMessage());
		logger.error("Stacktrace :");
		e.printStackTrace(System.err);
		System.exit(1);
	}
		
		logger.info("Jaguar has finished!");
		System.exit(0);
	}

}
