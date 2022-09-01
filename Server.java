import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.io.*;

public class Server {

    public static final int PORT_NUMBER = 8080;

    public static void main(String[] args) throws IOException {

        //เปิด Server เพื่อเตรียมการเชื่อมต่อ
        ServerSocket ss = new ServerSocket(PORT_NUMBER);
        Logger.printLog("Server created at localhost:" + PORT_NUMBER);

        //รอคำร้องขอการเชื่อมต่อจากฝั่ง Client
        Socket s = null; 
        int clientNumber = 1; //หมายเลข Client ที่มาทำการเชื่อมต่อ
        boolean isRunning = true;
        while (isRunning) {
            try {
                s = ss.accept(); //สร้างช่องทางการเชื่อมต่อ
                Thread t = new ClientHandler(s, s.getInputStream(), s.getOutputStream(), clientNumber++); //สร้าง Thread
                t.start(); // สั่งให้ Thread เริ่มทำงาน
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
    private int clientNumber;

    private final String FILE_STORAGE = "./resources/"; // โฟลเดอร์ที่ใช้เก็บไฟล์ในฝั่ง Server
    private final int THREAD_NUMBER = 9;
    private final int BUFFER_SIZE = 16 * 4096;

    File[] fileList; 

    ClientHandler(Socket socket, InputStream is, OutputStream os, int clientNumber) {
        s = socket;
        this.is = is;
        this.os = os;
        this.clientNumber = clientNumber;
        fileList = new File(FILE_STORAGE).listFiles(); // methode ที่บอกว่าในว่ามีไฟล์อะไรบ้างที่พร้อมให้ใช้งาน
    }

    public void run() {

        try {
            socketAddress = s.getInetAddress().toString(); 
            out = new DataOutputStream(os);
            in = new DataInputStream(is);

            Logger.printLog("Client " + clientNumber + " [" + socketAddress + "] Connected"); // แสดง(ฝั่งServer) ว่ามีการเชื่อมต่อของ Client ตัวไหน
            out.writeUTF("Connected to server"); //ส่งข้อความไปแสดงผลบนฝั่งของ Client
            out.writeUTF(getFileList()); // ส่ง list ของไฟล์ไปแสดงผลบนฝั่ง Client

            int index = -1;

            // Socket I/O Loop 
            // กระบวนการส่งไฟล์ไปให้ Client
            while (true) {  
                index = in.readInt(); // รับค่า index จากฝั่ง Client ว่าต้องการไฟล์ไหนใน Server
                if (index <= fileList.length) { // กระบวนการส่งไฟล์ไปตามที่ Client ร้องขอ
                    sendFile(fileList[index - 1].getName());
                } else {
                    Logger.printLog("Invalid file index"); // กรณีที่ไม่มีไฟล์ที่ Client ร้องขอ
                }
            }

        } catch (IOException e) {
            if (e.getMessage() == null || e.getMessage().equals("Connection reset")) {
                Logger.printLog("Client " + clientNumber + " [" + socketAddress + "] Disconnected");
            } else if (e.getMessage().equals("Socket closed")) {
                Logger.printLog("Socket Disconnected");
            } else {
                Logger.printLog("Unhandled Exception > " + e.getMessage());
            }

        } catch (Exception e) {
            Logger.printLog(e);
        }

    }

    private String getFileList() { // ฟังก์ชันเพื่อนำไปใช้แสดงว่ามีไฟล์อะไรบ้างโดยใช้เทคนิคการต่อ String โดยมี "/" เป็นตัวขั้นแต่ล่ะไฟล์
        String str = "";
        for (File file : fileList) {
            str += file.getName() + "/";
        }
        return str;
    }

    class ByteSender extends Thread { // (ข้ามได้)กระบวนการที่จะนำไปใช้ต่อสำหรับการส่งไฟล์แบบ Multi-Thread
 
        private byte[] b;

        ByteSender(byte[] b) {
            this.b = b;
        }

        @Override
        public void run() {
            try {
                out.write(b);
                b = null;
            } catch (Exception e) {
                Logger.printLog(e);
            }
        }

    }

    private boolean sendFile(String fileName) { // การส่งไฟล์แบบ 1 Thread
        try {
            File file = new File(FILE_STORAGE + fileName); // เลือกไฟล์ที่ต้องการ
            InputStream fin = new FileInputStream(file); //อ่านไฟล์จาก Disk มาไว้ใน RAM
            out.writeUTF(fileName); // ส่งชื่อไฟล์ไปให้ฝั่ง Client
            out.writeLong(file.length()); // ส่งขนาดของไฟล์ไปให้ฝั่ง Client
            Logger.printLog("Sending " + fileName + " [" + file.length() + " bytes] to Client " + clientNumber); // แสดงสถานะการส่ง (ฝั่ง Server)
            byte[] bytes = new byte[BUFFER_SIZE];
            int count = 0;
            while ((count = fin.read(bytes)) > 0) { 
                out.write(bytes, 0, count);
                // อ่านไฟล์ไปเก็บไว้ใน bytes  หลักจากนั้นจะได้ขนาดจำนวนของ bytes ที่อ่านได้กลับมา เพื่อนำไปเก็บไว้ใน count 
                // count มีเพื่อบอกว่าอ่าน bytes ไปได้จำนวนเท่าไหร่
                // หลังจากนั้นส่งข้อมูล bytes ที่อ่านได้ไปให้ฝั่ง Client เป็น Loop วนไปเรื่อยๆ จนกว่าจะอ่านถึง Bytes สุดท้ายของไฟล์
            }
            fin.close(); // จบกระบวนการ การส่งไฟล์ไปให้ Client
            Logger.printLog(fileName + " has been sent to Client " + clientNumber); // แสดงสถานะว่าส่งเสร็จแล้ว (ฝั่ง Server)
            return true;
        } catch (Exception e) {
            Logger.printLog("!! Socket Error !! " + e.getMessage());
            return false;
        }

    }

    private boolean sendFileThreaded(String fileName) { // การส่งแแบบ Multi-Thread
        try {
            File file = new File(FILE_STORAGE + fileName);
            FileInputStream fin = new FileInputStream(file);
            out.writeUTF(fileName);
            out.writeLong(file.length());
            Logger.printLog("Sending " + fileName + " [" + file.length() + " bytes] to Client " + clientNumber);

            final long FILE_SIZE = file.length();
            final long SLICE_SIZE = FILE_SIZE / THREAD_NUMBER;
            final long LEFT_OVER = FILE_SIZE % THREAD_NUMBER;

            byte[] fileBytes = new byte[(int) SLICE_SIZE];

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
            Logger.printLog(fileName + " has been sent to Client " + clientNumber);
            return true;
        } catch (Exception e) {
            Logger.printLog("!! Socket Error !! " + e);
            return false;
        }
    }

}

class Logger { // ฟังก์ชันที่ใช้จัดการ format เวลา
    public static void printLog(Object message) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();

        System.out.println("[" + dtf.format(now) + "]" + ": " + message);
    }
}
