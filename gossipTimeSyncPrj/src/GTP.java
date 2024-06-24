import java.io.*;
import java.net.*;
import java.util.Date;

public class GTP {
    public static void main(String[] args) {

        ServerSocket listener = null;
        Timer localTimer=null;

        if(args.length>0)
            localTimer = new Timer(new Date().getTime(),Integer.parseInt(args[0]));
        else
            localTimer = new Timer(new Date().getTime());

        localTimer.start();
        new TimerUpdater(localTimer).start();

        //************************************************
        try {
            listener = new ServerSocket(9091);

            System.out.println("Listening on 9091");
            new GtpUDPRequester().start(); //register yourself on all PSSs by broadcasting

            while (true) {
                Socket sersocket = listener.accept();
                new RequestHandler(sersocket, localTimer).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                listener.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class GtpUDPRequester extends Thread {
    DatagramPacket packet;
    Msg msg4Reg;
    long tetha_min = 500;
    long theta_max = 1000;
    long tetha;
    long TETA_min;
    long TETA_max;
    long DISPERSION = Long.MAX_VALUE;
    long LAST_UPDATE = -1900000000;
    private String hostname = "255.255.255.255";
    private int port = 1234;
    private InetAddress host;
    private DatagramSocket socket;
    public static int activeNodes=1;

    public void run() {
        while (true) {
            try {
                host = InetAddress.getByName(hostname);
                socket = new DatagramSocket(null);

                // ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                // ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                msg4Reg = new Msg();
                msg4Reg.setMsg_type("HeartBit");
                msg4Reg.setIP_Add("192.168.1.10");  //redundant; should be removed
                msg4Reg.setPort_Num(9091);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(msg4Reg);
                oos.flush();
                byte[] Buf = baos.toByteArray();
                packet = new DatagramPacket(Buf, Buf.length, host, port);
                socket.send(packet);

                //socket.receive(packet); //ACK message should be removed.
                socket.close();
            /*
            byte[] data = packet.getData();
            String time = new String(data);  // convert byte array data into string
            System.out.println(time);
            */
                Thread.sleep(3000*GtpUDPRequester.activeNodes);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

class RequestHandler extends Thread {
    private String name;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Timer localTimer;
    long recvTimestamp, T1, T2, roundtripDelay, msgDispersion, ourDispersion;

    public RequestHandler(Socket socket, Timer localTimer) {
        this.socket = socket;
        this.localTimer = localTimer;
    }

    public void run() {
        Msg recvMsg = new Msg();
        Msg recvFbMsg;
        //long syncIssuedTimestamp,delayRecTimestamp;

        try {

            //in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //out = new PrintWriter(socket.getOutputStream(), true);

            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            recvMsg = (Msg) ois.readObject();
            recvTimestamp = new Date().getTime();
            //System.out.println("1111111");
            if (recvMsg.getMsg_type().compareToIgnoreCase("Exit") == 0) {
                recvMsg.setMsg_type("Ack_End");
                oos.writeObject(recvMsg);
            }

            if (recvMsg.getMsg_type().compareToIgnoreCase("getTimeSampleReq") == 0) {

                //r.setTime(new Date().getTime());
                recvMsg.setMsg_type("getTimeSampleResp");
                recvMsg.setTime(localTimer.getTime());
                recvMsg.setDistance(localTimer.getDistance());
                recvMsg.setDispersion(localTimer.CalcDispersion());

                T1 = localTimer.getTime();
                oos.writeObject(recvMsg);
                //syncIssuedTimestamp=new Date().getTime();
                recvFbMsg = (Msg) ois.readObject();
                T2 = localTimer.getTime();
                if (recvFbMsg.getMsg_type().compareToIgnoreCase("Feedback") == 0) {
                    //if(ExamineSample(recvFbMsg))
                    //  adjustLocalClock
                    //else rejectSample()
                    roundtripDelay = T1 - T2;
                    if (recvFbMsg.getDispersion() < Long.MAX_VALUE)
                        msgDispersion = recvFbMsg.getDispersion() + roundtripDelay / 2;
                    else
                        msgDispersion = Long.MAX_VALUE;
                    ourDispersion = localTimer.CalcDispersion();
                    if (msgDispersion <= ourDispersion && localTimer.getDistance() > recvFbMsg.getDistance()) {
                        localTimer.setDistance(recvFbMsg.getDistance() + 1);
                        localTimer.setTime(recvFbMsg.getTime() + roundtripDelay / 2);
                        localTimer.setDISPERSION(msgDispersion);
                    }

                } else {
                    System.out.println("Error when Feedbck packet is expected");
                }
            } else {
                recvMsg.setTime(new Date().getTime());
                oos.writeObject(recvMsg);
            }
        }
        catch (EOFException e) {
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        catch (SocketException e) {
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
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

class Timer extends Thread {
    private long cTime;
    private Object timeLock = new Object();


    long DISPERSION = Long.MAX_VALUE;
    long lastUpdate = -Long.MAX_VALUE;
    double accuarcy = .99;
    int distance = 10000;


    public Timer(long cT) {
        this.cTime = cT;
    }
    public Timer(long cT,int distance) {
        this.cTime = cT;
        this.distance= distance;
    }


    public long getDISPERSION() {
        synchronized (timeLock) {
            return DISPERSION;
        }
    }

    public void setDISPERSION(long DISPERSION) {
        synchronized (timeLock) {
            this.DISPERSION = DISPERSION;
        }
    }

    long CalcDispersion() {
        if (DISPERSION != Long.MAX_VALUE)
            return (long) (DISPERSION + accuarcy * (getTime() - lastUpdate));
        return DISPERSION;
    }

    public int getDistance() {
        synchronized (timeLock) {
            return distance;
        }
    }

    public void setDistance(int distance) {
        synchronized (timeLock) {
            this.distance = distance;
        }
    }


    long getTime() {
        synchronized (timeLock) {
            return cTime;
        }
    }

    void setTime(long t) {
        synchronized (timeLock) {
            cTime = t;
            lastUpdate = t;
        }
        return;
    }

    long syncTime(long dt) {
        synchronized (timeLock) {
            cTime += dt;
            lastUpdate = cTime;
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

class TimerUpdater extends Thread {
    Timer localTimer;

    TimerUpdater(Timer localTimer) {
        this.localTimer = localTimer;
    }

    public void removeNode(String IpAdd)
    {
        DatagramPacket packet;
        Msg msg4Reg;
        String hostname = "255.255.255.255";
        int port = 1234;
        InetAddress host;
        DatagramSocket socket;

        try {
            host = InetAddress.getByName(hostname);
            socket = new DatagramSocket(null);

        // ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        // ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        msg4Reg = new Msg();
        msg4Reg.setMsg_type("Dead");
        msg4Reg.setIP_Add(IpAdd);
        msg4Reg.setPort_Num(9091); //redundant; should be removed

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(msg4Reg);
        oos.flush();
        byte[] Buf = baos.toByteArray();
        packet = new DatagramPacket(Buf, Buf.length, host, port);
        socket.send(packet);

        //socket.receive(packet); //ACK message should be removed.
        socket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        Socket socket = null;
        Socket socket4TimeSync = null;
        Msg msg4GetPeer=null;
        long T1, T2, roundtripDelay, msgDispersion, ourDispersion;
        int  actNodenum=1;
        while (true) {
            try {
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    socket = new Socket("127.0.0.1", 7080);


                    ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    msg4GetPeer = new Msg();

                    msg4GetPeer.setMsg_type("GetPeerReq");
                    oos.writeObject(msg4GetPeer);

                    msg4GetPeer = (Msg) ois.readObject();
                    System.out.println("Response: " + msg4GetPeer.getMsg_type() + "Peer Address(IP:Port): " + msg4GetPeer.getIP_Add()
                            + ":" + msg4GetPeer.getPort_Num());
                    GtpUDPRequester.activeNodes =msg4GetPeer.getActiveNodesNum();

                    //********** for time sync ************************
                    if (msg4GetPeer.getIP_Add() != null) {

                        System.out.println("\nTry to connect to ->  " + msg4GetPeer.getIP_Add() + ":" + msg4GetPeer.getPort_Num());

                        socket4TimeSync = new Socket(msg4GetPeer.getIP_Add(), 9091);
                        Msg msg4GetTime = new Msg();
                        ObjectInputStream ois4TimeSync = new ObjectInputStream(socket4TimeSync.getInputStream());
                        ObjectOutputStream oos4TimeSync = new ObjectOutputStream(socket4TimeSync.getOutputStream());

                        msg4GetTime.setMsg_type("getTimeSampleReq");
                        T1 = localTimer.getTime();
                        oos4TimeSync.writeObject(msg4GetTime);

                        msg4GetTime = (Msg) ois4TimeSync.readObject();
                        T2 = localTimer.getTime();
                        System.out.println("Response: " + msg4GetTime.getMsg_type() + "Peer Time: " + msg4GetTime.getTime());

                        roundtripDelay = T1 - T2;
                        msgDispersion = msg4GetTime.getDispersion() + roundtripDelay / 2;
                        ourDispersion = localTimer.CalcDispersion();
                        if (msgDispersion <= ourDispersion && localTimer.getDistance() > msg4GetTime.getDistance()) {
                            localTimer.setDistance(msg4GetTime.getDistance() + 1);
                            localTimer.setTime(msg4GetTime.getTime() + roundtripDelay / 2);
                            System.out.println("Accept and Set Clock: " + localTimer.getTime());
                            localTimer.setDISPERSION(msgDispersion);

                        }
                        else{
                        System.out.println("Rejected Clock:" + localTimer.getTime());
                        }
                        Msg msg4FbTime = new Msg();
                        msg4FbTime.setMsg_type("Feedback");
                        msg4FbTime.setTime(localTimer.getTime());
                        msg4FbTime.setDispersion(localTimer.CalcDispersion());
                        msg4FbTime.setDistance(localTimer.getDistance());
                        oos4TimeSync.writeObject(msg4FbTime);

                    }
                }
                catch (ConnectException e) {
                    removeNode(msg4GetPeer.getIP_Add());
                    System.out.println("Node -----> " + msg4GetPeer.getIP_Add() +"<-------- is not accessable");
                    //e.printStackTrace();
                }
                catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                catch (IOException e) {
                    e.printStackTrace();

                } finally {
                    try {
                        if (socket != null)
                            socket.close();
                        if (socket4TimeSync != null)
                            socket4TimeSync.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //System.exit(0);
                }


                Thread.sleep(2000);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
