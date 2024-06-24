import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;

public class PSS {
    static private ArrayList<InetAdd> nodeAddList = new ArrayList<InetAdd>();

    static boolean removeNode(String Ipadd, int portNum) {
        for (int i = 0; i < nodeAddList.size(); i++) {
            InetAdd nodeAdd = nodeAddList.get(i);
            if (nodeAdd.Ip.compareTo(Ipadd) == 0) {
                nodeAddList.remove(i);
                return true;
            }
        }
        return false;
    }

    static boolean addNode(String Ipadd, int portNum) {

        for (int i = 0; i < nodeAddList.size(); i++) {
            InetAdd nodeAdd = nodeAddList.get(i);
            if (nodeAdd.Ip.compareTo(Ipadd) == 0) {
                return true;
            }
        }
        nodeAddList.add(new InetAdd(Ipadd, portNum));
        return true;
    }


    static InetAdd getPeerNode() {
        if (nodeAddList.size() > 0) {
            int randomIdx = (int) (Math.random() * nodeAddList.size());
            return (nodeAddList.get(randomIdx));
        }
        return null;
    }

    static int getActiveNodes()
    {
        return (nodeAddList.size());
    }

    public static void main(String[] args) throws IOException {



        new PssUDPHandler().start();

        ServerSocket listener = new ServerSocket(7080);

        try {
            while (true) {
                Socket socket = listener.accept();
                new PssHandler(socket).start();
            }
        } finally {
            listener.close();
        }
    }

}

class PssHandler extends Thread {
    private String name;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;


    public PssHandler(Socket socket) {
        this.socket = socket;
    }


    public void run() {
        Msg r = new Msg();
        long syncIssuedTimestamp, delayRecTimestamp;
        try {
            //in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //out = new PrintWriter(socket.getOutputStream(), true);

            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            r = (Msg) ois.readObject();
            //System.out.println("1111111");
            if (r.getMsg_type().compareToIgnoreCase("GetPeerReq") == 0) {
                InetAdd node = PSS.getPeerNode();
                r.setMsg_type("GetPeerResp");
                if (node != null) {
                    r.setIP_Add(node.Ip);
                    r.setPort_Num(node.Port);
                }
                else{
                    r.setIP_Add(null);
                    r.setPort_Num(0);
                }
                r.setActiveNodesNum(PSS.getActiveNodes()+1);

                oos.writeObject(r);
            }
            if (r.getMsg_type().compareToIgnoreCase("REG") == 0) {
                PSS.addNode(r.getIP_Add(), r.getPort_Num());

                //r.setMsg_type("ACK_REG"); //removed because of non ACK
                //oos.writeObject(r);

            } else {
                r.setMsg_type("UNKNOWN_REQ");
                oos.writeObject(r);
            }
        } catch (EOFException e) {
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
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

class InetAdd {
    String Ip;
    int Port;
    public InetAdd(String ip, int port) {
        Ip = ip;
        Port = port;
    }
}

class PssUDPHandler extends Thread {
    public static final int DEFAULT_PORT = 1234;
    private DatagramSocket socket;
    private DatagramPacket packet;

    public void run() {
        Msg msgClinet;
        try {
            socket = new DatagramSocket(DEFAULT_PORT);
            socket.setBroadcast(true);

        } catch (Exception ex) {
            System.out.println("Problem creating socket on port: " + DEFAULT_PORT);
        }

        //packet = new DatagramPacket (new byte[1], 1);
        byte[] buffer = new byte[100000];
        packet = new DatagramPacket(buffer, buffer.length);

        while (true) {
            try {
                socket.receive(packet);
                System.out.println("Received from(UDP): " + packet.getAddress() + ":" +
                        packet.getPort());


                ByteArrayInputStream baos = new ByteArrayInputStream(buffer);
                ObjectInputStream oos = new ObjectInputStream(baos);

                msgClinet = (Msg) oos.readObject();
                if(msgClinet.getMsg_type().compareToIgnoreCase("HeartBit")==0) {
                    msgClinet.setIP_Add(packet.getAddress().getHostAddress());
                    if (!islocalIpAdd(msgClinet.getIP_Add())) {
                        PSS.addNode(msgClinet.getIP_Add(), msgClinet.getPort_Num());
                    }
                    System.out.println("recived Req(UDP): " + msgClinet.getMsg_type() + " For IP(Port): " + msgClinet.getIP_Add() + ":" + msgClinet.getPort_Num());
                }

                if(msgClinet.getMsg_type().compareToIgnoreCase("Dead")==0) {

                    if (!islocalIpAdd(msgClinet.getIP_Add())) {
                        PSS.removeNode(msgClinet.getIP_Add(), msgClinet.getPort_Num());
                    }
                    System.out.println("--recived Req(UDP): " + msgClinet.getMsg_type() + " For IP(Port): " + msgClinet.getIP_Add() + ":" + msgClinet.getPort_Num());
                }


                /* // Ack of recieved packet should be removed
                byte[] outBuffer = new java.util.Date ().toString ().getBytes ();
                packet.setData (outBuffer);
                packet.setLength (outBuffer.length);
                socket.send (packet);
                */
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException ie) {
                ie.printStackTrace();
            }
        }
    }

    boolean islocalIpAdd(String ipAdd)
    {
        Enumeration e = null;
        try {
            e = NetworkInterface.getNetworkInterfaces();

        while(e.hasMoreElements())
        {
            NetworkInterface n = (NetworkInterface) e.nextElement();
            Enumeration ee = n.getInetAddresses();
            while (ee.hasMoreElements())
            {
                InetAddress i = (InetAddress) ee.nextElement();
                if(i.getHostAddress().equalsIgnoreCase(ipAdd))
                    return true;
                //System.out.println(i.getHostAddress());
            }
        }} catch (SocketException e1) {
            e1.printStackTrace();
        }
        return false;

    }
}