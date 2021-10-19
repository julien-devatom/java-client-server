package com.server;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Operation permet d'executer les opérations serveur, telles que lire les fichiers, créer des dossiers ...
 * Excepté pour la lecture et l'écriture de flux de fichiers ( upload & download), cette classe ne communique pas avec le client.
 * Autrement dit, le client ne recevra jamasi de message venant de cette classe. Tout cela ce passe dans la classe Connection.
 */
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

    /**
     * Execute les operations définies
     * @param operation opération a effectuer
     * @param args arguments si nécessaire
     * @return Le message a renvoyer au client
     */
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
        } catch (IOException | InvalidCommandExecutionException e) {
            // capture des erreurs d'execution des commandes
            return "ERROR : " + e.getMessage();
        }
    }

    /**
     * Cette methode permet de se déplacer dans l'arboréscence des fichiers de stockage.
     * Il y a une particularité pour l'argument ".." qui permet de remonter dans l'arborescence$
     *
     * On ne peut pas se déplacer vers un fichier en lui donnant unchemin absolue, mais uniquement un chemin relatif
     *
     * @param folderName : le nom nu dossier ou se déplacer
     * @throws InvalidCommandExecutionException : si on esssaie de remonter trop haut dans l'arborescence, on bloque l'utilisateur pour protéger le serveur
     */
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

    /**
     * Permet de lister les fichiers contenus dans le dossier courant
     * @return Un nom de fichier par ligne, "Empty directory" si le dossier est vide
     */
    private String list() {
        String[] pathnames = (new File(path)).list();
        if (pathnames != null && pathnames.length > 0)
            return String.join("\n", pathnames);
        else return "Empty directory";
    }

    /**
     * Permet de créer un dossier à partir du chemin courant
     * @param dirName nom du nouveau dossier
     * @throws InvalidCommandExecutionException : envoyé si le dossier existe déjà
     */
    private void makeDirectory(String dirName) throws InvalidCommandExecutionException {
        File file = new File(path, dirName);
        if (file.exists())
            // error message
            throw new InvalidCommandExecutionException("The directory " + dirName + " already exists.");
        if (!file.mkdirs())
            throw new InvalidCommandExecutionException("Cannot create the directory "+ dirName);

    }

    /**
     * Permet de supprimer uin fichier ou un dossier
     * @param filename : nom du fichier / dossier à supprimer
     * @throws InvalidCommandExecutionException : envoyé si le fichier/dossier n'existe pas
     */
    private void deleteFile(String filename) throws InvalidCommandExecutionException {
        File file = new File(path, filename);
        if (!file.exists())
            throw new InvalidCommandExecutionException("The file " + filename + " does not exist.");
        if (!file.delete())
            throw new InvalidCommandExecutionException("Cannot delete the file " + filename);
    }

    /**
     * Permet de télécherger un dossier du serveur vers le client
     * @param filename nom du fichier à télécharger
     * @throws InvalidCommandExecutionException : envoyé si le fichier ne peut pas être lue
     */
    private void download(String filename) throws InvalidCommandExecutionException {

        try{
            // on transfert tout le fichier d'un coup. Cela ne fonctionne pas pour les longs fichiers...
            byte[] bytes = Files.readAllBytes(Paths.get(path, filename));
            int length = bytes.length;
            os.writeInt(length);
            os.write(bytes,0,length);
        } catch (IOException e){
            throw new InvalidCommandExecutionException("file" + filename + "does not exist");
        }
    }

    /**
     * Permet de recevoir un fiichier d'un client
     * @param filename nom du fichier
     * @throws IOException Renvoyé si le client n'envoie pas bien le fichier : on doit d'abord recevoir la taille du fichier puis ses données
     * @throws InvalidCommandExecutionException Renvoyé si le fichier existe déjà
     *
     * NB : On capture toujours le flux du fichier même si on a une InvalidCommandExecutionException,
     * car le client l'a déjà envoyé. Ainsi, on clean le stream de données.
     */
    private void upload(String filename) throws IOException, InvalidCommandExecutionException {
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
