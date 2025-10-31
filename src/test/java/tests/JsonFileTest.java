package tests;

import com.google.gson.stream.JsonReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import projects.JsonFile;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonFileTest {
    private JsonFile jsonfile;

    @BeforeEach
    void setUp() {
        jsonfile = new JsonFile();
        System.setProperty("json.array", "true");
    }

    @Test
    void shouldGenerateFileWithId10000() throws Exception {
        // config zaten: System.setProperty("json.array", "true");
        Path filePath = jsonfile.generateFile();
        assertTrue(Files.exists(filePath), "File couldn't created");

        int lastId = -1;

        try (BufferedReader br = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
             JsonReader reader = new JsonReader(br)) {

            reader.beginArray();
            while (reader.hasNext()) {
                reader.beginObject();
                int currentId = -1;
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (name.equals("id")) currentId = reader.nextInt();
                    else reader.skipValue();
                }
                reader.endObject();
                lastId = currentId;
            }
            reader.endArray();
        }

        assertEquals(10_000, lastId, "Last record ID should be 10000");
        Files.deleteIfExists(filePath);
    }

    @Test
    void shouldGenerateFileInTxt() throws IOException {
        Path filePath = jsonfile.generateFile();
        String fileName = filePath.getFileName().toString();

        String lastPart = fileName.substring(fileName.lastIndexOf('.') + 1);
        assertEquals("json", lastPart);
        Files.deleteIfExists(filePath);
    }
}
