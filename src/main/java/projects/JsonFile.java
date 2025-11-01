package projects;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class JsonFile extends FileOps {
    private static final Logger log = LoggerFactory.getLogger(JsonFile.class);

    // Generates a 10,000-row JSON file with 5% malformed records
    public Path generateFile() {
        String fileName = "Sales" + randomUniqueInt() + ".json";
        Path filePath = Paths.get(fileName);
        log.info("Generating JSON file: {}", fileName);

        String flag = Config.get("json.array");
        if (flag == null) throw new IllegalStateException("config.properties: json.array not found");
        boolean asArray = Boolean.parseBoolean(flag);

        try (JsonWriter w = new JsonWriter(new FileWriter(filePath.toFile()))) {
            w.setIndent("  ");
            writeJsonStreaming(w, asArray, 10_000);
            createdFiles.add(filePath);
        } catch (IOException e) {
            log.error("Error creating JSON file: {}", fileName, e);
            throw new RuntimeException("Error creating file: " + fileName, e);
        }

        log.info("JSON file generated successfully: {}", fileName);
        return filePath;
    }

    // Writes JSON file using streaming mode
    private void writeJsonStreaming(JsonWriter w, boolean asArray, int count) throws IOException {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        if (asArray) {
            w.beginArray();
            for (int i = 1; i <= count; i++) writeRecord(w, i, rnd);
            w.endArray();
        } else {
            w.beginObject();
            w.name("records").beginArray();
            for (int i = 1; i <= count; i++) writeRecord(w, i, rnd);
            w.endArray();
            w.endObject();
        }
    }

    // Writes a single record; 5% chance to corrupt one field
    private void writeRecord(JsonWriter w, int id, ThreadLocalRandom rnd) throws IOException {
        final double MALFORM_PROB = 0.05;

        String[] keys = {
                "id", "region", "amount", "tax", "discount", "revenue",
                "transactions", "customers", "satisfaction", "score"
        };
        Object[] values = {
                id,
                REGIONS[rnd.nextInt(REGIONS.length)],
                rnd.nextDouble(10, 10000),
                rnd.nextDouble(0.00, 0.30),
                rnd.nextDouble(0.00, 0.60),
                rnd.nextDouble(0, 20000),
                rnd.nextInt(0, 1000),
                rnd.nextInt(0, 1000),
                rnd.nextDouble(1.0, 5.0),
                rnd.nextInt(0, 101)
        };

        Integer corruptIndex = (rnd.nextDouble() < MALFORM_PROB) ? rnd.nextInt(keys.length) : null;

        w.beginObject();
        for (int i = 0; i < keys.length; i++) {
            w.name(keys[i]);
            if (corruptIndex != null && i == corruptIndex) {
                w.value(".");
                continue;
            }

            Object v = values[i];
            if (v instanceof Number) {
                w.value(((Number) v).doubleValue());
            } else {
                w.value(String.valueOf(v));
            }
        }
        w.endObject();
    }

    @Override
    public boolean supports(String name) {
        return name.toLowerCase().endsWith(".json");
    }

    // Reads JSON file, validates regions, parses numeric fields, counts malformed cells
    @Override
    public void readFile(InputStream in, String entryName) throws IOException {
        log.info("Reading JSON file: {}", entryName);

        JsonReader jr = new JsonReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        jr.setLenient(true);

        // Define mapping between JSON keys and column indices
        Map<String, Integer> IDX = Map.of(
                "id", 0,
                "region", -1,
                "amount", 2,
                "tax", 3,
                "discount", 4,
                "revenue", 5,
                "transactions", 6,
                "customers", 7,
                "satisfaction", 8,
                "score", 9
        );

        // Determine JSON structure type
        JsonToken root = jr.peek();

        if (root == JsonToken.BEGIN_ARRAY) {
            jr.beginArray();
            while (jr.hasNext()) {
                // Parse each record and process its numeric and region fields
                JsonElement el = JsonParser.parseReader(jr);
                accumulateJsonElement(el, IDX, entryName);
            }
            jr.endArray();

        } else if (root == JsonToken.BEGIN_OBJECT) {
            //
            JsonElement rootEl = JsonParser.parseReader(jr);

            if (rootEl.isJsonObject() && rootEl.getAsJsonObject().has("records")) {
                //Object contains a "records" array
                JsonElement arr = rootEl.getAsJsonObject().get("records");

                if (arr != null && arr.isJsonArray()) {
                    // Iterate through all records inside "records"
                    for (JsonElement el : arr.getAsJsonArray()) {
                        accumulateJsonElement(el, IDX, entryName);
                    }
                } else {
                    // "records" field exists but is not a valid array
                    incMalformed("JSON", entryName);
                }
            } else {
                // Single object directly represents a record
                accumulateJsonElement(rootEl, IDX, entryName);
            }

        } else {
            //invalid format
            incMalformed("JSON", entryName);
        }

        log.info("Finished reading JSON file: {}", entryName);
    }


    //Processes a single JSON element and updates statistics.
    private void accumulateJsonElement(JsonElement el, Map<String, Integer> IDX, String entryName) {
        if (el == null || !el.isJsonObject()) {
            incMalformed("JSON", entryName);
            return;
        }

        var obj = el.getAsJsonObject();

        // Validate the "region" field, if present
        if (obj.has("region")) {
            JsonElement rv = obj.get("region");
            String region = (rv != null && rv.isJsonPrimitive()) ? rv.getAsString().trim() : "";
            if (!isValidRegion(region)) incMalformed("JSON", entryName);
        }

        // Validate numeric fields and update statistics
        for (var e : obj.entrySet()) {
            String name = e.getKey();
            Integer idx = IDX.get(name);

            // Skip unknown or non-numeric fields
            if (idx == null || idx < 0 || idx >= cols.length) continue;

            JsonElement v = e.getValue();

            // If the value is a valid number, include it in stats
            if (v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber()) {
                try {
                    addValue(idx, v.getAsDouble());
                } catch (Exception ex) {
                    incMalformed("JSON", entryName);
                }
            } else {
                // Field expected to be numeric but isn't
                incMalformed("JSON", entryName);
            }
        }
    }
}