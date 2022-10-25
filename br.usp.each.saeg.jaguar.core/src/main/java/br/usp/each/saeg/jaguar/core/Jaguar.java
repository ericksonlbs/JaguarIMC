package br.usp.each.saeg.jaguar.core;

import br.usp.each.saeg.jaguar.codeforest.model.Requirement;
import br.usp.each.saeg.jaguar.core.heuristic.Heuristic;
import br.usp.each.saeg.jaguar.core.heuristic.HeuristicCalculator;
import br.usp.each.saeg.jaguar.core.model.core.CoverageStatus;
import br.usp.each.saeg.jaguar.core.model.core.requirement.AbstractTestRequirement;
import br.usp.each.saeg.jaguar.core.model.core.requirement.DuaTestRequirement;
import br.usp.each.saeg.jaguar.core.output.html.HtmlBuilder;
import br.usp.each.saeg.jaguar.core.output.html.HtmlWriter;
import br.usp.each.saeg.jaguar.core.output.xml.flat.FlatXmlWriter;
import br.usp.each.saeg.jaguar.core.output.xml.hierarchical.HierarchicalXmlWriter;
import br.usp.each.saeg.jaguar.core.utils.TestRequirementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jacoco.core.analysis.*;
import org.jacoco.core.analysis.dua.DuaCoverageBuilder;
import org.jacoco.core.analysis.dua.IDua;
import org.jacoco.core.analysis.dua.IDuaClassCoverage;
import org.jacoco.core.analysis.dua.IDuaMethodCoverage;
import org.jacoco.core.data.AbstractExecutionDataStore;
import org.jacoco.core.data.ControlFlowExecutionData;
import org.jacoco.core.data.ControlFlowExecutionDataStore;
import org.jacoco.core.data.DataFlowExecutionDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This class store the coverage information received from Jacoco and generate a
 * suspicious test requirement rank based on one SFL Heuristic.
 * 
 * @author Henrique Ribeiro
 */
public class Jaguar {

	private static final Logger logger = LoggerFactory.getLogger("JaguarLogger");

	private static final String XML_NAME = "jaguar_output";
	private static int nTests = 0;
	private static int nTestsFailed = 0;
	
	private Map<String, File> classFilesCache;

	private JaguarSFL sfl = new JaguarSFL();
	
	private Long startTime;
	private Long totalTimeSpent;
	
	public static final String REPORTS_FOLDER_NAME = ".jaguar";

	/**
	 * Construct the Jaguar object.
	 * 
	 * @param heuristic
	 *            the heuristic to be used on the fault localization rank.
	 * @param targetDir
	 *            the target dir created by eclipse
	 */
	public Jaguar(File classesDir) {
		this.startTime = System.currentTimeMillis();

		classFilesCache = new HashMap<String, File>();
		populateClassFilesCache(classesDir, "");
		logger.debug("ClassFilesCache size = {}", classFilesCache.size());
	}

	private void populateClassFilesCache(File dir, String path) {
		File[] files = dir.listFiles();
		if (files == null) {
			return;
		}
		for (File file : files) {
			if (file.isDirectory()) {
				populateClassFilesCache(file, path + file.getName() + "/");
			} else if (file.getName().endsWith(".class")) {
				String className = path + StringUtils.removeEnd(file.getName(), ".class");
				classFilesCache.put(className, file);
				logger.debug("Added {} to classFilesCache", className);
			}
		}
	}

