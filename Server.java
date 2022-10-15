import java.net.*;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import utils.ConsoleColors;

import java.io.*;

public class Server {

    public static final int PORT_NUMBER = 8080;
    public static final int PORT_NUMBER_CHANNEL = 8081;

    public static void main(String[] args) throws IOException {

        ServerSocket ss = new ServerSocket(PORT_NUMBER);
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.socket().bind(new InetSocketAddress(PORT_NUMBER_CHANNEL));

        Logger.printLog("\u001B[32mServer created at localhost:" + PORT_NUMBER + "\u001B[0m");

        Socket s = null;
        SocketChannel sc = null;
        int clientNumber = 1;
        boolean isRunning = true;
        while (isRunning) {
            try {
                s = ss.accept();
                sc = ssc.accept();
                Thread t = new ClientHandler(s, s.getInputStream(), s.getOutputStream(), sc, clientNumber++);
                t.start();
            }

            catch (Exception e) {
                s.close();
            }
        }

        ss.close();

    }
}

class ClientHandler extends Thread {

    private String socketAddress;
    private Socket s;
    private SocketChannel sc;
    private InputStream is;
    private OutputStream os;
    private DataOutputStream out;
    private DataInputStream in;
    private int clientNumber;

    private final String FILE_STORAGE = "./resources/";
    private final int BUFFER_SIZE = 8388608;

    File[] fileList;

    ClientHandler(Socket socket, InputStream is, OutputStream os, SocketChannel sc, int clientNumber) {
        s = socket;
        this.is = is;
        this.os = os;
        this.sc = sc;
        this.clientNumber = clientNumber;
        fileList = new File(FILE_STORAGE).listFiles();
    }

    public void run() {

        try {
            socketAddress = s.getInetAddress().toString();
            out = new DataOutputStream(os);
            in = new DataInputStream(is);

            Logger.printLog(ConsoleColors.YELLOW + "Client " + clientNumber + " [" + socketAddress + "]"
                    + ConsoleColors.RESET + " Connected");
            out.writeUTF("Connected to server");
            out.writeUTF(getFileList());

            int index = -1;

            while (true) {
                index = in.readInt();
                if (index <= fileList.length) {
                    int sendMethod = in.readInt();
                    if (sendMethod == 1) {
                        sendFile(fileList[index - 1].getName());
                    } else {
                        sendFileZeroCopy(fileList[index - 1].getName());
                    }
                } else {
                    Logger.printErrorLog("Invalid file index");
                }
            }

        } catch (IOException e) {
            if (e.getMessage() == null || e.getMessage().equals("Connection reset")) {
                Logger.printLog(ConsoleColors.YELLOW + "Client " + clientNumber + " [" + socketAddress + "]"
                        + ConsoleColors.RESET + " Disconnected");
            } else if (e.getMessage().equals("Socket closed")) {
                Logger.printLog("Socket Disconnected");
            } else {
                Logger.printErrorLog("Unhandled Exception > " + e.getMessage());
            }

        } catch (Exception e) {
            Logger.printLog(e);
        }

    }

    private String getFileList() {
        String str = "";
        for (File file : fileList) {
            str += file.getName() + "/";
        }
        return str;
    }

    public void sendFileZeroCopy(String fileName) throws Exception {
        long startTime = System.currentTimeMillis();
        File file = new File(FILE_STORAGE + fileName);
        Logger.printLog(
                "Sending Zero Copy " + fileName + " [" + file.length() + " bytes] to " + ConsoleColors.YELLOW
                        + "Client " + clientNumber + ConsoleColors.RESET);
        out.writeUTF(fileName);
        out.writeLong(file.length());
        FileInputStream fis = new FileInputStream(FILE_STORAGE + fileName);
        FileChannel source = fis.getChannel();
        long totalSent = 0;
        while (totalSent < file.length()) {
            long sent = source.transferTo(totalSent,
                    file.length() - totalSent, sc);
            totalSent += sent;
        }
        long endTime = System.currentTimeMillis();
        Logger.printLog(fileName + " has been sent to " + ConsoleColors.YELLOW + "Client " + clientNumber
                + ConsoleColors.RESET + " - Elapsed Time " + (endTime - startTime) + " ms");
        fis.close();

    }

    private boolean sendFile(String fileName) throws Exception {
        long startTime = System.currentTimeMillis();
        File file = new File(FILE_STORAGE + fileName);
        InputStream fin = new FileInputStream(file);
        out.writeUTF(fileName);
        out.writeLong(file.length());
        Logger.printLog("Sending " + fileName + " [" + file.length() + " bytes] to " + ConsoleColors.YELLOW
                + "Client " + clientNumber + ConsoleColors.RESET);
        byte[] bytes = new byte[BUFFER_SIZE];
        int count = 0;
        while ((count = fin.read(bytes)) > 0) {
            out.write(bytes, 0, count);
        }
        fin.close();
        long endTime = System.currentTimeMillis();
        Logger.printLog(fileName + " has been sent to " + ConsoleColors.YELLOW + "Client " + clientNumber
                + ConsoleColors.RESET + " - Elapsed Time " + (endTime - startTime) + " ms");
        return true;

    }

}

class Logger {
    public static void printLog(Object message) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();

        System.out.println(ConsoleColors.BLUE + "[" + dtf.format(now) + "]" + ": " + ConsoleColors.RESET + message);
    }

    public static void printErrorLog(Object message) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();

        System.out.println(
                ConsoleColors.BLUE + "[" + dtf.format(now) + "]" + ": " + ConsoleColors.RESET + ConsoleColors.RED
                        + message + ConsoleColors.RESET);
    }
}
