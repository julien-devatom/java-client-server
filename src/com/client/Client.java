package com.client;

import com.utils.ZipFile;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class Client {

    public static String HOST = ""; // ip du serveur
    public static int PORT = 0; // port du serveur
    private DataOutputStream os; // flux pour ecrire au serveur
    private DataInputStream is; // flux pour lire les données serveur
    private static final BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in)); // flux pour lire les commandes du client
    private Socket connection; // defini la connection socket avec le client. On la sauvegarde pour pouvoir déconnecter le client proprement.

    /**
     * Permet d'ivalider l'ip HOST en tant que IPv4
     *
     * @return true si l'ip est Invalide
     */
    public static boolean isIPInvalid() {
        // IPv4 pattern
        String PATTERN = "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$";
        return !Client.HOST.matches(PATTERN);
    }

    public Client() throws IOException {
        try {
            SetupServerAddressAndPort();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Connected to the server " + connection.toString());
    }

    /**
     * Permet au client d'entrer le serveur sur lequel il souhaite se connecter
     * L'ip doit etre une IPv4, et le port dans le range [5002, 5049]
     * <p>
     * Le client doit entrer une IP valide avant de choisir son port
     *
     * @throws IOException Renvoye si il y a une erreur avec la console client
     */
    private void SetupServerAddressAndPort() throws IOException {
        // validation de l'ip
        while (Client.isIPInvalid()) {
            System.out.println("Please enter the IP address of the server (XXX.XXX.XXX.XXX)");
            System.out.print("IP address = ");
            Client.HOST = consoleReader.readLine();
            if (Client.isIPInvalid())
                System.out.println(Client.HOST + " is not a valid IPv4...");
        }
        // validation du port
        while (Client.PORT < 5002 || Client.PORT > 5049) {
            System.out.println("Please select the port of the server (between 5002 et 5049) ");
            try {
                System.out.print("Port = ");
                Client.PORT = Integer.parseInt(consoleReader.readLine());
                if (Client.PORT < 5002 || Client.PORT > 5049)
                    System.out.println(Client.PORT + " is not a valid port");
            } catch (NumberFormatException e) {
                // le client a entre des lettres...
                System.out.println("Please enter a number..");
            }
        }
        // et on tente de se connecter au dit serveur
        serverConnection();
    }

    /**
     * Connection avec le serveur, dont l'ip et le port a ete valide.
     * On defini la connection socket ainsi que les flux de communication avec le serveur
     *
     * @throws IOException
     */
    private void serverConnection() throws IOException {
        // on se connecte au serveur
        try {
            System.out.println("Connection to the server " + Client.HOST + ":" + Client.PORT + " ...");
            connection = new Socket(Client.HOST, Client.PORT);
            // on initialise les connections avec le serveur
            os = new DataOutputStream(connection.getOutputStream());
            is = new DataInputStream(connection.getInputStream());


            System.out.println("Client connected");
            listen();
        } catch (ConnectException | UnknownHostException e) {
            System.out.println("Server" + Client.HOST + ":" + Client.PORT + " Unreachable");
            Client.HOST = "";
            Client.PORT = 0;
            SetupServerAddressAndPort();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * On attend une commande de l'utilisateur.
     * Lorsqu'on capture une commande, on verifie si elle est bien formulee avant d'envoyer des donnees au serveur
     * Les commandes upload et download necessite plus de logique cote client que les autres, etant donne qu'on doit
     * envoyer / recevoir un flux de donnees avant de reecouter une commance du client
     *
     * @throws IOException si il y a une erreur avec le terminal du client
     */
    public void listen() throws IOException {
        String inputCommand = consoleReader.readLine();
        try {
            // on confirme la commande
            inputCommand = cleanAndConfirmCommand(inputCommand);
        } catch (InvalidClientExecutionException e) {
            // la commande est mal formulee
            System.out.println(e.getMessage());
            listen();
            return;
        }
        try{
            // la commande a ete verifiee, on l'envoie donc au serveur
            os.writeUTF(inputCommand);
            if (Objects.equals(inputCommand.split(" ")[0], "upload"))
                upload(inputCommand);
            if (Objects.equals(inputCommand.split(" ")[0], "download"))
                download(inputCommand);
            if (Objects.equals(inputCommand.split(" ")[0], "disconnect")) {
                disconnect();
                return;
            }
        } catch (InvalidClientExecutionException e) {
            System.out.println("ERROR : " + e.getMessage());
        }

        String response;
        try {
            while (!Objects.equals(response = is.readUTF(), "EOF")) {
                System.out.println(response);
                if (response.equals("bye")) {
                    disconnect();
                    return;
                }
            }
        } catch (EOFException e) {
            disconnect();
            return;
        }
        listen();
    }

    /**
     * Deconnection du serveur
     *
     * @throws IOException : si il y a un probleme avec la connection socket
     */
    private void disconnect() throws IOException {
        connection.close();
        System.out.println("Server disconnected");
        Client.HOST = "";
        Client.PORT = 0;
        SetupServerAddressAndPort();
    }

    /**
     * Permet de confirmer la structure d'une commande avant de l'envoyer au serveur.
     * On n'empeche pas les commandes que l'on ne connais pas cote client,
     * immaginons que le servaur accepte d'autre commandes que le client ne connaitrait pas
     * (par exemple pwd), on laisse le serveur nous dire quelles sont les commandes possibles
     *
     * @param inputCommand commande d'entree
     * @throws InvalidClientExecutionException : la commande est mal structuree.
     * @return La commande modifie si besoin (pour zipper)
     */
    private String cleanAndConfirmCommand(String inputCommand) throws InvalidClientExecutionException {
        String command = inputCommand.split(" ")[0];
        // on doit specifier un fichier a telecharger
        if (Objects.equals(command, "download") & inputCommand.split(" ").length < 2) {
            throw new InvalidClientExecutionException("Please, enter a filename");
        }
        // on doit specifier un fichier a uploader
        if (Objects.equals(command, "upload") & inputCommand.split(" ").length < 2) {
            throw new InvalidClientExecutionException("Please, enter a filename");
        }
        // on doit specifier un fichier a supprimer
        if (Objects.equals(command, "remove") & inputCommand.split(" ").length < 2) {
            throw new InvalidClientExecutionException("Please, enter a filename");
        }
        // on verifie si le fichier a upload existe, avant de prevenir le serveur
        if (Objects.equals(command, "upload")) {
            if (inputCommand.split(" ").length > 2) {
                if (Objects.equals(inputCommand.split(" ")[2], "-z")) {
                    inputCommand = inputCommand.replace(inputCommand.split(" ")[1], inputCommand.split(" ")[1].concat(".zip"));
                }
            }
            if (!(new File(inputCommand.split(" ")[1])).exists() || !(new File(inputCommand.split(" ")[1].replace(".zip", "")).exists()))
                throw new InvalidClientExecutionException("Filename " + inputCommand.split(" ")[1] + " doesn't exist");
        }
        return inputCommand;
    }

    /**
     * Permet d'uploader un fichier sur le serveur
     *
     * @param inputCommand pour recuperer le nom du fichier
     * @throws IOException si il y a une erreur lors de la lecture du fichier
     */
    private void upload(String inputCommand) throws IOException {
        String filename = inputCommand.split(" ")[1];
        if (inputCommand.split(" ").length > 2){
            if(Objects.equals(inputCommand.split(" ")[2], "-z")){
                if (!(new File(filename)).exists())
                    ZipFile.zipFile(filename.replace(".zip", ""));
            }
        }
        // on upload tout le fichier d'un coup. Cela ne fonctionne pas pour les longs fichiers...
        byte[] bytes = Files.readAllBytes(Paths.get(filename));
        int length = bytes.length;
        os.writeInt(length);
        os.write(bytes, 0, length);
    }

    /**
     * Permet de telecharger un fichier distant
     *
     * @param inputCommand permet de recuperer le nom du fichier
     * @throws IOException si il y a une erreur lors de la lecture/l'ecriture du fichier
     */
    private void download(String inputCommand) throws IOException {
        String filename = inputCommand.split(" ")[1];
        if(inputCommand.split(" ").length > 2 && Objects.equals(inputCommand.split(" ")[2], "-z") && filename.split(".zip").length == 1)
            filename = filename.concat(".zip");
        File file = new File(filename);

        // on lit les donnees envoyees, correspondant au fichier a telecharger
        int length = is.readInt();
        byte[] buffer = new byte[length];
        int count = 0;
        while (count < length) {
            count += is.read(buffer, count, length - count);
        }
        // si le fichier existe deja, on ecrit rien.
        if (!file.createNewFile())
            throw new InvalidClientExecutionException("File " + filename + " already exists...");
        // sinon, on ecrit le fichier
        try (FileOutputStream outFile = new FileOutputStream(file.getAbsolutePath())) {
            outFile.write(buffer);
            outFile.flush();
        }
        // on ferme toujours le fichier pour eviter de le garder en memoire !
    }

    public static void main(String[] args) {
        try {
            Client client = new Client();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}