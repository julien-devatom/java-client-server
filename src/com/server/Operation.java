package com.server;

import com.utils.ZipFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Path.*;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Operation permet d'executer les operations serveur, telles que lire les fichiers, creer des dossiers ...
 * Excepte pour la lecture et l'ecriture de flux de fichiers ( upload & download), cette classe ne communique pas avec le client.
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
     * Execute les operations definies
     * @param operation operation a effectuer
     * @param args arguments si necessaire
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
                    Boolean toZip = args.length > 1 && Objects.equals(args[1], "-z") && args[0].split(".zip").length == 1;
                    download(fileName, toZip);
                    if(toZip)
                        output = "file " + fileName  + ".zip downloaded";
                    else
                        output = "file " + fileName  + " downloaded";
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
     * Cette methode permet de se deplacer dans l'arborescence des fichiers de stockage.
     * Il y a une particularite pour l'argument ".." qui permet de remonter dans l'arborescence.
     *
     * On ne peut pas se deplacer vers un fichier en lui donnant unchemin absolue, mais uniquement un chemin relatif
     *
     * @param folderName : le nom nu dossier ou se deplacer
     * @throws InvalidCommandExecutionException : si on esssaie de remonter trop haut dans l'arborescence, on bloque l'utilisateur pour proteger le serveur
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
        String[] fileTypes = new String[pathnames.length];
        String[] typeWithNames =new String[pathnames.length];
        if (pathnames != null && pathnames.length > 0) {
            for(int i=0; i!=pathnames.length; i++) {
                File file = new File(Paths.get(path, pathnames[i]).toString());
                if (file.isFile()) fileTypes[i]="[File] ";
                else fileTypes[i]="[Folder] ";
                typeWithNames[i]=fileTypes[i]+pathnames[i];
            }

            return String.join("\n", typeWithNames);
        }

        else return "Empty directory";
    }

    /**
     * Permet de creer un dossier a partir du chemin courant
     * @param dirName nom du nouveau dossier
     * @throws InvalidCommandExecutionException : envoye si le dossier existe deja
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
     * @param filename : nom du fichier / dossier a supprimer
     * @throws InvalidCommandExecutionException : envoye si le fichier/dossier n'existe pas
     */
    private void deleteFile(String filename) throws InvalidCommandExecutionException {
        File file = new File(path, filename);
        if (!file.exists())
            throw new InvalidCommandExecutionException("The file " + filename + " does not exist.");
        if (!file.delete())
            throw new InvalidCommandExecutionException("Cannot delete the file " + filename);
    }

    /**
     * Permet de telecharger un dossier du serveur vers le client
     * @param filename nom du fichier a telecharger
     * @throws InvalidCommandExecutionException : envoye si le fichier ne peut pas etre lue
     */
    private void download(String filename, Boolean toZip) throws InvalidCommandExecutionException, IOException {
        if (toZip) {
            ZipFile.zipFile(Paths.get(path, filename).toString());
            filename = filename.concat(".zip");
        }
        try{
            // on transfert tout le fichier d'un coup. Cela ne fonctionne pas pour les longs fichiers... probleme de memoire
            byte[] bytes = Files.readAllBytes(Paths.get(path, filename));
            int length = bytes.length;
            System.out.println(length);
            os.writeInt(length);
            os.write(bytes,0,length);
        } catch (IOException e){
            throw new InvalidCommandExecutionException("file " + filename + " does not exist");
        }
        if(toZip) // on supprime l'archive cree une fois le transfert effectue
            (new File(path, filename)).delete();
    }

    /**
     * Permet de recevoir un fichier d'un client
     * @param filename nom du fichier
     * @throws IOException Renvoye si le client n'envoie pas bien le fichier : on doit d'abord recevoir la taille du fichier puis ses donnÃ©es
     * @throws InvalidCommandExecutionException Renvoye si le fichier existe deja 
     *
     * NB : On capture toujours le flux du fichier meme si on a une InvalidCommandExecutionException,
     * car le client l'a deja  envoye. Ainsi, on clean le stream de donnees.
     */
    private void upload(String filename) throws IOException, InvalidCommandExecutionException {
        String fullPath = Paths.get(path, filename).toString();

        // on lit les donnees envoyees, correspondant au fichier a telecharger
        int length = is.readInt();
        byte[] buffer  = new byte [length];
        int count = 0;
        while (count<length) {
            count+= is.read(buffer, count, length-count);
        }
        // si le fichier existe deja , on ecrit rien
        if ((new File(fullPath)).exists())
            throw new InvalidCommandExecutionException("File " + fullPath + " already exists");
        // sinon, on ecrit le fichier
        // on ferme toujours le fichier pour eviter de le garder en memoire, via l'automatic ressource managment
        try (OutputStream outFile = new FileOutputStream(Paths.get(path, filename).toString())) {
            outFile.write(buffer);
            outFile.flush();
        }
    }
}