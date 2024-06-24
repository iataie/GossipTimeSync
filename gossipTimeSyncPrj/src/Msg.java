import java.io.Serializable;

public class Msg implements Serializable {
    private static final long serialVersionUID = -5399605122490343339L;
    private String msg_type;
    private long time;
    private String IP_Add;
    private int Port_Num;
    private int distance;
    private long dispersion;
    private int activeNodesNum;

    public int getActiveNodesNum() {
        return activeNodesNum;
    }

    public void setActiveNodesNum(int activeNodesNum) {
        this.activeNodesNum = activeNodesNum;
    }

    public String getIP_Add() {
        return IP_Add;
    }

    public int getPort_Num() {
        return Port_Num;
    }


    public void setIP_Add(String IP_Add) {
        this.IP_Add = IP_Add;
    }

    public void setPort_Num(int port_Num) {
        Port_Num = port_Num;
    }


    public String getMsg_type() {
        return msg_type;
    }

    public void setMsg_type(String req_type) {
        this.msg_type = req_type;
    }

    public void setTime(long t) {
        time = t;
    }

    public long getTime() {
        return time;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public long getDispersion() {
        return dispersion;
    }

    public void setDispersion(long dispersion) {
        this.dispersion = dispersion;
    }
}