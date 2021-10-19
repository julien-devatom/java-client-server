package com.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    static int PORT = 5006; // port du serveur Socket
    private final ServerSocket server; // instance du serveur socket
    private  BufferedReader is; // flux d'entrée communiquant avec les connections entrantes
    private  BufferedWriter os; // flux d'entrée communiquant avec les connections entrantes

    /**
     * Constructeur
     * @param port Port du serveur : (Server.PORT)
     *
     * @throws IOException
     */
    public Server(int port) throws IOException {
        // On initialise le serveur
        server = new ServerSocket(port);

        System.out.println("Server Ready !");
    }

    /**
     * listenConnection est une fonction récursive
     * qui écoutre en continue les connections entrantes et les redirige sur des threads,
     * chaque thread est implémenté ici dans la classe Connection.
     * On peut y sortir uniquement en arretant le serveur
     */
    public void listenConnection() {
        System.out.println("Waiting for a new connection");
        Socket socket;
        try {
            // on attend une connection
            socket = server.accept();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        //on démarre un thread
        if(socket != null)
            new Connection(socket).start();
        // et on se remet à l'écoute de connection
        listenConnection();
    }


    public static void main(String[] args) {
        try{
            Server server = new Server(Server.PORT);
            server.listenConnection();
        } catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

}
