import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class TimeServer {
    public static void main(String[] args) throws IOException {
        ServerSocket listener = new ServerSocket(9090);
        try {
            while (true) {
                Socket socket = listener.accept();
                new Handler(socket).start();
            }
        } finally {
            listener.close();
        }
    }
}

class Handler extends Thread {
    private String name;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public Handler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        Msg r= new Msg();
        long syncIssuedTimestamp,delayRecTimestamp;
        try {
            //in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //out = new PrintWriter(socket.getOutputStream(), true);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            while (true) {
                r=(Msg)ois.readObject();
                //System.out.println("1111111");
                if(r.getMsg_type().compareToIgnoreCase("Exit")==0)
                {
                    r.setMsg_type("Ack_End");
                    oos.writeObject(r);
                    break;
                }
                if(r.getMsg_type().compareToIgnoreCase("Ready")==0)
                {
                    //r.setTime(new Date().getTime());
                    r.setMsg_type("Sync");
                    oos.writeObject(r);
                    syncIssuedTimestamp=new Date().getTime();
                    Msg r2=new Msg();
                    r2.setTime(syncIssuedTimestamp);
                    r2.setMsg_type("FolllowUp");
                    oos.writeObject(r2);
                    r=(Msg)ois.readObject();
                    delayRecTimestamp=new Date().getTime();
                    if(r.getMsg_type().compareToIgnoreCase("DelayReq")==0) {
                        r.setTime(delayRecTimestamp);
                        r.setMsg_type("DelayResp");
                        oos.writeObject(r);
                    }
                    else{
                        System.out.println("Error when DelayReq packet is expected");
                        break;}
                }
                else {
                    r.setTime(new Date().getTime());
                    oos.writeObject(r);
                }
            }
        }
        catch (EOFException e){
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
