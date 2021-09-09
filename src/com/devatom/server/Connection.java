package com.devatom.server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Connection extends Thread{
    private int number;
    private final Socket socket;
    private BufferedReader is;
    private BufferedWriter os;

    public Connection(Socket socket, int number)
    {
        this.socket = socket;
        this.number = number;
        try {
            this.is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.os = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        log("Connected, Connection n°" + number);
    }
    @Override
    public void run(){
        try {
            listenFlux();
        } catch (IOException e) {
            System.exit(0);
        }

    }
    private void listenFlux() throws IOException {
        try{
            String line = is.readLine();
            try {
                os.write(runCommand(line));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            os.newLine();
            os.flush();
            if(line.equals("q")){
                os.write("bye");
                os.newLine();
                os.flush();
                disconnect();
            }
        }catch (NullPointerException | IOException e){
            disconnect();
        }
        listenFlux();
    }
    private String runCommand(String cmd) throws IOException, InterruptedException {
        Runtime run = Runtime.getRuntime();
        Process pr = run.exec(cmd);
        pr.waitFor();
        BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        String line = "";
        List<String> lines = new ArrayList<String>();
        while ((line=buf.readLine())!=null) {
            lines.add(line);
            System.out.println(line);
        }
        return String.join("\n", lines);
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
        System.exit(0);
    }
    private void log(String message)
    {
        System.out.println(System.currentTimeMillis() +
                " : From " + socket.toString() +
                "(n°" + number + "): " + message);
    }
}
