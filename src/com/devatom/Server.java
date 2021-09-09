package com.devatom;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    static int PORT = 5555;
    private final ServerSocket server;
    private Socket connection;
    private  BufferedReader is;
    private  BufferedWriter os;

    public Server(int port) throws IOException {
        // On initialise le serveur
        server = new ServerSocket(port);

        System.out.println("Server OK");
        // on initialise les connections avec le client
    }
    public void listenConnection() throws IOException {
        System.out.println("Waiting for a new connection");
        connection = server.accept();
        System.out.println("New connection, from" + connection.toString());
        // et on ouvre les streams d'écoute et de réponse
        is = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        os = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
        try{
            listenFlux();
        }catch (IOException ignored){
        }finally {
            System.out.println("Deconnection of " + connection.toString());
            connection.close();
            is.close();
            os.close();
        }
        System.out.println("Ready to accept new connection");

    }

    private void listenFlux() throws IOException, NullPointerException {
        System.out.println("Waiting command");
        try{
            String line = is.readLine();
            os.write("Server has recieved the command " + line);
            os.newLine();
            os.flush();

            if(line.equals("q")){
                os.write("bye");
                os.newLine();
                os.flush();
            }
        }catch (NullPointerException | IOException e){
            System.out.println("Connection with" + connection.toString() + " closed");
            connection.close();
            os.close();
            is.close();
            listenConnection();
        }
        listenFlux();
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
