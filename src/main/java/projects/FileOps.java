package projects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public abstract class FileOps {

    private static final Logger log = LoggerFactory.getLogger(FileOps.class);

    // Files produced by the program
    public final List<Path> createdFiles = new ArrayList<>();

    // Tracks used numbers to ensure uniqueness
    private static final Set<Integer> usedNumbers = new HashSet<>();

    // Region codes for data generation
    protected static final String[] REGIONS = {"TR", "SA", "EU"};

    // Malformed counters per file
    protected final Map<String, Integer> malformedByFile = new LinkedHashMap<>();

    // Column stats (10 columns)
    protected final Stat[] cols = new Stat[10];

    // Malformed counters per type
    protected final Map<String, Integer> malformedCounts = new HashMap<>();

    public FileOps() {
        for (int i = 0; i < cols.length; i++) cols[i] = new Stat();
        malformedCounts.put("TXT", 0);
        malformedCounts.put("CSV", 0);
        malformedCounts.put("JSON", 0);
    }

    protected void addValue(int col, double val) {
        if (col < cols.length) cols[col].add(val);
    }

    protected void incMalformed(String type, String entryName) {
        malformedCounts.merge(type, 1, Integer::sum);
        malformedByFile.merge(entryName, 1, Integer::sum);
    }

    public Stat getColStat(int i) { return cols[i]; }

    public long getMalformedCount(String type) {
        return malformedCounts.getOrDefault(type, 0);
    }

    public static Path writeSummaryTxtForAll(List<FileOps> sources) throws IOException {
        final String[] COL_NAMES = {
                "id", "region", "amount", "tax", "discount", "revenue",
                "transactions", "customers", "satisfaction", "score"
        };
        final int[] NUMERIC_IDXS = {2, 3, 4, 5, 6, 7, 8, 9};

        // Output path from config
        String outPathStr = Config.get("output.summary.path");
        if (outPathStr == null || outPathStr.isBlank()) {
            throw new IllegalStateException("config.properties: output.summary.path not found");
        }

        Path out = Paths.get(outPathStr);
        Path parent = out.getParent();
        if (parent != null) Files.createDirectories(parent);

        StringBuilder sb = new StringBuilder();

        // Aggregated stats for numeric columns (excluding region)
        for (int idx : NUMERIC_IDXS) {
            long totalCount = 0;
            double totalSum = 0.0;
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;

            for (FileOps s : sources) {
                Stat st = s.getColStat(idx);
                if (st.count() > 0) {
                    totalCount += st.count();
                    totalSum += st.sum();
                    min = Math.min(min, st.min());
                    max = Math.max(max, st.max());
                }
            }
            double avg = (totalCount > 0) ? (totalSum / totalCount) : 0.0;
            if (min == Double.POSITIVE_INFINITY) min = 0.0;
            if (max == Double.NEGATIVE_INFINITY) max = 0.0;

            sb.append(COL_NAMES[idx]).append(":\n\n");
            sb.append(String.format("    min = %.2f%n%n", min));
            sb.append(String.format("    max = %.2f%n%n", max));
            sb.append(String.format("    average = %.2f%n%n", avg));
            sb.append(String.format("    sum = %.2f%n%n", totalSum));
            sb.append("\n");
        }

        // Error summary per file
        sb.append("---- Error Summary ----\n\n");
        Map<String, Integer> perFile = new LinkedHashMap<>();
        for (FileOps s : sources) {
            for (var e : s.malformedByFile.entrySet()) {
                perFile.merge(e.getKey(), e.getValue(), Integer::sum);
            }
        }

        if (perFile.isEmpty()) {
            sb.append("(no malformed records)\n");
        } else {
            perFile.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> {
                        int n = e.getValue();
                        sb.append(String.format("%s: %d malformed record%s%n",
                                e.getKey(), n, (n == 1 ? "" : "s")));
                    });
        }

        Files.writeString(out, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        LoggerFactory.getLogger(FileOps.class)
                .info("Summary file written: {}", out.toAbsolutePath());
        return out;
    }

    // Zip all paths in createdFiles into a single archive and delete originals
    public void zipAndCleanup() {
        if (createdFiles.isEmpty()) {
            log.info("No files to zip");
        }

        Path zipPath = Paths.get("input_dataset.zip");
        Path tmpPath = zipPath.resolveSibling(zipPath.getFileName() + ".tmp");

        try {
            // Clean old ZIP and temp
            Files.deleteIfExists(tmpPath);
            Files.deleteIfExists(zipPath);

            log.info("Creating ZIP (temp): {}", tmpPath.toAbsolutePath());

            // Guard against duplicate entry names
            Set<String> seen = new HashSet<>();

            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tmpPath))) {
                for (Path file : createdFiles) {
                    if (!Files.isRegularFile(file)) {
                        log.info("Skipped non-regular file: {}", file);
                        continue;
                    }
                    String entryName = file.getFileName().toString();
                    if (!seen.add(entryName)) {
                        log.warn("Duplicate entry skipped: {}", entryName);
                        continue;
                    }
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(file, zos);
                    zos.closeEntry();
                }
            }

            // Move temp to final
            try {
                Files.move(tmpPath, zipPath, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmpPath, zipPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // Delete sources on success
            for (Path p : createdFiles) {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException delEx) {
                    log.warn("Could not delete: {}", p, delEx);
                }
            }

            log.info("ZIP created: {}", zipPath.toAbsolutePath());

        } catch (IOException e) {
            log.error("Failed to create ZIP: {}", zipPath.toAbsolutePath(), e);
            try { Files.deleteIfExists(tmpPath); } catch (IOException ignore) {}
        }
    }

    // Extension check
    public abstract boolean supports(String entryName);

    // Read/process a single file stream
    public abstract void readFile(InputStream in, String entryName) throws IOException;

    // Iterate entries in a ZIP and dispatch to the appropriate reader without extracting
    public static void processZip(Path zipPath, List<FileOps> readers) throws IOException {
        if (!Files.exists(zipPath)) {
            throw new NoSuchFileException(zipPath.toString());
        }
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if (e.isDirectory()) { zis.closeEntry(); continue; }

                String name = e.getName();
                FileOps handler = readers.stream()
                        .filter(r -> r.supports(name))
                        .findFirst()
                        .orElse(null);

                if (handler != null) {
                    handler.readFile(zis, name);
                } else {
                    log.info("No reader for entry: {}", name);
                }
                zis.closeEntry();
            }
        }
    }

    // Generate a random unique integer between 1 and 9999
    int randomUniqueInt() {
        int min = 1, max = 9999;
        if (usedNumbers.size() >= (max - min)) {
            System.out.println("All possible unique numbers were generated.");
            return -1;
        }
        int num = ThreadLocalRandom.current().nextInt(min, max);
        while (usedNumbers.contains(num)) {
            num = ThreadLocalRandom.current().nextInt(min, max);
        }
        usedNumbers.add(num);
        return num;
    }

    //checks region column is valid or not
    protected boolean isValidRegion(String s) {
        if (s == null) return false;

        for (String r : REGIONS) {
            if (r.equals(s)) return true;
        }
        return false;
    }
}