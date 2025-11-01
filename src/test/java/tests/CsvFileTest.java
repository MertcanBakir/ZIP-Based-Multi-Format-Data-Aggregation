package tests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import projects.CsvFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CsvFileTest {
    private CsvFile csvFile;

    @BeforeEach
    void setUp() {
        // Initialize CsvFile before each test
        csvFile = new CsvFile();
        System.setProperty("csv.delimiter", ",");
    }

    @Test
    void shouldGenerateFileWith10000Lines() throws IOException {
        // Generate a CSV file
        Path filePath = csvFile.generateFile();

        // Verify file exists
        assertTrue(Files.exists(filePath), "File not created");

        // Verify it has 10,000 lines
        long lineCount = Files.lines(filePath).count();
        assertEquals(10000, lineCount, "Row count mismatch");

        // Clean up
        Files.deleteIfExists(filePath);
    }

    @Test
    void shouldGenerateFileInTxt() throws IOException {
        // Generate file and get extension
        Path filePath = csvFile.generateFile();
        String fileName = filePath.getFileName().toString();

        // Verify extension is .csv
        String lastPart = fileName.substring(fileName.lastIndexOf('.') + 1);
        assertEquals("csv", lastPart);

        // Clean up
        Files.deleteIfExists(filePath);
    }

    @Test
    void shouldCountMalformed_inSingleLine() throws Exception {
        // Create a single CSV line with one malformed value (discount = ".")
        String csvfile = "id,region,amount,tax,discount,revenue,transactions,customers,satisfaction,score\n"
                + "1,TR,10.5,0.10,.,100.0,5,10,4.5,80";

        // Feed it to the reader
        try (InputStream in = new java.io.ByteArrayInputStream(
                csvfile.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            csvFile.readFile(in, "single.csv");
        }

        // Verify numeric column stats
        var c0 = csvFile.getColStat(0);
        assertEquals(1, c0.count(), "col0 count");
        assertEquals(1.0, c0.sum(), 1e-9);

        var c5 = csvFile.getColStat(5);
        assertEquals(1, c5.count(), "col5 count");
        assertEquals(100.0, c5.sum(), 1e-9);

        // Malformed cell should not be counted
        var c4 = csvFile.getColStat(4);
        assertEquals(0, c4.count(), "col4 should not be counted");

        // Verify malformed count = 1
        assertEquals(1L, csvFile.getMalformedCount("CSV"), "malformed CSV count");
    }
}