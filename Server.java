import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.io.*;

public class Server {

    public static final int PORT_NUMBER = 8080;

    public static void main(String[] args) throws IOException {

        ServerSocket ss = new ServerSocket(PORT_NUMBER);
        Logger.printLog("Server created at localhost:" + PORT_NUMBER);

        Socket s = null;

        boolean isRunning = true;
        while (isRunning) {
            try {
                s = ss.accept();
                Thread t = new ClientHandler(s, s.getInputStream(), s.getOutputStream());
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
    private InputStream is;
    private OutputStream os;
    private DataOutputStream out;
    private DataInputStream in;

    private final String FILE_STORAGE = "./resources/";
    private final int THREAD_NUMBER = 9;

    File[] fileList;

    ClientHandler(Socket socket, InputStream is, OutputStream os) {
        s = socket;
        this.is = is;
        this.os = os;
        fileList = new File(FILE_STORAGE).listFiles();
    }

    class ByteSender extends Thread {

        private byte[] b;

        ByteSender(byte[] b) {
            this.b = b;
        }

        @Override
        public void run() {
            try {
                // out.writeInt(i);
                // System.out.println(new String(b));
                os.write(b);
            } catch (Exception e) {
                Logger.printLog(e);
            }
        }

    }

    private boolean sendFile(String fileName) {
        final int BUFFER_SIZE = 16 * 4096;
        try {
            File file = new File(FILE_STORAGE + fileName);
            InputStream fin = new FileInputStream(file);
            out.writeUTF(fileName);
            out.writeInt((int) file.length());
            Logger.printLog("Sending " + fileName + " [" + file.length() + " bytes] to " + s.getInetAddress());
            byte[] bytes = new byte[BUFFER_SIZE];
            int count = 0;
            while ((count = fin.read(bytes)) > 0) {
                out.write(bytes, 0, count);
            }
            fin.close();
            Logger.printLog(fileName + " sent to " + socketAddress);
            return true;
        } catch (Exception e) {
            Logger.printLog("!! Socket Error !! " + e.getMessage());
            return false;
        }

    }

    private boolean sendFileThreaded(String fileName) {
        try {
            File file = new File(FILE_STORAGE + fileName);
            FileInputStream fin = new FileInputStream(file);
            out.writeUTF(fileName);
            out.writeLong(file.length());
            Logger.printLog("Sending " + fileName + " [" + file.length() + " bytes] to " + s.getInetAddress());
            
            final int FILE_SIZE = (int) file.length();
            final int SLICE_SIZE = FILE_SIZE / THREAD_NUMBER;
            final int LEFT_OVER = FILE_SIZE % THREAD_NUMBER;
            
            byte[] fileBytes = new byte[SLICE_SIZE];

            for (int i = 0; i < THREAD_NUMBER; i++) {
                int read = fin.read(fileBytes);
                byte[] b = new byte[read];
                b = Arrays.copyOf(fileBytes, read);
                Thread bs = new ByteSender(b);
                bs.start();
                bs.sleep(i);
            }
            if (LEFT_OVER != 0) {
                int read = fin.read(fileBytes);
                byte[] b = new byte[read];
                b = Arrays.copyOf(fileBytes, read);
                Thread bs = new ByteSender(b);
                bs.start();                
            }

            fin.close();
            Logger.printLog(fileName + " sent to " + socketAddress);
            return true;
        } catch (Exception e) {
            Logger.printLog("!! Socket Error !! " + e);
            return false;
        }
    }



    public void run() {

        try {
            socketAddress = s.getInetAddress().toString();
            out = new DataOutputStream(os);
            in = new DataInputStream(is);

            Logger.printLog(socketAddress + " Connected");
            out.writeUTF("Connected to server");
            out.writeUTF(getFileList());

            int index = -1;

            // Socket I/O Loop
            while ((index = in.readInt()) != -1) {
                if (index <= fileList.length) {
                    sendFileThreaded(fileList[index - 1].getName());
                } else {
                    Logger.printLog("Invalid file index");
                }
            }

        } catch (IOException e) {
            if (e.getMessage() == null) {
                Logger.printLog(socketAddress + " Disconnected");
            } else if (e.getMessage().equals("Connection reset")) {
                Logger.printLog(socketAddress + " Disconnected");
            } else if (e.getMessage().equals("Socket closed")) {
                Logger.printLog("Socket Disconnected");
            } else {
                Logger.printLog("Unhandled Exception > " + e.getMessage());
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

}

class Logger {
    public static void printLog(Object message) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();

        System.out.println("[" + dtf.format(now) + "]" + ": " + message);
    }
}
