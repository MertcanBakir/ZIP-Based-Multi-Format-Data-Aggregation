package projects;

import com.google.gson.stream.JsonWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ThreadLocalRandom;

public class JsonFile extends FileOps {

    Path GenerateFile() {
        String fileName = "Sales" + randomUniqueInt() + ".json";
        Path filePath = Paths.get(fileName);

        //take delimiter from config
        String flag = Config.get("json.array");
        if (flag == null) throw new IllegalStateException("config.properties: not found");
        boolean asArray = Boolean.parseBoolean(flag.trim());


        try (JsonWriter w = new JsonWriter(new FileWriter(filePath.toFile()))) {
            w.setIndent("  ");
            writeJsonStreaming(w, asArray, 10_000);
            createdFiles.add(filePath);
            System.out.println("JSON created: " + filePath.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return filePath;
    }

    //write with stream
    private void writeJsonStreaming(JsonWriter w, boolean asArray, int count) throws IOException {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();


        if (asArray) {
            //delimiter is true
            w.beginArray();
            for (int i = 1; i <= count; i++) writeRecord(w, i, rnd);
            w.endArray();
        } else {
            // delimiter is false
            w.beginObject();
            w.name("records").beginArray();
            for (int i = 1; i <= count; i++) writeRecord(w, i, rnd);
            w.endArray();
            w.endObject();
        }
    }


    //write the row
    private void writeRecord(JsonWriter w, int id, ThreadLocalRandom rnd) throws IOException {
        w.beginObject();
        w.name("id").value(id);
        w.name("region").value(REGIONS[rnd.nextInt(REGIONS.length)]);
        w.name("amount").value(round2(rnd.nextDouble(10, 10_000)));
        w.name("tax").value(round4(rnd.nextDouble(0.00, 0.30)));
        w.name("discount").value(round4(rnd.nextDouble(0.00, 0.60)));
        w.name("revenue").value(round2(rnd.nextDouble(0, 20_000)));
        w.name("transactions").value(rnd.nextInt(0, 1_000));
        w.name("customers").value(rnd.nextInt(0, 1_000));
        w.name("satisfaction").value(round2(rnd.nextDouble(1.0, 5.0)));
        w.name("score").value(rnd.nextInt(0, 101));
        w.endObject();
    }

    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
    private static double round4(double v) { return Math.round(v * 10_000.0) / 10_000.0; }
}