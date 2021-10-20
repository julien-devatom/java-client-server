package com.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipFile {

    public static void zipFile(String filePath) throws IOException {
        try {
            System.out.println(filePath);
            File file = new File(filePath);
            String zipFileName = file.getAbsolutePath().concat(".zip");

            FileOutputStream fos = new FileOutputStream(zipFileName);
            ZipOutputStream zos = new ZipOutputStream(fos);

            zos.putNextEntry(new ZipEntry(file.getName()));

            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            zos.write(bytes, 0, bytes.length);
            zos.closeEntry();
            zos.close();

        } catch (FileNotFoundException ex) {
            System.err.format("The file %s does not exist", filePath);
        }
    }

}