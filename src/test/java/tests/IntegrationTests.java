package tests;

import org.junit.jupiter.api.Test;
import projects.CsvFile;
import projects.FileOps;
import projects.JsonFile;
import projects.TxtFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationTests {

    @Test
    void shouldGenerateAndRead_SingleRowPerType() throws Exception {
        // Use same settings as your config
        System.setProperty("json.array", "true");
        System.setProperty("txt.delimiter", ";");
        System.setProperty("csv.delimiter", ",");

        // Prepare tiny contents (1 record per type)
        String txtLine = "1;TR;10.5;0.10;0.20;100.0;5;10;4.5;80\n"; // all numeric ok, region ok
        String csv = ""
                + "id,region,amount,tax,discount,revenue,transactions,customers,satisfaction,score\n" // header
                + "2,SA,20.0,0.05,.,200.0,3,8,4.0,90\n"; // discount '.' -> malformed (CSV)
        String json = """
            {
              "id": 3,
              "region": "EU",
              "amount": 30.0,
              "tax": 0.15,
              "discount": 0.05,
              "revenue": 300.0,
              "transactions": 7,
              "customers": 12,
              "satisfaction": 4.8,
              "score": 77
            }
            """;

        // Create a temp ZIP with 3 entries: sample.txt, sample.csv, sample.json
        Path zipPath = Files.createTempFile("mini_dataset", ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zos.putNextEntry(new ZipEntry("sample.txt"));
            zos.write(txtLine.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("sample.csv"));
            zos.write(csv.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("sample.json"));
            zos.write(json.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        // Create handlers and process the ZIP
        TxtFile txtReader = new TxtFile();
        CsvFile csvReader = new CsvFile();
        JsonFile jsonReader = new JsonFile();

        FileOps.processZip(zipPath, List.of(txtReader, csvReader, jsonReader));

        // --- Assertions ---

        // TXT: id=1, revenue=100.0; no malformed expected
        var t0 = txtReader.getColStat(0); // id
        assertEquals(1, t0.count());
        assertEquals(1.0, t0.sum(), 1e-9);

        var t5 = txtReader.getColStat(5); // revenue
        assertEquals(1, t5.count());
        assertEquals(100.0, t5.sum(), 1e-9);

        assertEquals(0L, txtReader.getMalformedCount("TXT"));

        // CSV: header skipped, one data row; discount='.' -> malformed
        var c0 = csvReader.getColStat(0); // id=2
        assertEquals(1, c0.count());
        assertEquals(2.0, c0.sum(), 1e-9);

        var c5 = csvReader.getColStat(5); // revenue=200.0
        assertEquals(1, c5.count());
        assertEquals(200.0, c5.sum(), 1e-9);

        var c4 = csvReader.getColStat(4); // discount='.' not parsed
        assertEquals(0, c4.count());

        assertEquals(1L, csvReader.getMalformedCount("CSV"));

        // JSON: all numeric ok, region ok; no malformed expected
        var j0 = jsonReader.getColStat(0); // id=3
        assertEquals(1, j0.count());
        assertEquals(3.0, j0.sum(), 1e-9);

        var j5 = jsonReader.getColStat(5); // revenue=300.0
        assertEquals(1, j5.count());
        assertEquals(300.0, j5.sum(), 1e-9);

        assertEquals(0L, jsonReader.getMalformedCount("JSON"));

        // cleanup
        Files.deleteIfExists(zipPath);
    }

    @Test
    void shouldGenerateFilesAndZip() throws Exception {
        System.setProperty("json.array", "true");

        // concrete generators
        TxtFile txtfile = new TxtFile();
        JsonFile jsonfile = new JsonFile();
        CsvFile csvfile = new CsvFile();

        // generate files
        Path txt = txtfile.generateFile();
        Path json = jsonfile.generateFile();
        Path csv = csvfile.generateFile();

        assertAll(
                () -> assertTrue(Files.exists(txt),  "TXT not created: "   + txt),
                () -> assertTrue(Files.exists(json), "JSON not created: "  + json),
                () -> assertTrue(Files.exists(csv),  "CSV not created: "   + csv)
        );

        // use a single collector instance
        txtfile.createdFiles.clear();
        txtfile.createdFiles.add(txt);
        txtfile.createdFiles.add(json);
        txtfile.createdFiles.add(csv);

        // compute ZIP path and ensure a clean start
        Path baseDir = txt.toAbsolutePath().getParent();
        Path zipPath = baseDir.resolve("input_dataset.zip");
        Files.deleteIfExists(zipPath);

        // create ZIP
        txtfile.zipAndCleanup();

        //ZIP must exist
        assertTrue(Files.exists(zipPath), "ZIP file was not created at: " + zipPath);

        //ZIP must contain exactly the generated filenames
        Set<String> entries = new HashSet<>();
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            zip.stream().map(ZipEntry::getName).forEach(entries::add);
        }

        // expected names = just the basenames of the generated files
        Set<String> expected = Set.of(
                txt.getFileName().toString(),
                json.getFileName().toString(),
                csv.getFileName().toString()
        );

        String debug = "ZIP entries: " + String.join(", ", entries);
        assertEquals(expected, entries, "ZIP entries mismatch. " + debug);

        //originals must be deleted after successful zipping
        assertFalse(Files.exists(txt),   "TXT file was not deleted: "   + txt);
        assertFalse(Files.exists(json),  "JSON file was not deleted: "  + json);
        assertFalse(Files.exists(csv),   "CSV file was not deleted: "   + csv);

        // cleanup ZIP
        Files.deleteIfExists(zipPath);
    }
}