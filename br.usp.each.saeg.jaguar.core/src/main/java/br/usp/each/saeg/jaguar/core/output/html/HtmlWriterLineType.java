package br.usp.each.saeg.jaguar.core.output.html;

import br.usp.each.saeg.jaguar.codeforest.model.Requirement;
import br.usp.each.saeg.jaguar.core.Jaguar;
import br.usp.each.saeg.jaguar.core.heuristic.Heuristic;
import br.usp.each.saeg.jaguar.core.model.core.requirement.AbstractTestRequirement;
import br.usp.each.saeg.jaguar.core.utils.CodeUtils;
import br.usp.each.saeg.jaguar.core.utils.FileUtils;
import br.usp.each.saeg.jaguar.core.utils.OperationalSystemUtils;
import br.usp.each.saeg.jaguar.core.utils.TestRequirementUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class HtmlWriterLineType {
	
	private static final String MAIN_CLASS_TABLE_HTML_TEMPLATE_PATH = "br.usp.each.saeg.jaguar.core/src/main/resources/html-output/html/main-class-table-template.html";
	
	private static final String TEST_REQUIREMENT_TABLE_HTML_TEMPLATE_PATH = "br.usp.each.saeg.jaguar.core/src/main/resources/html-output/html/test-requirement-table-template.html";
	
	private static final String TEST_REQUIREMENT_CODE_HTML_TEMPLATE_PATH = "br.usp.each.saeg.jaguar.core/src/main/resources/html-output/html/test-requirement-code-template.html";
	
	public static final String REPORT_DATE = "$reportDate$";
	
	private final HtmlBuilder htmlBuilder;
	
	private final File projectDirectory;
	
	private final List<AbstractTestRequirement> testRequirementsList;
	
	private final Requirement.Type requirementType;
	
	private final Heuristic heuristic;
	
	private final String outputFile;
	
	private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy - HH:mm");
	
	public HtmlWriterLineType(
			HtmlBuilder htmlBuilder,
			File projectDirectory,
			List<AbstractTestRequirement> testRequirementsList,
			Requirement.Type requirementType,
			Heuristic heuristic,
			String outputFile) {
		this.htmlBuilder = htmlBuilder;
		this.projectDirectory = projectDirectory;
		this.testRequirementsList = testRequirementsList;
		this.requirementType = requirementType;
		this.heuristic = heuristic;
		this.outputFile = outputFile;
	}
	
	public File write() throws IOException {
		
		File reportsFolder = FileUtils.createOrGetFolder(projectDirectory.getAbsolutePath(), Jaguar.REPORTS_FOLDER_NAME);
		File subHtmlFolder = FileUtils.createOrGetHiddenFolder(reportsFolder.getAbsolutePath(), HtmlWriter.HTML_FILES_FOLDER_NAME);
		File lineFolder = FileUtils.createOrGetFolder(subHtmlFolder.getAbsolutePath(),"line");
		
		HtmlWriter.writeCssFiles(subHtmlFolder, HtmlWriter.CSS_FILES_FOLDER_NAME);
		HtmlWriter.writeImgFiles(subHtmlFolder, HtmlWriter.IMG_FILES_FOLDER_NAME);
		HtmlWriter.writeJsFiles(subHtmlFolder, HtmlWriter.JS_FILES_FOLDER_NAME);
		
		MultiValuedMap<File, AbstractTestRequirement> requirementsGroupByClassFile = TestRequirementUtils
				.testRequirementsGroupByClassFile(testRequirementsList, projectDirectory.getAbsolutePath());
		
		File mainHtml = new File(reportsFolder.getAbsolutePath() + OperationalSystemUtils.systemFileSeparator() +
				outputFile + HtmlWriter.HTML_TYPE_FOR_FILE
		);
		
		Map<File, File> htmlFileMapByClassFile = new HashMap<>();
		
		for (File classFile : requirementsGroupByClassFile.keySet()) {
			htmlFileMapByClassFile.put(
					classFile,
					createHtmlforClass(lineFolder, classFile, requirementsGroupByClassFile.get(classFile), mainHtml.getAbsolutePath())
			);
		}
		
		org.apache.commons.io.FileUtils.writeStringToFile(
				mainHtml,
				buildContentForMainHtml(htmlFileMapByClassFile, requirementsGroupByClassFile)
		);
		
		return mainHtml;
	}
	
	public String buildContentForMainHtml(Map<File, File> htmlFileMapByClassFile, MultiValuedMap<File, AbstractTestRequirement> requirementsGroupByClassFile) throws IOException {
		String htmlTemplateForTestRequirement = htmlBuilder.getStringFromHtmlTemplate(MAIN_CLASS_TABLE_HTML_TEMPLATE_PATH);
		htmlTemplateForTestRequirement = htmlTemplateForTestRequirement.replace("$heuristic$", StringUtils.upperCase(
				StringUtils.removeEndIgnoreCase(
						heuristic.getClass().getSimpleName(), "heuristic"
				)
		));
		htmlTemplateForTestRequirement = htmlTemplateForTestRequirement.replace("$requirementType$", requirementType.toString());
		htmlTemplateForTestRequirement = htmlTemplateForTestRequirement.replace("$numberOfTests$", String.valueOf(Jaguar.getnTests()));
		htmlTemplateForTestRequirement = htmlTemplateForTestRequirement.replace("$numberOfFailTests$", String.valueOf(Jaguar.getnTestsFailed()));
		htmlTemplateForTestRequirement = htmlTemplateForTestRequirement.replace("$allTableDataForClassFiles$", htmlBuilder.buildTDTagForClassList(htmlFileMapByClassFile, requirementsGroupByClassFile));
		htmlTemplateForTestRequirement = htmlTemplateForTestRequirement.replace(REPORT_DATE,  simpleDateFormat.format(new Date()));
		
		return htmlTemplateForTestRequirement;
	}
	
	private File createHtmlforClass(File subHtmlFolder, File classFile, Collection<AbstractTestRequirement> abstractTestRequirementsForThisClass, String mainHtmlAbsolutePath) throws IOException {
		
		String classNameWithPath = abstractTestRequirementsForThisClass.iterator().next().getClassName();
		String classNameToHtmlFile = classNameWithPath.replace(OperationalSystemUtils.systemFileSeparator(), "-");
		
		File htmlWithTableForTestRequirements = new File(
				subHtmlFolder.getAbsolutePath()
						+ OperationalSystemUtils.systemFileSeparator()
						+ classNameToHtmlFile
						+ HtmlWriter.HTML_TYPE_FOR_FILE
		);
		
		File htmlWithCodePaintedForAllRequirements = new File(
				subHtmlFolder.getAbsolutePath()
						+ OperationalSystemUtils.systemFileSeparator()
						+ classNameToHtmlFile
						+ "-code"
						+ HtmlWriter.HTML_TYPE_FOR_FILE
		);
		
		String javaCodeInString = CodeUtils.getCodeFromAbsolutePath(classFile);
		String[] nameBreakBySeparator = abstractTestRequirementsForThisClass.iterator().next().getClassName().split(
				OperationalSystemUtils.systemFileSeparator()
		);
		String classNameOnly = nameBreakBySeparator[nameBreakBySeparator.length - 1];
		
		org.apache.commons.io.FileUtils.writeStringToFile(
				htmlWithCodePaintedForAllRequirements,
				buildContentForHtmlCodeFile(
						javaCodeInString,
						htmlWithTableForTestRequirements.getAbsolutePath(),
						abstractTestRequirementsForThisClass,
						mainHtmlAbsolutePath
				)
		);
		
		org.apache.commons.io.FileUtils.writeStringToFile(
				htmlWithTableForTestRequirements,
				buildContentForHtmlTestRequirementsTableFile(
						classNameOnly,
						classNameWithPath,
						htmlWithCodePaintedForAllRequirements.getAbsolutePath(),
						abstractTestRequirementsForThisClass,
						mainHtmlAbsolutePath
				)
		);
		
		return htmlWithTableForTestRequirements;
	}
	
	public String buildContentForHtmlCodeFile(String javaCode, String pathToHtmlWithTableForRequirements, Collection<AbstractTestRequirement> abstractTestRequirementsForThisClass, String mainHtmlAbsolutePath) throws IOException {
		String htmlTemplateForTestRequirement = htmlBuilder.getStringFromHtmlTemplate(TEST_REQUIREMENT_CODE_HTML_TEMPLATE_PATH);
		
		htmlTemplateForTestRequirement = htmlTemplateForTestRequirement.replace("$mainHtmlPath$", mainHtmlAbsolutePath);
		htmlTemplateForTestRequirement = htmlTemplateForTestRequirement.replace("$classNameWithPath$", testRequirementsList.iterator().next().getClassName());
		htmlTemplateForTestRequirement = htmlTemplateForTestRequirement.replace("$htmlTable$", pathToHtmlWithTableForRequirements);
		htmlTemplateForTestRequirement = htmlTemplateForTestRequirement.replace("$javaCode$", htmlBuilder.transformJavaCodeToHtml(javaCode, abstractTestRequirementsForThisClass));
		htmlTemplateForTestRequirement = htmlTemplateForTestRequirement.replace(REPORT_DATE,  simpleDateFormat.format(new Date()));
		
		return htmlTemplateForTestRequirement;
	}
	
	public String buildContentForHtmlTestRequirementsTableFile(String className, String classNameWithPath, String pathToHtmlWithCode, Collection<AbstractTestRequirement> abstractTestRequirementsForThisClass, String mainHtmlAbsolutePath) throws IOException {
		String htmlTemplateForTestRequirement = htmlBuilder.getStringFromHtmlTemplate(TEST_REQUIREMENT_TABLE_HTML_TEMPLATE_PATH);
		
		htmlTemplateForTestRequirement = htmlTemplateForTestRequirement.replace("$mainHtmlPath$", mainHtmlAbsolutePath);
		htmlTemplateForTestRequirement = htmlTemplateForTestRequirement.replace("$classNameWithPath$", classNameWithPath);
		htmlTemplateForTestRequirement = htmlTemplateForTestRequirement.replace("$heuristic$", StringUtils.upperCase(
				StringUtils.removeEndIgnoreCase(
						heuristic.getClass().getSimpleName(), "heuristic"
				)
		));
		htmlTemplateForTestRequirement = htmlTemplateForTestRequirement.replace("$requirementType$", requirementType.toString());
		htmlTemplateForTestRequirement = htmlTemplateForTestRequirement.replace("$numberOfTests$", String.valueOf(Jaguar.getnTests()));
		htmlTemplateForTestRequirement = htmlTemplateForTestRequirement.replace("$numberOfFailTests$", String.valueOf(Jaguar.getnTestsFailed()));
		htmlTemplateForTestRequirement = htmlTemplateForTestRequirement.replace("$subTitle$", className);
		htmlTemplateForTestRequirement = htmlTemplateForTestRequirement.replace("$allTableDataForTestRequirements$", htmlBuilder.buildTDTagForTestRequirementList(abstractTestRequirementsForThisClass, pathToHtmlWithCode));
		htmlTemplateForTestRequirement = htmlTemplateForTestRequirement.replace(REPORT_DATE,  simpleDateFormat.format(new Date()));
		
		return htmlTemplateForTestRequirement;
	}
	
}
