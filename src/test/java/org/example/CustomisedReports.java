package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.*;
import org.testng.xml.XmlSuite;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.lang.Thread.currentThread;
import static java.nio.file.Files.readAllBytes;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;

public class CustomisedReports implements IReporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomisedReports.class);

    private static final String CSS_FILE = "circle.css";

    private static final String ROW_TEMPLATE = "<tr class=\"%s\"><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td><a href=\"%s\">Log</a></td><td>%s</td></tr>";

    private static double passed;
    private static double failed;

    private void prepareCss() {
        Path source = new File(requireNonNull(requireNonNull(currentThread().getContextClassLoader().getResource(CSS_FILE))).getFile()).toPath();
        Path target = Path.of("target/%s".formatted(CSS_FILE));

        try {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("File copied successfully!");
        } catch (IOException e) {
            LOGGER.error("An error occurred while copying the file.", e);
        }
    }

    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
        prepareCss();

        String reportTemplate = initReportTemplate();

        final String body = suites
                .stream()
                .flatMap(suiteToResults())
                .collect(Collectors.joining());

        int total = (int) (passed + failed);
        int passRate = (int) ((passed / total) * 100);


        saveReportTemplate(
                reportTemplate
                        .replace("PERCENTAGE", valueOf(passRate))
                        .replace("%PASSED%", valueOf((int) passed))
                        .replace("%TOTAL%", valueOf(total))
                        .replaceFirst("</tbody>", format("%s</tbody>", body))
        );
    }

    private Function<ISuite, Stream<? extends String>> suiteToResults() {
        return suite -> suite.getResults().entrySet()
                .stream()
                .flatMap(resultsToRows(suite));
    }

    private Function<Map.Entry<String, ISuiteResult>, Stream<? extends String>> resultsToRows(ISuite suite) {
        return e -> {
            ITestContext testContext = e.getValue().getTestContext();

            Set<ITestResult> failedTests = testContext
                    .getFailedTests()
                    .getAllResults();
            Set<ITestResult> passedTests = testContext
                    .getPassedTests()
                    .getAllResults();
            Set<ITestResult> skippedTests = testContext
                    .getSkippedTests()
                    .getAllResults();

            String suiteName = suite.getName();

            passed += passedTests.size();
            failed += failedTests.size();

            return Stream
                    .of(failedTests, passedTests, skippedTests)
                    .flatMap(results -> generateReportRows(e.getKey(), suiteName, results).stream());
        };
    }

    private List<String> generateReportRows(String testName, String suiteName, Set<ITestResult> allTestResults) {
        return allTestResults
                .stream()
                .map(testResultToResultRow(testName, suiteName))
                .collect(toList());
    }

    private Function<ITestResult, String> testResultToResultRow(String testName, String suiteName) {
        String logs = "logs/";
        return testResult -> switch (testResult.getStatus()) {
            case ITestResult.FAILURE -> format(
                    ROW_TEMPLATE,
                    "danger",
                    suiteName,
                    testName,
                    testResult.getName(),
                    "FAILED",
                    "N/A",
                    logs + testResult.getName(),
                    testResult.getThrowable().getMessage()
            );
            case ITestResult.SUCCESS -> format(
                    ROW_TEMPLATE,
                    "success",
                    suiteName,
                    testName,
                    testResult.getName(),
                    "PASSED",
                    testResult.getEndMillis() - testResult.getStartMillis(),
                    logs + testResult.getName(),
                    EMPTY
            );
            case ITestResult.SKIP -> format(
                    ROW_TEMPLATE,
                    "warning",
                    suiteName,
                    testName,
                    testResult.getName(),
                    "SKIPPED",
                    "N/A",
                    logs + testResult.getName(),
                    testResult.getThrowable().getMessage()
            );
            default -> EMPTY;
        };
    }

    private String initReportTemplate() {
        String template = null;
        byte[] reportTemplate;
        try {
            reportTemplate = readAllBytes(new File(requireNonNull(currentThread().getContextClassLoader().getResource("report_template.html")).getFile()).toPath());
            template = new String(reportTemplate, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Problem initializing template", e);
        }
        return template;
    }

    private void saveReportTemplate(String reportTemplate) {
        String targetFolder = "target";
        new File(targetFolder).mkdirs();
        try (PrintWriter reportWriter = new PrintWriter(new BufferedWriter(new FileWriter(new File(targetFolder, "custom-report.html"))))) {
            reportWriter.println(reportTemplate);
            reportWriter.flush();
        } catch (IOException e) {
            LOGGER.error("Problem saving template", e);
        }
    }
}
