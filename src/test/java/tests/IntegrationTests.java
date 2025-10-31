package tests;

import org.junit.jupiter.api.Test;
import projects.CsvFile;
import projects.FileOps;
import projects.JsonFile;
import projects.TxtFile;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationTests {

    @Test
    void shouldGenerateFilesAndZip() throws Exception {
        System.setProperty("json.array", "true");

        TxtFile txtfile = new TxtFile();
        JsonFile jsonfile = new JsonFile();
        CsvFile csvfile = new CsvFile();
        FileOps fileops = new FileOps();

        Path txt = txtfile.generateFile();
        Path json = jsonfile.generateFile();
        Path csv = csvfile.generateFile();

        fileops.createdFiles.addAll(List.of(txt, json, csv));

        // Create ZIP file
        fileops.zipAndCleanup();

        // ZIP location
        Path baseDir = txt.toAbsolutePath().getParent();
        Path zipPath = baseDir.resolve("input_dataset.zip");

        //Check ZIP exists
        assertTrue(Files.exists(zipPath), "ZIP file was not created");

        //Check ZIP contents
        Set<String> names = new HashSet<>();
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            zip.stream().map(ZipEntry::getName).forEach(names::add);
        }

        assertTrue(names.stream().anyMatch(n -> n.endsWith(".txt")), "Missing TXT file");
        assertTrue(names.stream().anyMatch(n -> n.endsWith(".json")), "Missing JSON file");
        assertTrue(names.stream().anyMatch(n -> n.endsWith(".csv")), "Missing CSV file");
        assertEquals(3, names.size(), "Unexpected number of entries in ZIP");

        //Check if original files are deleted
        assertFalse(Files.exists(txt), "TXT file was not deleted");
        assertFalse(Files.exists(json), "JSON file was not deleted");
        assertFalse(Files.exists(csv), "CSV file was not deleted");

        Files.deleteIfExists(zipPath);
    }
}