package projects;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        // Create file handlers
        TxtFile txt = new TxtFile();
        CsvFile csv = new CsvFile();
        JsonFile json = new JsonFile();

        // Generate files
        Path t = txt.generateFile();
        Path c = csv.generateFile();
        Path j = json.generateFile();

        // Add all files to one list and zip them
        txt.createdFiles.addAll(List.of(t, c, j));
        txt.zipAndCleanup();

        // Read the ZIP and process files
        try {
            FileOps.processZip(Path.of("input_dataset.zip"), List.of(txt, csv, json));
        } catch (IOException e) {
            System.err.println("Error reading ZIP: " + e.getMessage());
        }

        // Write summary statistics
        try {
            FileOps.writeSummaryTxtForAll(List.of(txt, csv, json));
        } catch (IOException e) {
            System.err.println("Error writing summary: " + e.getMessage());
        }
    }
}