	/**
	 * Receive the coverage information and store it on Test Requiremtns.
	 * 
	 * @param executionData
	 *            the covarege data from Jacoco
	 * @param currentTestFailed
	 *            result of the test
	 * @throws IOException
	 * 
	 */
	public void collect(final AbstractExecutionDataStore executionData, boolean currentTestFailed) throws IOException {
		logger.debug("Test # {}", nTests);
		if (executionData instanceof DataFlowExecutionDataStore) {

			long startTime = System.currentTimeMillis();
			DuaCoverageBuilder duaCoverageBuilder = new DuaCoverageBuilder();
			AbstractAnalyzer analyzer = new DataflowAnalyzer(executionData, duaCoverageBuilder);
			analyzeCoveredClasses(executionData, analyzer);
			logger.debug("Time to analyze DF data: {}", System.currentTimeMillis() - startTime);

			startTime = System.currentTimeMillis();
			collectDuaCoverage(currentTestFailed, duaCoverageBuilder);
			logger.debug("Time to read and store data: {} , from {} classes", System.currentTimeMillis() - startTime, duaCoverageBuilder
					.getClasses().size());

		} else if (executionData instanceof ControlFlowExecutionDataStore) {

			long startTime = System.currentTimeMillis();
			CoverageBuilder coverageBuilder = new CoverageBuilder();
			AbstractAnalyzer analyzer = new ControlFlowAnalyzer(executionData, coverageBuilder);
			analyzeCoveredClasses(executionData, analyzer);
			logger.debug("Time to analyze CF data: {}", System.currentTimeMillis() - startTime);

			startTime = System.currentTimeMillis();
			collectLineCoverage(currentTestFailed, coverageBuilder);
			logger.debug("Time to read and store data: {} , from {} classes", System.currentTimeMillis() - startTime, coverageBuilder
					.getClasses().size());

		} else {
			logger.error("Unknown DataStore - {}", executionData.getClass().getName());
		}

	}

	private void analyzeCoveredClasses(AbstractExecutionDataStore executionData, AbstractAnalyzer analyzer) {
		Collection<File> classFiles = classFilesOfStore(executionData);
		for (File classFile : classFiles) {
			try (InputStream inputStream = new FileInputStream(classFile)) {
				analyzer.analyzeClass(inputStream, classFile.getPath());
			} catch (IOException e) {
				logger.warn("Exception during analysis of file " + classFile.getAbsolutePath(), e);
			}
		}
	}

	private Collection<File> classFilesOfStore(AbstractExecutionDataStore executionDataStore) {
		Collection<File> result = new ArrayList<File>();
		for (ControlFlowExecutionData data : executionDataStore.getContents()) {
			String vmClassName = data.getName();
			File classFile = classFilesCache.get(vmClassName);
			if (classFile != null) {
				result.add(classFile);
			}
		}
		return result;
	}

	private void collectDuaCoverage(boolean currentTestFailed, DuaCoverageBuilder coverageVisitor) {
		int totalDuas = 0;
		int totalDuasCovered = 0; 
		for (IDuaClassCoverage clazz : coverageVisitor.getClasses()) {
			logger.trace("Collecting duas from class  {}", clazz.getName());
			for (IDuaMethodCoverage method : clazz.getMethods()) {
				logger.trace("Collecting duas from method  {}", method.getSignature());
				for (IDua dua : method.getDuas()) {
					totalDuas++;
					logger.trace("Collecting information from dua {}", dua);
					CoverageStatus coverageStatus = CoverageStatus.as(dua.getStatus());
					if (CoverageStatus.FULLY_COVERED == coverageStatus) {
						totalDuasCovered++;
						sfl.updateRequirement(clazz, method, dua, currentTestFailed);
					}
				}
			}
		}
		logger.debug("#duas = {}, #coveredDuas = {}", totalDuas, totalDuasCovered);
	}

	private void collectLineCoverage(boolean currentTestFailed, CoverageBuilder coverageVisitor) {
		int totalLines = 0;
		int totalLinesCovered = 0;
		for (IClassCoverage clazz : coverageVisitor.getClasses()) {
			logger.trace("Collecting lines from class " + clazz.getName());
			CoverageStatus coverageStatus = CoverageStatus.as(clazz.getClassCounter().getStatus());
			if (CoverageStatus.FULLY_COVERED == coverageStatus || CoverageStatus.PARTLY_COVERED == coverageStatus) {
				int firstLine = clazz.getFirstLine();
				int lastLine = clazz.getLastLine();
				if (firstLine >= 0) {
					for (int currentLine = firstLine; currentLine <= lastLine; currentLine++) {
						totalLines++;
						ILine line = clazz.getLine(currentLine);
						logger.trace("Collecting information from line {}", currentLine);
						coverageStatus = CoverageStatus.as(line.getStatus());
						if (CoverageStatus.FULLY_COVERED == coverageStatus || CoverageStatus.PARTLY_COVERED == coverageStatus) {
							totalLinesCovered++;
							sfl.updateRequirement(clazz, currentLine, currentTestFailed);
						}
					}
				}
			}
		}
		logger.debug("#lines = {}, #coveredlines = {}", totalLines, totalLinesCovered);

	}

