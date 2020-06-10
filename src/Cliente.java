import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.*;
import java.sql.Array;
import java.sql.Time;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Cliente {

    public static final String ENDCONNECTION = "Servidor.fim\n";

    public static final String UDPSTART = "Servidor.udp\n";

    public static class TCPConnection implements Runnable{

        String hostname;
        int portNumber;
        Socket socket;
        String textToSend = "Connected!";
        BufferedReader br;
        String recieved = null;

        TCPConnection(String host, int port) {
            this.hostname = host;
            this.portNumber = port;
        }

        public void open() throws IOException {
            socket = new Socket(hostname, portNumber);
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        public void send(String s) throws IOException {
            PrintStream ps = new PrintStream(socket.getOutputStream());
            ps.println(s);
        }

        public String recieve() throws IOException, InterruptedException {
            String line = null;
            String text = "";
            while (!br.ready()) {
            }
            while (br.ready()) {
                line = br.readLine();
                text += line + "\n";
            }
            return text;
        }

        public void close() throws IOException {
            if (socket != null) {
                socket.close();
            }
        }

        public void runRec() throws IOException, InterruptedException {
            recieved = this.recieve();
        }

        public void run() {
            try {
                this.send(textToSend);
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public static class UDPConnection implements Runnable {
        private String hostname;
        private DatagramSocket socket;
        private InetAddress address;
        private int port;

        public UDPConnection(String address, int port) throws SocketException, UnknownHostException {
            socket = new DatagramSocket();
            this.hostname = address;
            this.address = InetAddress.getByName(address);
            this.port = port;
            socket = new DatagramSocket(null);
            InetSocketAddress sockAd = new InetSocketAddress(hostname, port);
            socket.bind(sockAd);
        }

        public void sendEcho(String msg) throws IOException {
            byte[] buf = msg.getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, this.port);
            socket.send(packet);
        }

        public String recieveEcho() throws IOException {
            byte[] recBuf = new byte[256];
            Arrays.fill(recBuf, (byte) 0);
            DatagramPacket packet = new DatagramPacket(recBuf, recBuf.length);
            socket.receive(packet);
            return new String(packet.getData(), 0, packet.getLength());
        }

        public void close() {
            socket.close();
        }

        public void run() {
            try {
                while(true) {
                    String messageRec = this.recieveEcho();
                    System.out.println();
                    System.out.println(messageRec);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void menu() throws IOException {
        System.out.println("MENU CLIENTE\n");
        System.out.println("0 - Menu Inicial");
        System.out.println("1 - Listar utilizadores online");
        System.out.println("2 - Enviar mensagem a um utilizador");
        System.out.println("3 - Enviar mensagem a todos os utilizadores");
        System.out.println("4 - Lista branca de utilizadores");
        System.out.println("5 - Lista negra de utilizadores");
        System.out.println("99 - Sair");
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        UDPConnection ligUDP = new UDPConnection("localhost", 9031);
        TCPConnection ligTCP = new TCPConnection("localhost", 6500);
        Thread udpThread = new Thread(ligUDP);
        //udpThread.start();
        boolean exit = false;
        ligTCP.open();
        menu();
        udpThread.start();
        while(!exit){
            System.out.println();
            System.out.print("Opção? ");
            BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
            String s = bufferRead.readLine();
            if ("0".equals(s)) { //mostrar menu, continuar
                menu();
                continue;
            }
            else if ("99".equals(s)) {
            }
            else {
                ligTCP.textToSend = s;
                ligTCP.run();
                ligTCP.runRec();
                if ("2".equals(s) && ligTCP.recieved.equals(UDPSTART)) {
                    System.out.println();
                    System.out.print("Utilizador? ");
                    String destinatario = bufferRead.readLine();
                    System.out.println();
                    System.out.print("Mensagem? ");
                    String mensagem = bufferRead.readLine();
                    String toSend = mensagem + "|" + destinatario;
                    //ligUDP.sendEcho(toSend);
                    ligTCP.textToSend = toSend;
                    ligTCP.run();
                }
                else if("3".equals(s)&& ligTCP.recieved.equals(UDPSTART)){
                    System.out.println();
                    System.out.print("Mensagem? ");
                    String mensagem = bufferRead.readLine();
                    String toSend = mensagem + "|" + "all";
                    ligTCP.textToSend = toSend;
                    ligTCP.run();
                    //ligUDP.sendEcho(toSend);
                }
                else if (!ligTCP.recieved.equals(UDPSTART) && (("2".equals(s) || "3".equals(s)))) {
                    System.out.println("There was a failrule trying to start the UDP client");
                }
            }
            if (ENDCONNECTION.equals(ligTCP.recieved) || "99".equals(s)) { //server-side end connection
                ligTCP.close();
                udpThread.stop(); //necessario, senao rebenta
                ligUDP.close();
                System.out.println("A sair");
                System.out.println("Cliente desconectado...");
                exit = true;
            }
            else if (UDPSTART.equals(ligTCP.recieved)) {
                continue;
            }
            else {
                if (ligTCP.recieved != null) {
                    System.out.print(ligTCP.recieved);
                    ligTCP.recieved = "";
                }
            }
        }
    }
}



