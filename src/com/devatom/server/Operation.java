package com.devatom.server;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

public class Operation {

    public static String[] enabledOperations = new String[] {"mkdir", "ls", "cd", "delete", "download", "upload"};
    private String path;
    private DataInputStream is;
    private DataOutputStream os;
    private final String basePath;

    public Operation(String basePath, DataInputStream is, DataOutputStream os) throws IOException {
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

        // on transfert tout le fichier d'un coup. Cela ne fonctionne pas pour les longs fichiers...
        byte[] bytes = Files.readAllBytes(Paths.get(path, filename));
        int length = bytes.length;
        os.writeInt(length);
        os.write(bytes,0,length);
    }
    private void upload(String filename) throws IOException {
        String fullPath = Path.of(path, filename).toString();

        // on lit les données envoyées, correspondant au fichier a télécharger
        int length = is.readInt();
        byte[] buffer  = new byte [length];
        int count = 0;
        while (count<length) {
            count+= is.read(buffer, count, length-count);
        }
        // si le fichier existe déjà, on écrit rien
        if ((new File(fullPath)).exists())
            throw new InvalidCommandExecutionException("File " + fullPath + " already exists");
        // sinon, on écrit le fichier
        // on ferme toujours le fichier pour éviter de le garder en mémoire, via l'automatic ressource managment
        try (OutputStream outFile = new FileOutputStream(Path.of(path, filename).toString())) {
            outFile.write(buffer);
            outFile.flush();
        }
    }
}
