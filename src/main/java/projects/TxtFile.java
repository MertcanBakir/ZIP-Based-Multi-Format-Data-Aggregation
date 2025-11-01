package projects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class TxtFile extends FileOps {
    private static final Logger log = LoggerFactory.getLogger(TxtFile.class);
    private static final int COLUMN_COUNT = 10;
    private static final double CORRUPTION_PROBABILITY = 0.05;

    // Generates 10k-row TXT file with 5% random cell corruption
    public Path generateFile() {
        String fileName = "Sales" + randomUniqueInt() + ".txt";
        Path filePath = Paths.get(fileName);
        log.info("Generating TXT file: {}", fileName);

        String delimiter = Config.get("txt.delimiter");
        if (delimiter == null || delimiter.isEmpty())
            throw new IllegalStateException("config.properties: 'txt.delimiter' missing");

        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            for (int i = 1; i <= 10_000; i++) {
                String[] cols = new String[COLUMN_COUNT];
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

                if (rnd.nextDouble() < CORRUPTION_PROBABILITY)
                    cols[rnd.nextInt(cols.length)] = ".";

                writer.write(String.join(delimiter, cols));
                writer.newLine();
            }
            createdFiles.add(filePath);
            log.info("TXT file generated successfully: {}", fileName);
            return filePath;
        } catch (IOException e) {
            log.error("TXT file generation failed: {}", fileName, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean supports(String name) {
        return name.toLowerCase().endsWith(".txt");
    }

    // Reads TXT file, validates regions, parses numeric fields, counts malformed cells
    @Override
    public void readFile(InputStream in, String entryName) throws IOException {
        log.info("Reading TXT entry: {}", entryName);
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

        String delimiter = Config.get("txt.delimiter");
        if (delimiter == null || delimiter.isEmpty()) {
            log.warn("Missing 'txt.delimiter', fallback to ';'");
            delimiter = ";";
        }

        String line;
        while ((line = r.readLine()) != null) {
            String[] parts = line.split(java.util.regex.Pattern.quote(delimiter), -1);
            int limit = Math.min(parts.length, cols.length);
            for (int i = 0; i < limit; i++) {
                String cell = parts[i].trim();

                // Validate region
                if (i == 1) {
                    if (!isValidRegion(cell)) incMalformed("TXT", entryName);
                    continue;
                }
                // Empty cell
                if (cell.isEmpty()) {
                    incMalformed("TXT", entryName);
                    continue;
                }
                // Validate numeric value
                try {
                    double v = Double.parseDouble(cell);
                    addValue(i, v);
                } catch (NumberFormatException ex) {
                    incMalformed("TXT", entryName);
                }
            }
        }
        log.info("Finished TXT read: {}", entryName);
    }
}