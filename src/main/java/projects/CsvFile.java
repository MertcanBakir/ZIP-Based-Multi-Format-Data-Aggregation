package projects;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvFile extends FileOps {

    private static final Logger log = LoggerFactory.getLogger(CsvFile.class);

    // Generates a 10,000-row CSV file with 5% malformed cells
    public Path generateFile() {
        String fileName = "Sales" + randomUniqueInt() + ".csv";
        Path filePath = Paths.get(fileName);
        log.info("Generating CSV file: {}", fileName);

        String delimiter = Config.get("csv.delimiter");
        if (delimiter == null || delimiter.isEmpty()) {
            throw new IllegalStateException("config.properties: csv.delimiter not found");
        }

        final double MALFORM_PROB = 0.05;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            for (int i = 0; i < 10000; i++) {
                String[] cols = new String[10];
                cols[0] = String.valueOf(i + 1);
                cols[1] = REGIONS[rnd.nextInt(REGIONS.length)];
                cols[2] = String.format(Locale.US, "%.2f", rnd.nextDouble(1.0, 100.0));
                cols[3] = String.format(Locale.US, "%.2f", rnd.nextDouble(0.00, 0.25));
                cols[4] = String.format(Locale.US, "%.2f", rnd.nextDouble(0.00, 0.50));
                cols[5] = String.format(Locale.US, "%.2f", rnd.nextDouble(100.0, 10_000.0));
                cols[6] = String.format(Locale.US, "%.2f", rnd.nextDouble(1.0, 200.0));
                cols[7] = String.format(Locale.US, "%.2f", rnd.nextDouble(1.0, 2_000.0));
                cols[8] = String.format(Locale.US, "%.2f", rnd.nextDouble(0.0, 100.0));
                cols[9] = String.format(Locale.US, "%.2f", rnd.nextDouble(0.0, 100.0));

                // Introduce malformed cell with 5% probability
                if (rnd.nextDouble() < MALFORM_PROB) {
                    cols[rnd.nextInt(cols.length)] = ".";
                }

                writer.write(String.join(delimiter, cols));
                writer.newLine();
            }
            createdFiles.add(filePath);

        } catch (IOException e) {
            log.error("Error creating CSV file: {}", fileName, e);
            throw new RuntimeException("Error creating file: " + fileName, e);
        }

        log.info("CSV file generated successfully: {}", fileName);
        return filePath;
    }

    @Override
    public boolean supports(String name) {
        return name.toLowerCase().endsWith(".csv");
    }

    // Reads CSV file, validates regions, parses numeric fields, counts malformed cells
    @Override
    public void readFile(InputStream in, String entryName) throws IOException {
        log.info("Reading CSV file: {}", entryName);

        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

        String delimiter = Config.get("csv.delimiter");
        if (delimiter == null || delimiter.isEmpty()) {
            log.warn("CSV delimiter not found, using default ','");
            delimiter = ",";
        }

        String line;
        boolean headerSkipped = false;

        while ((line = reader.readLine()) != null) {
            // Skip header row if not yet skipped
            if (!headerSkipped) {
                headerSkipped = true;
                continue;
            }

            String[] parts = line.split(java.util.regex.Pattern.quote(delimiter), -1);
            int limit = Math.min(parts.length, cols.length);

            for (int i = 0; i < limit; i++) {
                String cell = parts[i].trim();

                // Validate region
                if (i == 1) {
                    if (!isValidRegion(cell)) incMalformed("CSV", entryName);
                    continue;
                }

                // Empty cell
                if (cell.isEmpty()) {
                    incMalformed("CSV", entryName);
                    continue;
                }

                // Validate numeric cell
                try {
                    double v = Double.parseDouble(cell);
                    addValue(i, v);
                } catch (NumberFormatException ex) {
                    incMalformed("CSV", entryName);
                }
            }
        }
        log.info("Finished reading CSV file: {}", entryName);
    }
}