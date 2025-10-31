package tests;

import org.junit.jupiter.api.*;
import projects.TxtFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TxtFileTest {

    private TxtFile txtFile;

    @BeforeEach
    void setUp() {
        txtFile = new TxtFile();
        System.setProperty("txt.delimiter", ",");
    }

    @Test
    void shouldGenerateFileWith10000Lines() throws IOException {

        Path filePath = txtFile.generateFile();

        assertTrue(Files.exists(filePath), "File couldn't created");

        long lineCount = Files.lines(filePath).count();

        assertEquals(10000, lineCount, "The number of rows is not 10,000");

        Files.deleteIfExists(filePath);
    }

    @Test
    void shouldGenerateFileInTxt() throws IOException {
        Path filePath = txtFile.generateFile();
        String fileName = filePath.getFileName().toString();

        String lastPart = fileName.substring(fileName.lastIndexOf('.') + 1);
        assertEquals("txt", lastPart);
        Files.deleteIfExists(filePath);
    }
}