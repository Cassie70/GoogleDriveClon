package org.cassie.cliente;

import java.io.*;
import java.net.*;
import java.util.Objects;

public class ClienteDatagrama {

    public static void main(String[] args) {
        int port = 1234;
        String dir = "127.0.0.1";
        try {
            InetAddress dst = InetAddress.getByName(dir);
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            try(DatagramSocket cl = new DatagramSocket()) {
                while (true){
                    System.out.println("Escribe un mensaje, <Enter> para enviar, \"salir\" para terminar");
                    String msj = br.readLine();
                    if(msj.compareToIgnoreCase("salir") == 0){
                        System.out.println("fin");
                        br.close();
                        cl.close();
                        System.exit(0);
                    }else{
                        sendCommand(cl,dst,port, msj);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (UnknownHostException e) {
            System.err.println("Host invalido");
        }
    }

    private static void sendCommand(DatagramSocket socket, InetAddress address, int port, String command) throws IOException {
        byte[] buffer = command.getBytes();
        DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length, address, port);
        socket.send(requestPacket);

        String[] parts = command.split(" ");
        if(Objects.equals(parts[0], "UPLOAD")){
            if (!handleFileUpload(parts[1]))
                return;
        }
        byte[] responseBuffer = new byte[65535];
        DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
        socket.receive(responsePacket);

        String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
        System.out.println("Respuesta del servidor: \n" + response);
    }

    private static boolean handleFileUpload(String route) {
        try(DatagramSocket tempClient = new DatagramSocket()){

        }catch(IOException e){

        }
        return false;
    }



}



