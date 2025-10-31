package projects;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class TxtFile extends FileOps {


    //generates 10000 row txt file with malforms in 0.05 probability
    public Path generateFile() {
        String fileName = "Sales" + randomUniqueInt() + ".txt";
        Path filePath = Paths.get(fileName);

        //take delimiter from config
        String delimiter = Config.get("txt.delimiter");
        if (delimiter == null || delimiter.isEmpty()) {
            throw new IllegalStateException("config.properties: delimiter not found ");
        }

        final double PROB = 0.05;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        //write the row in a random range
        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            for (int i = 1; i <= 10000; i++) {
                String[] cols = new String[10];
                cols[0] = String.valueOf(i);
                cols[1] = REGIONS[rnd.nextInt(REGIONS.length)];
                cols[2] = String.format(Locale.US, "%.2f", rnd.nextDouble(1.0, 100.0));
                cols[3] = String.format(Locale.US, "%.2f", rnd.nextDouble(0.00, 0.25));
                cols[4] = String.format(Locale.US, "%.2f", rnd.nextDouble(0.00, 0.50));
                cols[5] = String.format(Locale.US, "%.2f", rnd.nextDouble(100.0, 10_000.0));
                cols[6] = String.format(Locale.US, "%.2f", rnd.nextDouble(1.0, 200.0));
                cols[7] = String.format(Locale.US, "%.2f", rnd.nextDouble(1.0, 2_000.0));
                cols[8] = String.format(Locale.US, "%.2f", rnd.nextDouble(0.0, 100.0));
                cols[9] = String.format(Locale.US, "%.2f", rnd.nextDouble(0.0, 100.0));

                // 5% chance of corrupting a cell
                if (rnd.nextDouble() < PROB) {
                    int col = rnd.nextInt(cols.length);
                    cols[col] = ".";
                }
                //write the row to the file
                writer.write(String.join(delimiter, cols));
                writer.newLine();
            }

            createdFiles.add(filePath);
            System.out.println("File created: " + fileName);

        } catch (IOException e) {
            throw new RuntimeException("Error creating file: " + fileName, e);
        }
        return filePath;
    }
}