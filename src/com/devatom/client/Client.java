package com.devatom.client;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
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

        os.writeUTF(inputCommand);
        if (Objects.equals(inputCommand.split(" ")[0], "upload")){
            upload(inputCommand);
        }
        System.out.println("Waiting for a Server response...");
        String response = "";
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

    private void upload(String inputCommand) throws IOException {
        String filename = inputCommand.split(" ")[1];
        try {
            FileInputStream fileContent = new FileInputStream(filename);
            byte[] bytes = new byte[16*1024];

            int count;
            while ((count = fileContent.read(bytes)) > 0) {
                System.out.print("Write " + count + " bytes\r");
                os.write(bytes, 0, count);
            }

            fileContent.close();
        } catch (FileNotFoundException ex) {
            System.out.println("ERROR : file " + filename + " does not exist");
        }
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