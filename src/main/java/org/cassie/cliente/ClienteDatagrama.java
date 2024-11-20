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
            handleFileUpload(parts[1]);
        }
        byte[] responseBuffer = new byte[65535];
        DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
        socket.receive(responsePacket);

        String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
        System.out.println("Respuesta del servidor: \n" + response);
    }

    private static void handleFileUpload(String route) {
        try (
                InputStream inputStream = new FileInputStream(route);
                DatagramSocket cl = new DatagramSocket()
        ) {
            // Calcular el tamaño total del archivo
            File file = new File(route);
            long fileSize = file.length();
            int maxPacketSize = 1024; // Tamaño máximo del paquete
            int totalPackets = (int) Math.ceil((double) fileSize / (maxPacketSize - 8)); // 8 bytes para metadatos

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            int packageNumber = 1;
            InetAddress dir = InetAddress.getByName("127.0.0.1");

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead); // Agregar datos leídos

                // Verificar si el paquete está listo para ser enviado
                if (baos.size() + 8 >= maxPacketSize || bytesRead < buffer.length) {
                    ByteArrayOutputStream packetStream = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(packetStream);

                    // Escribir metadatos
                    dos.writeInt(totalPackets);   // Total de paquetes
                    dos.writeInt(packageNumber); // Número del paquete
                    dos.writeInt(baos.size());   // Tamaño de los datos

                    // Escribir los datos del archivo
                    dos.write(baos.toByteArray());

                    // Enviar paquete
                    byte[] packetData = packetStream.toByteArray();
                    DatagramPacket packet = new DatagramPacket(packetData, packetData.length, dir, 1235);
                    cl.send(packet);

                    System.out.println("Paquete enviado: " + packageNumber + "/" + totalPackets);

                    // Resetear para el siguiente paquete
                    baos.reset();
                    packageNumber++;
                }
            }

            System.out.println("Transferencia completada.");
        } catch (IOException e) {
            System.err.println("Error durante la transferencia: " + e.getMessage());
            e.printStackTrace();
        }
    }


}



