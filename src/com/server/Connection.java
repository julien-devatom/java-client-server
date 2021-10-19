package com.server;


import java.io.*;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

/**
 * Une instance de Connection représente un lien socket entre le serveur et un client spécifique. Ce lien est executé dans un thread,
 * de telle sorte a libérer le thread principal pour l'écoute des connections
 */
public class Connection extends Thread{
    public static int number = 0; // nombre de threads contenant des connections
    private final Socket socket;
    private DataInputStream is;
    private DataOutputStream os;
    private Operation executor;

    public Connection(Socket socket)
    {
        this.socket = socket;
        ++number;

        try {
            // on initialise le dossier client ( un seul dossiert pour tous les clients)
            String basePath = (new File("clientdir")).getAbsolutePath();
            this.is = new DataInputStream(socket.getInputStream());
            this.os = new DataOutputStream(socket.getOutputStream());

            // Operation est utilisé pour effectuer les opérations courantes sur le serveur
            this.executor = new Operation(basePath, is, os);
        } catch (IOException e) {
            e.printStackTrace();
        }
        log("Connected, There is currently " + number + " clients connected");
    }
    @Override
    public void run(){
        listenFlux();
    }

    private void listenFlux() {
        log("Waiting for a command");
        try{
            // on attend de recevoir un instruction du client
            String line = is.readUTF();
            try {
                // on execute la commande
                log("Run command " + line);
                String output = runCommand(line);
                System.out.println("Command executed");
                os.writeUTF(output);
            } catch (IOException e) {
                // capture des erreurs de flux
                os.writeUTF(e.getMessage());
            } catch (InterruptedException e) {
                System.out.println("Interrupted Exception");
                // capture de l'interruption de la connection
                e.printStackTrace();
                disconnect();
                return;
            }
            // la commande q permet de quitter la connection de manière non brute
            if(line.equals("q")){
                os.writeUTF("bye");
                os.writeUTF("EOF");
                os.flush();
                disconnect();
                return;
            }
            os.writeUTF("EOF");
            os.flush();
        }catch (NullPointerException | IOException e){
            // on ne retrouve plus le client
            log(e.getMessage());
            disconnect();
            return;
        }
        listenFlux();
    }

    /**
     * Cette methode execute la commande demandé, si celle-ci est possible.
     * @param cmd : commande a executer : contient l'instruction et les eventuels arguments
     * @return le message a envoyer au client
     * @throws IOException
     * @throws InterruptedException
     */
    private String runCommand(String cmd) throws IOException, InterruptedException {
        String operation = cmd.split(" ")[0];
        String[] args = Arrays.copyOfRange(cmd.split(" "), 1, cmd.split(" ").length);

        // on vérifie si l'opération est possible
        if (!Arrays.asList(Operation.enabledOperations).contains(operation))
                throw new CommandNotFoundException(operation, Operation.enabledOperations);
        // et on l'execute
        return executor.execute(operation, args);
    }

    /**
     * Déconnecte le client, sans crasher le serveur
     */
    private void disconnect(){
        log("Connection closed");
        try {
            os.close();
            is.close();
            socket.close();
            --number;
            System.out.println("There is now " + number + " connections");
            // et on coupe le thread
            this.interrupt();
        } catch (IOException e) {
            e.printStackTrace();
            this.interrupt();
        }
    }

    /**
     * Formattage des messages en console du serveur, au format
     * [Adresse IP client : Port client // Date et Heure (min, sec)] : <message>
     * @param message : commande ou information à afficher en console
     */
    private void log(String message)
    {
        if(message == null) return;
        String ip_client = socket.getRemoteSocketAddress().toString().split("/")[1];
        LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
        System.out.println("[" + ip_client + " // " + LocalDate.now() + " @ " + time + "] : " + message);
    }
}
