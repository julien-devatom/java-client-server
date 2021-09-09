package com.devatom;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {

    public static String HOST = "localhost";
    public static int PORT = 5555;
    private final BufferedWriter os;
    private final BufferedReader is;
    private final BufferedReader consoleReader;
    private final Socket connection;
    public Client() throws IOException {

        // on se connecte au serveur
        connection = new Socket(Client.HOST, Client.PORT);

        // on initialise les connections avec le serveur
        os = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
        is = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        // on se connecte à la console client
        consoleReader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Connected to the server " + connection.toString());
    }

    public void listen() throws IOException {
        String inputCommand = consoleReader.readLine();
        os.write(inputCommand);
        os.newLine();
        os.flush();
        String response = is.readLine();
        System.out.println(response);
        if(response.equals("bye")) {
            connection.close();
            System.out.println("Disconnected");
            return;
        }
        listen();
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

        }

    }
}