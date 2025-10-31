package projects;

import java.io.*;
import java.nio.file.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class FileOps {

    // files produces by the program
    public java.util.List<Path> createdFiles = new java.util.ArrayList<>();

    // tracks used numbers to ensure uniqueness
    private static final Set<Integer> usedNumbers = new HashSet<>();

    // region codes for data generation
    protected static final String[] REGIONS = {"TR", "SA", "EU"};


    // Zip all paths in createdFiles into a single archive and delete originals.
    public void zipAndCleanup() {
        if (createdFiles.isEmpty()) {
            System.out.println("No file");
            return;
        }

        String zipName = "input_dataset.zip";
        Path zipPath = Paths.get(zipName);
        System.out.println(zipPath);

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            for (Path file : createdFiles) {
                if (Files.isRegularFile(file)) {
                    zos.putNextEntry(new ZipEntry(file.getFileName().toString()));
                    Files.copy(file, zos);
                    zos.closeEntry();
                } else {
                    System.out.println("Skipped (not a file): " + file);
                }
            }
        } catch (IOException e) {
            System.out.println("Failed to create ZIP: " + e.getMessage());
            return; // do not delete originals if zipping failed
        }

        // delete originals after a successful ZIP
        for (Path p : createdFiles) {
            try {
                Files.deleteIfExists(p);
            } catch (IOException e) {
                System.out.println("Could not delete: " + p.getFileName());
            }
        }

        System.out.println("Created ZIP: " + zipPath.toAbsolutePath());
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
}