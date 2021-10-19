package com.devatom.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    static int PORT = 5005;
    private final ServerSocket server;
    private  BufferedReader is;
    private  BufferedWriter os;

    public Server(int port) throws IOException {
        // On initialise le serveur
        server = new ServerSocket(port);

        System.out.println("Server Ready !");
    }

    /**
     * listenConnection est une fonction récursive
     * qui écoutre en continue les connections entrantes et les redirige sur des threads,
     * implémenté ici dans la classe Connection
     */
    public void listenConnection() {
        System.out.println("Waiting for a new connection");
        Socket socket = null;
        try {
            socket = server.accept();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //on démarre un thread
        if(socket != null)
            new Connection(socket).start();
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
