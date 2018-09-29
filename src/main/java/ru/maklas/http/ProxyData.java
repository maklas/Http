package ru.maklas.http;

import java.net.Proxy;

public class ProxyData {

    private final Proxy.Type type;
    private final String address;
    private final int port;
    private boolean validated;

    public ProxyData(String address, int port) {
        this.address = address;
        this.port = port;
        this.type = Proxy.Type.HTTP;
        this.validated = false;
    }

    public boolean isValidated() {
        return validated;
    }

    public void setValidated(boolean validated) {
        this.validated = validated;
    }

    public Proxy.Type getType() {
        return type;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return address + ":" + port;
    }
}
