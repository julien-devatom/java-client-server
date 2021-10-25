package com.server;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    static int PORT = 0; // port du serveur Socket
    public static String HOST = ""; // IP du serveur
    private final ServerSocket server; // instance du serveur socket
    private  BufferedReader is; // flux d'entree communiquant avec les connections entrantes
    private  BufferedWriter os; // flux d'entree communiquant avec les connections entrantes
    private static final BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in)); // flux pour lire les commandes sur le serveur

    /**
     * Permet d'ivalider l'ip HOST en tant que IPv4
     *
     * @return true si l'ip est Invalide
     */
    public static boolean isIPInvalid() {
        // IPv4 pattern
        String PATTERN = "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$";
        return !Server.HOST.matches(PATTERN);
    }

    /**
     * Constructeur
     *
     * @throws IOException
     */
    public Server() throws IOException {
        // On initialise le serveur
        SetupServerAddressAndPort();
        server = new ServerSocket();
        server.setReuseAddress(true);
        InetAddress serverIP = InetAddress.getByName(Server.HOST);
        server.bind(new InetSocketAddress(serverIP, Server.PORT));
        System.out.println("Server Ready !");
    }

    /**
     * Permet a l'usager d'entrer l'IP du serveur et le port
     * L'ip doit etre une IPv4, et le port dans le range [5002, 5049]
     * <p>
     * Le client doit entrer une IP valide avant de choisir son port
     *
     * @throws IOException Renvoye si il y a une erreur avec la console client
     */
    private void SetupServerAddressAndPort() throws IOException {
        // validation de l'ip
        while (Server.isIPInvalid()) {
            System.out.println("Please enter the IP address of the server (XXX.XXX.XXX.XXX)");
            System.out.print("IP address = ");
            Server.HOST = consoleReader.readLine();
            if (Server.isIPInvalid())
                System.out.println(Server.HOST + " is not a valid IPv4...");
        }
        // validation du port
        while (Server.PORT < 5002 || Server.PORT > 5049) {
            System.out.println("Please select the port of the server (between 5002 et 5049) ");
            try {
                System.out.print("Port = ");
                Server.PORT = Integer.parseInt(consoleReader.readLine());
                if (Server.PORT < 5002 || Server.PORT > 5049)
                    System.out.println(Server.PORT + " is not a valid port");
            } catch (NumberFormatException e) {
                // l'usager a entre des lettres...
                System.out.println("Please enter a number..");
            }
        }
    }

    /**
     * listenConnection est une fonction recursive
     * qui ecoute en continue les connections entrantes et les redirige sur des threads,
     * chaque thread est implemente ici dans la classe Connection.
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
        //on demarre un thread
        if(socket != null)
            new Connection(socket).start();
        // et on se remet a l'ecoute de connection
        listenConnection();
    }


    public static void main(String[] args) {
        try{
            Server server = new Server();
            server.listenConnection();
        } catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

}