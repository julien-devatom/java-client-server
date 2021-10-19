package com.devatom.server;

import java.io.*;
import java.util.Arrays;
import java.util.Objects;

public class Operation {

    public static String[] enabledOperations = new String[] {"mkdir", "ls", "cd", "delete", "download", "upload"};
    private String path = "";
    private InputStream is;
    private OutputStream os;
    private final String basePath;

    public Operation(String basePath, InputStream is, OutputStream os) {
        this.basePath = basePath;
        this.path = basePath;
        this.is = is;
        this.os = os;
    }
    public String execute(String operation, String[] args){
        try {
            String output;
            String dirName;
            String fileName;
            switch (operation) {
                case "mkdir":
                    dirName = args[0];
                    makeDirectory(dirName);
                    output = dirName + " created";
                    break;
                case "ls":
                    output = list();
                    break;
                case "cd":
                    dirName = args[0];
                    changeDirectory(dirName);
                    output = "new path: " + path;
                    break;
                case "delete":
                    fileName = args[0];
                    deleteFile(fileName);
                    output = "file " + fileName + " deleted";
                    break;
                case "download":
                    fileName = args[0];
                    download(fileName);
                    output = "file " + fileName + " downloaded";
                    break;
                case "upload":
                    fileName = args[0];
                    upload(fileName);
                    output = "file " + fileName + " uploaded successfully !";
                    break;
                default:
                    throw new CommandNotFoundException(operation, args);
            }
            return output;
        } catch (IOException e) {
            return "ERROR : " + e.getMessage();
        }
    }
    private void changeDirectory(String folderName) throws InvalidCommandExecutionException {
        if (folderName.equals("..")) {
            if (Objects.equals(path, basePath))
                // There is no parent repertory
                throw new InvalidCommandExecutionException("There is no parent directory.");
            path = (new File(path)).getParentFile().getAbsolutePath();
        }
        else {
            String newPath = (new File(path, folderName)).getAbsolutePath();

            if (!(new File(newPath)).exists())
                // error message
                throw new InvalidCommandExecutionException("The directory " + folderName + " does not exist.");
            else {
                path = newPath;
            }
        }
    }
    private String list() {
        String[] pathnames = (new File(path)).list();
        if (pathnames != null)
            return String.join("\n", pathnames);
        else return "Empty directory";
    }

    private void makeDirectory(String dirName) throws InvalidCommandExecutionException {
        File file = new File(path, dirName);
        if (file.exists())
            // error message
            throw new InvalidCommandExecutionException("The directory " + dirName + " already exists.");
        if (!file.mkdirs())
            throw new InvalidCommandExecutionException("Cannot create the directory "+ dirName);

    }
    private void deleteFile(String filename) throws InvalidCommandExecutionException {
        File file = new File(path, filename);
        if (!file.exists())
            throw new InvalidCommandExecutionException("The file " + filename + " does not exist.");
        if (!file.delete())
            throw new InvalidCommandExecutionException("Cannot delete the file " + filename);
    }
    private void download(String filename) throws IOException {
        String localPath = (new File(path, filename)).getAbsolutePath();
        try {
            FileInputStream fileInputStream = new FileInputStream(localPath);
            byte[] bytes = new byte[16*1024];

            int count;
            while ((count = fileInputStream.read(bytes)) > 0) {
                os.write(bytes, 0, count);
            }
        } catch (FileNotFoundException e) {
            throw new InvalidCommandExecutionException("File " + filename + " doesn't exists");
        }
    }
    private void upload(String filename) throws IOException {
        File newFile = new File(path, filename);
        if (!newFile.createNewFile())
            throw new InvalidCommandExecutionException("file " + filename + " already exists..");

        FileOutputStream out = new FileOutputStream(newFile.getAbsolutePath());
        // on copy le flux de données
        byte[] bytes = new byte[16*1024];
        int count;
        while ((count = is.read(bytes)) > 0) {
            System.out.print("Read " + count + " bytes" + bytes.toString() + "\r");
            out.write(bytes, 0, count);
        }
        System.out.println("Uploadedddd");
        out.close();

    }
}
