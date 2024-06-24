import java.io.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Date;
import java.net.Socket;

public class Client {

    public static void main(String[] args) throws IOException {
        Date d = new Date();
        long localTime = d.getTime();
        long syncTimestamp1, syncTimestamp2,syncTimestamp3,syncTimestamp4;

        Clock clock = new Clock(new Date().getTime());
        clock.start();

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        Socket socket = new Socket("127.0.0.1", 9090);
        //BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        //PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

        Msg msg1,msg2,msg3,msg4,msg5;
        Date date=new Date();

        //oos.writeObject(new Msg());
        //oos.flush();
        //oos.defaultWriteObject();
        try {

            try {
                while (true) {
                    System.out.println("Command?");
                    System.out.println(NetworkDiagnostics.ttl("google.com"));
                    System.out.println(NetworkDiagnostics.ttl("fsu.edu"));
                    String cmd=br.readLine();
                    if(cmd.compareToIgnoreCase("St")==0) {

                        msg1 = new Msg();
                        msg1.setMsg_type("Ready");
                        oos.writeObject(msg1);
                        //r.setReq_type("Time");

                        msg2 = (Msg) ois.readObject();

                        if ((msg2.getMsg_type()).contains("Sync") != true ) {
                            System.out.println("Error when Sync packet is expected");
                            break;
                        }
                        msg3 = (Msg) ois.readObject();
                        syncTimestamp2 = clock.getTime();
                        syncTimestamp1 = msg3.getTime();

                        msg4=new Msg();
                        msg4.setMsg_type("DelayReq");

                        syncTimestamp3 = clock.getTime();
                        oos.writeObject(msg4);

                        msg5 = (Msg) ois.readObject();
                        syncTimestamp4 = msg5.getTime();

                        long delta = (syncTimestamp1-syncTimestamp2) + (syncTimestamp4 - syncTimestamp3) / 2;
                        long oldClock = clock.getTime();
                        long newClock = clock.syncTime(delta);

                        System.out.println("Oldtime:" + oldClock + "  Newtime:" + newClock + " dif:" + delta);

                        if (msg2.getMsg_type().contains("Ack_End")){
                            System.out.println("Clientclock:" + msg2.getTime() + "  Serverclock:" + newClock + " dif:" + delta);
                            break;}
                    }}
            } finally {
                socket.close();
                System.exit(0);
            }
        } catch (Exception e) {
        }
    }
}
//////////////////////////////////////////////////
class Clock extends Thread {
    private long cTime;
    private Object timeLock = new Object();

    public Clock(long cT) {
        this.cTime = cT;
    }

    long getTime() {
        synchronized (timeLock) {
            return cTime;
        }
    }

    void setTime(long t) {
        synchronized (timeLock) {
            cTime = t;
        }
        return;
    }
    long syncTime(long dt){
        synchronized (timeLock) {
            cTime += dt;
            return cTime;
        }
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(10);
                synchronized (timeLock) {
                    cTime += 10;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}

class NetworkDiagnostics {
    private final String os = System.getProperty("os.name").toLowerCase();

    /* public String traceRoute(InetAddress address) {
         String route = "";
         try {
             Process traceRt;
             if (os.contains("win")) traceRt = Runtime.getRuntime().exec("tracert " + address.getHostAddress());
             else traceRt = Runtime.getRuntime().exec("traceroute " + address.getHostAddress());

             // read the output from the command
             route = convertStreamToString(traceRt.getInputStream());

             // read any errors from the attempted command
             String errors = convertStreamToString(traceRt.getErrorStream());
             if (errors != "") LOGGER.error(errors);
         } catch (IOException e) {
             LOGGER.error("error while performing trace route command", e);
         }

         return route;
     }*/
    public static String ttl(String ip) {
        // TODO Auto-generated method stub
        int i=0;
        String pingResult = "";
        String pingCmd = "ping -c 1 " + ip;
        try {
            Runtime r = Runtime.getRuntime();
            Process p = r.exec(pingCmd);
            BufferedReader in = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                int ttlidx= inputLine.indexOf("ttl=");
                for(i=0;i<ttlidx;i++) {
                    char c= inputLine.charAt(ttlidx+4+i);
                    if (c == ' ')
                        break;
                }
                if (i>0) {pingResult=inputLine.substring(ttlidx+4,ttlidx+4+i);
                    break;
                }
            }
            in.close();
        } catch (IOException e) {
            System.out.println(e);
        }
        return pingResult;

    }
}
