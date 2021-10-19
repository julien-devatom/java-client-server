package com.devatom.client;

import com.devatom.server.InvalidCommandExecutionException;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class Client {

    public static String HOST = "localhost";
    public static int PORT = 5005;
    private  DataOutputStream os;
    private  DataInputStream is;
    private final BufferedReader consoleReader;
    private  Socket connection;
    public Client() {

        // on se connecte au serveur
        try {
            connection = new Socket(Client.HOST, Client.PORT);
            // on initialise les connections avec le serveur
            os = new DataOutputStream(connection.getOutputStream());
            is = new DataInputStream(connection.getInputStream());
        } catch (ConnectException e)
        {
            System.out.println("Server" + Client.HOST + ":" + Client.PORT + " Unreachable");
            System.exit(1);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // on se connecte à la console client
        consoleReader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Connected to the server " + connection.toString());
    }

    public void listen() throws IOException {
        System.out.println("Enter a command");
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

        String response;
        try {
            while (!Objects.equals(response = is.readUTF(), "EOF")) {
                System.out.println(response);
                if (response.equals("bye")) {
                    connection.close();
                    System.out.println("Disconnected");
                    return;
                }
            }
        } catch (EOFException e){
            System.out.println("Server disconnected..");
            connection.close();
            return;
        }
        listen();
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