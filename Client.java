import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import utils.ConsoleColors;

public class Client {

    private static final String HOST_NAME = "localhost";
    private static final int PORT_NUMBER = 8080;
    private static final int PORT_NUMBER_CHANNEL = 8081;
    private static final int BUFFER_SIZE = 8388608;

    private static Socket s;
    private static SocketChannel sc;
    private static String[] fileList;
    private static DataOutputStream out;
    private static DataInputStream in;

    public static void main(String[] args) {

        try {
            s = new Socket(HOST_NAME, PORT_NUMBER);
            SocketAddress socketAddress = new InetSocketAddress("localhost", PORT_NUMBER_CHANNEL);
            sc = SocketChannel.open();
            sc.connect(socketAddress);
            Scanner scan = new Scanner(System.in);
            in = new DataInputStream(s.getInputStream());
            out = new DataOutputStream(s.getOutputStream());

            String status = in.readUTF();
            System.out.println(ConsoleColors.GREEN + status + ConsoleColors.RESET);
            String fileListStr = in.readUTF();
            fileList = initFileList(fileListStr);
            while (true) {
                printFileList();
                System.out.print("Enter input : ");
                int index = scan.nextInt();
                if (index == 0) {
                    s.close();
                    scan.close();
                    break;
                } else if (index <= fileList.length) {
                    out.writeInt(index);
                    System.out.print("Enter downloading method [1] - Normal / [2] - Zero Copy : ");
                    int receiveMethod = scan.nextInt();
                    out.writeInt(receiveMethod);
                    if (receiveMethod == 1) {
                        receiveFile();
                    } else {
                        receiveFileZeroCopy();
                    }
                } else {
                    System.out.println("!! Invalid File Number !!");
                }

                System.out.println(" ---------------------------------");

            }
        } catch (IOException e) {
            System.err.println(ConsoleColors.RED + "Couldn't connect to " +
                    HOST_NAME + ":" + PORT_NUMBER + ConsoleColors.RESET);
            System.exit(1);
        } catch (Exception e) {
            if (e.getMessage().equals("Connection reset")) {
                System.err.println(
                        ConsoleColors.RED + "Connection from " + HOST_NAME + "is reset" + e + ConsoleColors.RESET);
            } else {
                System.out.println(ConsoleColors.RED + e + ConsoleColors.RESET);
            }
        }
    }

    private static String[] initFileList(String fileListStr) {
        return fileListStr.split("/");
    }

    private static void printFileList() {
        System.out.println();
        System.out.println(" --- Select a file to download ---");
        for (int i = 0; i < fileList.length; i++) {
            System.out.println("  [" + (i + 1) + "] - " + fileList[i]);
        }
        System.out.println("  [0] - Exit");
        System.out.println(" ---------------------------------");
    }

    private static void receiveFile() throws Exception {

        System.out.println("Receiving File...");
        long startTime = System.currentTimeMillis();

        String FILE_NAME = in.readUTF();
        long FILE_SIZE = in.readLong();

        BufferedInputStream bis = new BufferedInputStream(in);

        FileOutputStream fos = new FileOutputStream(FILE_NAME);
        byte[] bytes = new byte[BUFFER_SIZE];
        long count = FILE_SIZE;
        while (count > 0) {
            int received = bis.read(bytes);
            count -= received;
            fos.write(bytes, 0, received);
        }

        fos.close();
        long endTime = System.currentTimeMillis();

        System.out.println("File Received [" + FILE_SIZE + " bytes] - Elapsed Time " + (endTime - startTime) + " ms");
    }

    private static void receiveFileZeroCopy() throws Exception {
        System.out.println("Receiving File...");
        long startTime = System.currentTimeMillis();
        FileChannel destination = null;
        String FILE_NAME = in.readUTF();
        long FILE_SIZE = in.readLong();
        FileOutputStream fos = new FileOutputStream(FILE_NAME);
        destination = fos.getChannel();
        long start = 0;
        while (start < FILE_SIZE) {
            long received = destination.transferFrom(sc, start, FILE_SIZE - start);
            System.out.println(received);
            if (received <= 0) {
                break;
            }
            start += received;
        }

        long endTime = System.currentTimeMillis();
        System.out.println("File Received [" + FILE_SIZE + " bytes] - Elapsed Time " + (endTime - startTime) + " ms");
        destination.close();
        fos.close();

    }

}
