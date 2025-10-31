package projects;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        FileOps fileOps = new FileOps();

        TxtFile txtFile = new TxtFile();
        Path path = txtFile.generateFile();
        fileOps.createdFiles.add(path);


        CsvFile csvFile = new CsvFile();
        path = csvFile.generateFile();
        fileOps.createdFiles.add(path);


        JsonFile file = new JsonFile();
        path = file.GenerateFile();
        fileOps.createdFiles.add(path);

        fileOps.zipAndCleanup();


        System.out.println("İşlem tamamlandı.");

    }
}