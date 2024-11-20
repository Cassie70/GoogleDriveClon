package org.cassie.servidor;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.spec.RSAOtherPrimeInfo;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

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
                    System.out.println("se ha recibido datagrama desde"+p.getAddress());
                    System.out.println(command);
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
            case "CD" ->
                    CD(folderName);
            case "BACK" ->
                    back();
            case "UPLOAD" ->
                upload(folderName);
            default -> "Comando no reconocido";
        };
    }

    private static String upload(String folderName) {
        boolean completed = false;
        final int port = 1235;
        Map<Integer, byte[]> receivedFragments = new TreeMap<>();
        File fileName = new File(folderName);

        String outputFilePath = currentPath + File.separator+fileName.getName();
        int expectedPacket = 0;
        try (DatagramSocket tempServer = new DatagramSocket(port);
             FileOutputStream fos = new FileOutputStream(outputFilePath)) {

            while (true) {
                byte[] bytes = new byte[65535];
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
                tempServer.receive(packet);


                DataInputStream dis = new DataInputStream(new ByteArrayInputStream(packet.getData()));
                int packetIndex = dis.readInt();
                int windowsSize = dis.readInt();
                int packetSize = dis.readInt();

                if (packetIndex == -1) {
                    // Si el fragmento especial indica el fin de la transmisión
                    completed = true;
                    System.out.println("Todos los paquetes se recibieron.");
                    break;
                }

                // Leer los datos del fragmento
                byte[] fileData = new byte[packetSize];
                dis.readFully(fileData);


                if(packetIndex == expectedPacket ){
                    fos.write(fileData);
                    fos.flush();
                    System.out.println("Fragmento "+expectedPacket +" añadido");
                }else{
                    if (!receivedFragments.containsKey(packetIndex)) {
                        receivedFragments.put(packetIndex, fileData);
                        System.out.println("Fragmento " + packetIndex + " guardado."+" tamaño "+ packetSize);
                    }
                }
                if(receivedFragments.size()<= windowsSize) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    dos.writeInt(packetIndex); // ACK con el índice del fragmento confirmado
                    byte[] ackBytes = baos.toByteArray();

                    DatagramPacket ackPacket = new DatagramPacket(
                            ackBytes, ackBytes.length, packet.getAddress(), packet.getPort()
                    );
                    tempServer.send(ackPacket);
                    System.out.println("ACK enviado para fragmento " + packetIndex);
                    expectedPacket++;
                }else{
                    if(!receivedFragments.isEmpty()){
                        Iterator<Map.Entry<Integer, byte[]>> iterator = receivedFragments.entrySet().iterator();
                        while (iterator.hasNext()) {
                            Map.Entry<Integer, byte[]> entry = iterator.next();
                            fos.write(entry.getValue());
                            iterator.remove();
                            System.out.println("Fragmento "+entry.getKey()+ "añadido");
                        }
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error al iniciar la subida: " + e.getMessage());
            return "error al subir archivo";
        }

        if (completed) {
            return "Subida completada, archivo reconstruido en: " + outputFilePath;
        } else {
            return "Error al subir archivo";
        }
    }


    private static String back() {
        File currentDir = new File(currentPath);
        if(currentDir.getParent() != null){
            currentPath = currentDir.getParentFile().getPath();
        }
        return currentPath;
    }

    private static String CD(String folderName) {
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
        File[] folders = rootDir.listFiles(File::exists);

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