	/**
	 * Calculate the rank based on the heuristic and testRequirements. Return
	 * the rank in descending order.
	 * 
	 * @return the rank in descending order.
	 * 
	 */
	public ArrayList<AbstractTestRequirement> generateRank(Heuristic heuristic) {
		logger.debug("Rank calculation started...");
		HeuristicCalculator calc = new HeuristicCalculator(heuristic, sfl.getTestRequirements().values(), nTests - nTestsFailed, nTestsFailed);
		ArrayList<AbstractTestRequirement> result = calc.calculateRank();
		logger.debug("Rank calculation finished.");
		return result;
	}

	/**
	 * Use the given testRequirements to generate the Flat output XML. Using the
	 * default name.
	 * 
	 * @param testRequirements
	 *            the testRequirements
	 * @param projectDir
	 *            the directory in which the output folder and files will be
	 *            written
	 * 
	 */
	public void generateFlatXML(Heuristic heuristic, File projectDir) {
		generateFlatXML(heuristic, projectDir, XML_NAME);
	}

	/**
	 * Use the given testRequirements to generate the Flat output XML, using the
	 * paramenter fileName.
	 * 
	 * @param testRequirements
	 *            the testRequirements
	 * @param projectDir
	 *            the directory in which the output folder and files will be
	 *            written
	 * @param fileName
	 *            the name of the output xml file
	 * 
	 */
	public void generateFlatXML(Heuristic heuristic, File projectDir, String fileName) {
		ArrayList<AbstractTestRequirement> testRequirements = generateRank(heuristic);
		FlatXmlWriter xmlWriter = new FlatXmlWriter(testRequirements, heuristic, totalTimeSpent);
		xmlWriter.generateXML(projectDir, fileName);
	}

	/**
	 * Use the given testRequirements to generate the Hierarchical output XML.
	 * Using the default name.
	 * 
	 * @param testRequirements
	 *            the testRequirements
	 * @param projectDir
	 *            the directory in which the output folder and files will be
	 *            written
	 * 
	 */
	public void generateHierarchicalXML(Heuristic heuristic, File projectDir) {
		generateHierarchicalXML(heuristic, projectDir, XML_NAME);
	}

	/**
	 * Use the given testRequirements to generate the Hierarchical output XML,
	 * using the paramenter fileName.
	 * 
	 * @param testRequirements
	 *            the testRequirements
	 * @param projectDir
	 *            the directory in which the output folder and files will be
	 *            written
	 * @param fileName
	 *            the name of the output xml file
	 * 
	 */
	public void generateHierarchicalXML(Heuristic heuristic, File projectDir, String fileName) {
		ArrayList<AbstractTestRequirement> testRequirements = generateRank(heuristic);
		HierarchicalXmlWriter xmlWriter = new HierarchicalXmlWriter(testRequirements, heuristic, totalTimeSpent);
		xmlWriter.generateXML(projectDir, fileName);
	}
	
	public void generateHtml(Heuristic heuristic, File projectDirectory, String outputFile) throws IOException {
		ArrayList<AbstractTestRequirement> testRequirements = generateRank(heuristic);
		
		if(testRequirements.isEmpty()){
			return;
		}
		
		Requirement.Type testRequirementType = TestRequirementUtils.getType(testRequirements);
		
		HtmlWriter htmlWriter = new HtmlWriter(
				new HtmlBuilder(),
				testRequirements,
				testRequirementType,
				heuristic,
				projectDirectory,
				outputFile
		);
		
		if(Requirement.Type.LINE.equals(testRequirementType)){
			htmlWriter.generateHtmlForLineType();
		}else {
			htmlWriter.generateHtmlForDuaType();
		}
		
	}

	/**
	 * Currently only used to save the total time spent since Jaguar was
	 * created.
	 * 
	 */
	public void finish() {
		totalTimeSpent = System.currentTimeMillis() - startTime;
	}

	public static int getnTests() {
		return nTests;
	}

	public static int getnTestsFailed() {
		return nTestsFailed;
	}

	public static int increaseNTests() {
		return ++nTests;
	}

	public static int increaseNTestsFailed() {
		return ++nTestsFailed;
	}

}
