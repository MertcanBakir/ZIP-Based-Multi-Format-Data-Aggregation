package tests;

import com.google.gson.stream.JsonReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import projects.JsonFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonFileTest {
    private JsonFile jsonfile;

    @BeforeEach
    void setUp() {
        // Init generator and set array output
        jsonfile = new JsonFile();
        System.setProperty("json.array", "true");
    }

    @Test
    void shouldGenerateFileWith10000Records() throws Exception {
        // Generate JSON array file
        Path filePath = jsonfile.generateFile();
        assertTrue(Files.exists(filePath), "File not created");

        int recordCount = 0;

        // Stream-read JSON and count records
        try (BufferedReader br = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
             JsonReader reader = new JsonReader(br)) {

            reader.beginArray();
            while (reader.hasNext()) {
                reader.beginObject();
                while (reader.hasNext()) {
                    reader.nextName();
                    reader.skipValue(); // skip field content
                }
                reader.endObject();
                recordCount++;
            }
            reader.endArray();
        }

        // Must have exactly 10,000 records
        assertEquals(10_000, recordCount, "Record count should be 10,000");
        Files.deleteIfExists(filePath);
    }

    @Test
    void shouldGenerateFileInTxt() throws IOException {
        // Generate and verify extension is .json
        Path filePath = jsonfile.generateFile();
        String fileName = filePath.getFileName().toString();

        String lastPart = fileName.substring(fileName.lastIndexOf('.') + 1);
        assertEquals("json", lastPart);

        Files.deleteIfExists(filePath);
    }

    @Test
    void shouldDetectMalformedAndComputeStats() throws Exception {
        // One object: invalid region + non-numeric discount → 2 malforms
        String json = """
            {
              "id": 1,
              "region": "XX",
              "amount": 10.5,
              "tax": 0.1,
              "discount": ".",
              "revenue": 100.0,
              "transactions": 5,
              "customers": 10,
              "satisfaction": 4.5,
              "score": 80
            }
            """;

        // Feed JSON to reader
        try (InputStream in = new java.io.ByteArrayInputStream(
                json.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            jsonfile.readFile(in, "single.json");
        }

        // id aggregated
        var c0 = jsonfile.getColStat(0);
        assertEquals(1, c0.count());
        assertEquals(1.0, c0.sum(), 1e-9);

        // revenue aggregated
        var c5 = jsonfile.getColStat(5);
        assertEquals(1, c5.count());
        assertEquals(100.0, c5.sum(), 1e-9);

        // discount '.' not numeric → not counted
        var c4 = jsonfile.getColStat(4);
        assertEquals(0, c4.count(), "discount shouldn't be counted");

        // malformed: invalid region + non-numeric discount = 2
        assertEquals(2L, jsonfile.getMalformedCount("JSON"), "malformed JSON count");
    }
}