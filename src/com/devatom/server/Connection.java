package com.devatom.server;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

public class Connection extends Thread{
    public static int number = 0;
    private int currentNumber;
    private final Socket socket;
    private DataInputStream is;
    private DataOutputStream os;
    private Operation executor;

    public Connection(Socket socket)
    {
        this.socket = socket;
        this.currentNumber = ++number;

        try {
            // on initialise le dossier client
            String basePath = (new File("clientdir")).getAbsolutePath();
            this.is = new DataInputStream(socket.getInputStream());
            this.os = new DataOutputStream(socket.getOutputStream());
            this.executor = new Operation(basePath, is, os);
        } catch (IOException e) {
            e.printStackTrace();
        }
        log("Connected, Connection n°" + currentNumber);
    }
    @Override
    public void run(){
        listenFlux();
    }

    private void listenFlux() {
        log("Waiting for a command");
        try{
            String line = is.readUTF();
            try {
                log("Run command " + line);
                String output = runCommand(line);
                System.out.println("Command executed");
                os.writeUTF(output);
            } catch (IOException e) {

                // capture des erreurs de commandes
                os.writeUTF(e.getMessage());
            } catch (InterruptedException e) {
                System.out.println("Interrupted Exception");
                // capture de l'interruption de la connection
                e.printStackTrace();
                disconnect();
                return;
            }
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
            log(e.getMessage());
            disconnect();
            return;
        }
        listenFlux();
    }
    private String runCommand(String cmd) throws IOException, InterruptedException {
        String operation = cmd.split(" ")[0];
        String[] args = Arrays.copyOfRange(cmd.split(" "), 1, cmd.split(" ").length);

        // on vérifie si l'opération est possible
        if (!Arrays.asList(Operation.enabledOperations).contains(operation))
                throw new CommandNotFoundException(operation, Operation.enabledOperations);
        // et on l'execute
        return executor.execute(operation, args);
    }

    private void disconnect(){
        log("Connection closed");
        try {
            os.close();
            is.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void log(String message)
    {
        System.out.println(System.currentTimeMillis() +
                " : From " + socket.toString() +
                "(n°" + currentNumber + "): " + message);
    }
}
