package org.cassie.servidor;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class ServidorDatagrama {

    private static final String ROOT = "carpetas";
    public static String currentPath = ROOT;

    public static void main(String[] args) {

        File rootDir = new File(ROOT);
        if (!rootDir.exists()) {
            rootDir.mkdir();
        }

        int port = 1234;
        printCurrentPath();
        String command;

        try(DatagramSocket s = new DatagramSocket(port)){
            s.setReuseAddress(true);
            System.out.println("Servidor iniciado... esperando datagramas");

            try{
                while(true){
                    byte[] b = new byte[65535];
                    DatagramPacket p = new DatagramPacket(b,b.length);
                    s.receive(p);
                    command = new String(p.getData(),0,p.getLength());
                    System.out.println("se ha recibidi datagrama desde"+p.getAddress());

                    String response = handlerCommand(command);
                    byte[] responseBytes = response.getBytes();
                    InetAddress clientAddress = p.getAddress();
                    int clientPort = p.getPort();

                    DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length, clientAddress, clientPort);
                    s.send(responsePacket);
                }
            }catch (IOException e){
                System.err.println("Error durante la recepción del datagraama:"+e.getMessage());
            }
        }catch (SocketException e){
            System.err.println("Error al iniciar al servidor: "+e.getMessage());
        }
    }

    private static String handlerCommand(String command) {
        String []instructions = command.split(" ");
        command = instructions[0];
        String folderName = instructions.length > 1 ? instructions[1] : null;

        return switch (command) {
            case "CREATE" ->
                    createFolder(folderName) ? "Folder Creado: " + folderName : "Folder NO Creado: " + folderName;
            case "DELETE" ->
                    deleteFolder(folderName) ? "Folder Borrado: " + folderName : "Folder NO borrado: " + folderName;
            case "LIST" ->
                    listFolders();
            case "DIR" ->
                    dir(folderName);
            case "BACK" ->
                    back();
            default -> "Comando no reconocido";
        };
    }

    private static String back() {
        File currentDir = new File(currentPath);
        if(currentDir.getParent() != null){
            currentPath = currentDir.getParentFile().getPath();
        }
        return currentPath;
    }

    private static String dir(String folderName) {
        File file = new File(currentPath + File.separator+folderName);
        if(file.exists() && file.isDirectory()){
            currentPath = file.getPath();
        }
        return currentPath;
    }

    private static boolean createFolder(String folderName) {
        File folder = new File(currentPath + File.separator+folderName );
        return folder.mkdir();
    }
    private static boolean deleteFolder(String folderName) {
        File folder = new File(currentPath + File.separator+folderName );
        return folder.delete();
    }
    private static String listFolders() {
        File rootDir = new File(currentPath);
        StringBuilder folderList = new StringBuilder();
        File[] folders = rootDir.listFiles(File::isDirectory);

        if (folders != null && folders.length > 0) {
            for (File folder : folders) {
                folderList.append(folder.getName()).append("\n");
            }
        } else {
            folderList.append("No hay carpetas disponibles.");
        }

        return folderList.toString();
    }

    private static void printCurrentPath(){
        System.out.println(currentPath);
    }
}
