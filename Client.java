import java.io.*;
import java.net.Socket;
import java.util.Date;
import java.util.Scanner;

public class Client {

    private static final String HOST_NAME = "localhost";
    private static final int PORT_NUMBER = 8080;
    private static final int BUFFER_SIZE = 16 * 4096;

    private static Socket s;
    private static String[] fileList;
    private static DataOutputStream out;
    private static DataInputStream in;

    public static void main(String[] args) {

        try {
            s = new Socket(HOST_NAME, PORT_NUMBER); // สร้างช่องทางการเชื่อมต่อ
            Scanner scan = new Scanner(System.in);
            in = new DataInputStream(s.getInputStream());
            out = new DataOutputStream(s.getOutputStream());

            String status = in.readUTF(); // รับ status มาจากฝั่ง Server 
            System.out.println(status); // แสดงค่า status ว่า "Connected to server"
            String fileListStr = in.readUTF(); // รับ list ของไฟล์ที่ส่งมาจาก Server ซึ่งเป็น String ของชื่อไฟล์ทั้งหมด  ซึ่งแต่ล่ะไฟล์ถูกขั้นด้วย "/"
            fileList = initFileList(fileListStr); // ทำการแปลง String ที่รับมาจากฝั่ง Server ให้แยกเป็น list ของไฟล์
            while (true) { // กระบวนการ การร้องขอไฟล์ที่ต้องการจาก Server
                printFileList(); // แสดงรายชื่อไฟล์ทั้งหมดที่มีตาม list
                System.out.print("Enter input : ");
                int index = scan.nextInt(); // รับค่าทางคีย์บอร์ดจาก User
                if (index == 0) { // กรณีที่ User กรอกเลข 0 เข้ามา จะเป็นการบอกให้จบการเชื่อมต่อ
                    s.close();
                    scan.close();
                    break;
                } else if (index <= fileList.length) { // เงื่อนไขเช็คว่า User กรอกตัวเลขไฟล์ที่ต้องการมาถูกต้องหรือไม่ ถ้าถูกต้องจะทำตามเงื่อนไขนี้
                    out.writeInt(index); // ส้งข้อมูลของไฟล์ที่ต้องการ(index) ไปให้ Server
                    receiveFile(); // เรียกใช้ฟังก์ชัน reiveFile เพื่อรับไฟล์ที่ส่งมาจากฝั่ง Server
                } else { // กรณีที่ User กรอกข้อมูผิดจะแสดง status บอก User ว่ากรอกข้อมูลผิด
                    System.out.println("!! Invalid File Number !!"); 
                }

                System.out.println(" ---------------------------------");

            }
        } catch (IOException e) {
            System.err.println("Couldn't connect to " +
                    HOST_NAME + ":" + PORT_NUMBER);
            System.exit(1);
        } catch (Exception e) {
            if (e.getMessage().equals("Connection reset")) {
                System.err.println("Connection from " + HOST_NAME + "is reset" + e);
            } else {
                System.out.println(e);
            }
        }
    }

    private static String[] initFileList(String fileListStr) {  // ทำการนำ String ของ list ไฟล์ที่ได้ มาแยกเป็นชื่อไฟล์แต่ล่ะไฟล์โดยการถอด "/" ออก
        return fileListStr.split("/");
    }

    private static void printFileList() { // จัด Format ของ File List
        System.out.println(); 
        System.out.println(" --- Select a file to download ---");
        for (int i = 0; i < fileList.length; i++) {
            System.out.println("  [" + (i + 1) + "] - " + fileList[i]);
        }
        System.out.println("  [0] - Exit");
        System.out.println(" ---------------------------------");
    }

    private static void receiveFile() throws Exception { // ฟังก์ชันการรับไฟล์ที่ถูกส่งมาจาก Server

        Date date = new Date();
        long startTime = date.getTime(); // เวลาทีเริ่มต้นขั้นตอนการรับไฟล์จากฝั่ง Server

        String FILE_NAME = in.readUTF(); // อ่านชื่อไฟล์ที่ถูกส่งมาจากฝั่ง Server
        long FILE_SIZE = in.readLong(); // อ่านขนาดของไฟล์ที่ถูกส่งมาจากฝัั่ง Server

        BufferedInputStream bis = new BufferedInputStream(in);

        System.out.println("Receiving File..."); // แสดง status ว่ากำลังทำการรับไฟล์อยู่
        FileOutputStream fos = new FileOutputStream(FILE_NAME); // ประกาศตัวแปร(new Objet)ที่จะอ่านข้อมูลไฟล์ที่อยู่ใน RAM เพื่อนำไปบันทึกไว้ใน Disk
        byte[] bytes = new byte[BUFFER_SIZE];
        long count = FILE_SIZE; // ขนาดของไฟล์ที่ต้องอ่านและเขียนลงใน Disk
        while (count > 0) { // กระบวนการอ่านไฟล์และเขียนลงใน Disk  โดยการ Loop 
            int recieved = bis.read(bytes); // อ่านและเขียนไฟล์ลงใน bytes โดยค่าจำนวน bytes ที่อ่านได้จะถูกนำไปเก็บไว้ในตัวแปร recieved 
            count -= recieved; // บอกว่าเหลือ bytes ที่ต้องอ่านอีกจำนวนเท่าไหร่
            fos.write(bytes, 0, recieved);  // ทำการเขียนข่อมูลลงใน Disk
        }

        fos.close(); // จบกระบวนการ การรับไฟล์
        date = new Date();
        long endTime = date.getTime(); // เวลาที่จบกระบวนการทำงาน

        System.out.println("File Recieved [" + FILE_SIZE + " bytes] - Elasped Time " + (endTime - startTime) + " ms"); // แสดงเวลาที่ใช้ในการรับ-ส่งไฟล์ (เวลาจบ-เวลาเริ่ม)
    }

}
