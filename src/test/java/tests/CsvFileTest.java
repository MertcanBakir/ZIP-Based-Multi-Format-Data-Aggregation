package tests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import projects.CsvFile;
import projects.TxtFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CsvFileTest {
    private CsvFile csvFile;

    @BeforeEach
    void setUp() {
        csvFile = new CsvFile();
        System.setProperty("csv.delimiter", ",");
    }

    @Test
    void shouldGenerateFileWith10000Lines() throws IOException {

        Path filePath = csvFile.generateFile();

        assertTrue(Files.exists(filePath), "File couldn't created");

        long lineCount = Files.lines(filePath).count();

        assertEquals(10000, lineCount, "The number of rows is not 10,000");

        Files.deleteIfExists(filePath);
    }

    @Test
    void shouldGenerateFileInTxt() throws IOException {
        Path filePath = csvFile.generateFile();
        String fileName = filePath.getFileName().toString();

        String lastPart = fileName.substring(fileName.lastIndexOf('.') + 1);
        assertEquals("csv", lastPart);
        Files.deleteIfExists(filePath);
    }


}
