package org.example;

import org.testng.Assert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

abstract public class BaseTest {

    private final ThreadLocal<FileWriter> threadLogFile = new ThreadLocal<>();

    private static final String LOG_PATTERN = "target\\logs\\%s";

    protected void commonLogic(Method method) throws IOException, NoSuchMethodException {
        setLogFile(LOG_PATTERN.formatted(getTestName(method)));
        List<Boolean> givenList = Arrays.asList(true, false);
        boolean randomElement = givenList.get(new Random().nextInt(givenList.size()));

        log(randomAlphabetic(100));

        Assert.assertTrue(randomElement, "Test has failed [%s]:".formatted(randomAlphanumeric(10)));
    }

    private String getTestName(Method method) throws NoSuchMethodException {
        return this.getClass().getDeclaredMethod(method.getName(), Method.class).getName();
    }

    private void setLogFile(String filename) throws IOException {
        File file = new File(filename);
        file.getParentFile().mkdirs();
        threadLogFile.set(new FileWriter(file));
    }

    private void log(String message) throws IOException {
        FileWriter fileWriter = threadLogFile.get();
        fileWriter.write(message + "\n");
        fileWriter.flush();
    }
}
