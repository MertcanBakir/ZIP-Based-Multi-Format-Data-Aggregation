package tests;

import org.junit.jupiter.api.*;
import projects.TxtFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TxtFileTest {

    private TxtFile txtFile;

    @BeforeEach
    void setUp() {
        // Initialize TxtFile and set delimiter from config
        txtFile = new TxtFile();
        System.setProperty("txt.delimiter", ";");
    }

    @Test
    void shouldGenerateFileWith10000Lines() throws IOException {
        // Generate sample TXT file
        Path filePath = txtFile.generateFile();

        // Verify file creation
        assertTrue(Files.exists(filePath), "File not created");

        // Count total lines
        long lineCount = Files.lines(filePath).count();

        // Must have exactly 10,000 rows
        assertEquals(10000, lineCount, "Row count mismatch");

        // Clean up generated file
        Files.deleteIfExists(filePath);
    }

    @Test
    void shouldGenerateFileInTxt() throws IOException {
        // Generate TXT file and check extension
        Path filePath = txtFile.generateFile();
        String fileName = filePath.getFileName().toString();

        // Extract extension and validate
        String lastPart = fileName.substring(fileName.lastIndexOf('.') + 1);
        assertEquals("txt", lastPart);

        Files.deleteIfExists(filePath);
    }

    @Test
    void shouldCountMalformed_inSingleLine() throws Exception {
        // Single line with malformed value (discount=".")
        String line = "1;TR;10.5;0.10;.;100.0;5;10;4.5;80";

        // Simulate reading from input stream
        try (InputStream in = new java.io.ByteArrayInputStream(
                line.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            txtFile.readFile(in, "single.txt");
        }

        // id column processed normally
        var c0 = txtFile.getColStat(0);
        assertEquals(1, c0.count(), "col0 count");
        assertEquals(1.0, c0.sum(), 1e-9);

        // revenue column processed normally
        var c5 = txtFile.getColStat(5);
        assertEquals(1, c5.count(), "col5 count");
        assertEquals(100.0, c5.sum(), 1e-9);

        // discount='.' is not numeric → excluded from stats
        var c4 = txtFile.getColStat(4);
        assertEquals(0, c4.count(), "col4 should not be counted");

        // one malformed field detected
        assertEquals(1L, txtFile.getMalformedCount("TXT"), "malformed TXT count");
    }
}