package org.cassie.cliente;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ClienteDatagrama {

    static String dir = "127.0.0.1";
    public static void main(String[] args) {
        int port = 1234;
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
            if (parts.length == 1)
                return;
            if (!handleFileUpload(parts[1], socket, address))
                return;
        }
        byte[] responseBuffer = new byte[65535];
        DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
        socket.receive(responsePacket);

        String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
        System.out.println("Respuesta del servidor: \n" + response);
    }

    private static boolean handleFileUpload(String route, DatagramSocket tempClient, InetAddress address) {
        final int port = 1235;
        final int windowSize = 3;
        final int bufferSize = 1024; // Tamaño total del datagrama
        int firstWindowIndex = 0; // Primer índice de la ventana actual
        Map<Integer, DatagramPacket> packets = new HashMap<>(); // Paquetes pendientes de ACK

        try (FileInputStream fis = new FileInputStream(route)) {
            byte[] fileBuffer = new byte[bufferSize - 12]; // Reservar espacio para el encabezado
            int bytesRead;
            int fragmentId = 0;

            while ((bytesRead = fis.read(fileBuffer)) != -1 || !packets.isEmpty()) {
                while (packets.size() < windowSize && bytesRead != -1) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);

                    dos.writeInt(fragmentId);
                    dos.writeInt(windowSize);
                    dos.writeInt(bytesRead);
                    dos.write(fileBuffer, 0, bytesRead);

                    byte[] buffer = baos.toByteArray();
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
                    packets.put(fragmentId, packet);

                    tempClient.send(packet);
                    System.out.println("Fragmento " + fragmentId + " enviado con " + bytesRead + " bytes.");
                    fragmentId++;
                    bytesRead = fis.read(fileBuffer);
                }

                try {
                    tempClient.setSoTimeout(1000);
                    byte[] ackBuffer = new byte[bufferSize - 12];
                    DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);

                    while (true) {
                        tempClient.receive(ackPacket);
                        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(ackPacket.getData()));

                        int ackId = dis.readInt();
                        System.out.println("ACK recibido para fragmento " + ackId);

                        if (packets.containsKey(ackId)) {
                            packets.remove(ackId);
                            firstWindowIndex++;
                        }

                        if (packets.isEmpty() && bytesRead == -1) {
                            break;
                        }
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("Timeout alcanzado, retransmitiendo paquetes...");
                    for (DatagramPacket packet : packets.values()) {
                        tempClient.send(packet);
                    }
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(-1);
            dos.writeInt(0);
            dos.writeInt(0);

            byte[] endPacket = baos.toByteArray();
            DatagramPacket endDatagram = new DatagramPacket(endPacket, endPacket.length, address, port);
            tempClient.send(endDatagram);

            System.out.println("Paquete de finalización enviado.");
            return true;

        } catch (IOException e) {
            System.err.println("Error al enviar el archivo: " + e.getMessage());
        }
        return false;
    }

}



