package ru.maklas.http;

import java.net.InetSocketAddress;
import java.net.Proxy;

/** Http proxy **/
public class ProxyData {

	private final Proxy.Type type;
	private final String address;
	private final int port;
	private Proxy instance;

	public ProxyData(String address, int port) {
		this.address = address;
		this.port = port;
		this.type = Proxy.Type.HTTP;
	}

	public ProxyData(Proxy proxy) {
		this.address = ((InetSocketAddress) proxy.address()).getHostString();
		this.port = ((InetSocketAddress) proxy.address()).getPort();
		this.type = proxy.type();
		instance = proxy;
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

	public boolean isInitialized() {
		return instance != null;
	}

	public Proxy getJavaProxy() {
		if (instance == null) {
			instance = new Proxy(type, new InetSocketAddress(address, port));
		}
		return instance;
	}

	@Override
	public String toString() {
		return address + ":" + port;
	}
}
