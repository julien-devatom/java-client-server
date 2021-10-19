package com.devatom.client;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class Client {

    public static String HOST = "";
    public static int PORT = 0;
    private  DataOutputStream os;
    private  DataInputStream is;
    private static final BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
    private  Socket connection;

    public static boolean isIPInvalid() {
        // IPv4 pattern
        String PATTERN = "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$";
        return !Client.HOST.matches(PATTERN);
    }

    public Client() throws IOException {
        try{
            SetupServerAddressAndPort();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Connected to the server " + connection.toString());
    }
    private void SetupServerAddressAndPort() throws IOException {
        while (Client.isIPInvalid()){
            System.out.println("Please enter the IP address of the server (XXX.XXX.XXX.XXX0)");
            System.out.print("IP address = ");
            Client.HOST = consoleReader.readLine();
            if(Client.isIPInvalid())
                System.out.println(Client.HOST + " is not a valid IPv4...");
        }

        while(Client.PORT<5002 || Client.PORT>5049)
        {
            System.out.println("Please select the port of the server (between 5002 et 5049) ");
            try{
                System.out.print("Port = ");
                Client.PORT = Integer.parseInt(consoleReader.readLine());
                if(Client.PORT<5002 || Client.PORT>5049)
                    System.out.println(Client.PORT + " is not a valid port");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number..");
            }
        }
        serverConnection();
    }
    private void serverConnection() throws IOException {
        // on se connecte au serveur
        try {
            System.out.println("Connection to the server " + Client.HOST + ":" + Client.PORT + " ...");
            connection = new Socket(Client.HOST, Client.PORT);
            // on initialise les connections avec le serveur
            os = new DataOutputStream(connection.getOutputStream());
            is = new DataInputStream(connection.getInputStream());
        } catch (ConnectException | UnknownHostException e) {
            System.out.println("Server" + Client.HOST + ":" + Client.PORT + " Unreachable");
            SetupServerAddressAndPort();
            } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listen() throws IOException {
        String inputCommand = consoleReader.readLine();
        try{
            confirmCommand(inputCommand);
        } catch (InvalidClientExecutionException e) {
            System.out.println(e.getMessage());
            listen();
            return;
        }
        os.writeUTF(inputCommand);
        if (Objects.equals(inputCommand.split(" ")[0], "upload"))
            upload(inputCommand);
        if (Objects.equals(inputCommand.split(" ")[0], "download"))
            download(inputCommand);
        if (Objects.equals(inputCommand.split(" ")[0], "disconnect")) {
            disconnect();
            return;
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
        } catch (EOFException e){
            disconnect();
            return;
        }
        listen();
    }
    private void disconnect() throws IOException {
        connection.close();
        System.out.println("Server disconnected");
        Client.HOST = "";
        Client.PORT = 0;
        SetupServerAddressAndPort();
    }
    private void confirmCommand(String inputCommand) throws InvalidClientExecutionException {
        String command = inputCommand.split(" ")[0];
        if (Objects.equals(command, "download") & inputCommand.split(" ").length < 2){
            throw new InvalidClientExecutionException("Please, enter a filename");
        }
        if (Objects.equals(command, "upload") & inputCommand.split(" ").length < 2){
            throw new InvalidClientExecutionException("Please, enter a filename");
        }
        if (Objects.equals(command, "remove") & inputCommand.split(" ").length < 2){
            throw new InvalidClientExecutionException("Please, enter a filename");
        }
        if (Objects.equals(command, "upload") ){
            if (!(new File(inputCommand.split(" ")[1])).exists())
                throw new InvalidClientExecutionException("Filename " + inputCommand.split(" ")[1] + " doesn't exist");
        }

    }

    private void upload(String inputCommand) throws IOException {
        String filename = inputCommand.split(" ")[1];

        // on upload tout le fichier d'un coup. Cela ne fonctionne pas pour les longs fichiers...
        byte[] bytes = Files.readAllBytes(Paths.get(filename));
        int length = bytes.length;
        System.out.println(length);
        os.writeInt(length);
        os.write(bytes,0,length);
    }
    private void download(String inputCommand) throws IOException {
        String filename = inputCommand.split(" ")[1];
        File file = new File(filename);

        // on lit les données envoyées, correspondant au fichier a télécharger
        int length = is.readInt();
        byte[] buffer  = new byte [length];
        int count = 0;
        while (count<length) {
            count+= is.read(buffer, count, length-count);
        }
        // si le fichier existe déjà, on écrit rien.
        if (!file.createNewFile())
            throw new InvalidClientExecutionException("File" + filename + " already exists...");
        // sinon, on écrit le fichier
        try (FileOutputStream outFile = new FileOutputStream(file.getAbsolutePath())) {
            outFile.write(buffer);
            outFile.flush();
        }
        // on ferme toujours le fichier pour éviter de le garder en mémoire !
    }

    public static void main(String[] args) {
        try{
            Client client = new Client();
            System.out.println("Client connected");
            client.listen();

        } catch (UnknownHostException unknownHostException) {
                unknownHostException.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }catch (NullPointerException e){
            System.out.println("Server disconnected");
            System.exit(0);
        }

    }
}