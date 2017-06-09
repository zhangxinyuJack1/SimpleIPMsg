package com.ipmsg;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

public class UDPService {

	private static final int PORT = 20000;
	private static final int BUFF_SIZE = 8192;

	private DatagramSocket datagramSocket;

	public UDPService() throws SocketException {
		datagramSocket = new DatagramSocket(PORT);
	}

	public void broadcast(String data) throws IOException {
		byte[] bytes = data.getBytes("UTF-8");
		DatagramPacket dp = new DatagramPacket(bytes, 0, bytes.length, InetAddress.getByName("255.255.255.255"), PORT);
		datagramSocket.send(dp);
	}

	public void send(String ip, int port, String data) throws IOException {
		byte[] bytes = data.getBytes("UTF-8");
		DatagramPacket dp = new DatagramPacket(bytes, 0, bytes.length, InetAddress.getByName(ip), port);
		datagramSocket.send(dp);
	}

	public Map<String, String> receive() throws IOException {
		byte[] buff = new byte[BUFF_SIZE];
		DatagramPacket dp = new DatagramPacket(buff, buff.length);
		datagramSocket.receive(dp);
		Map<String, String> map = new HashMap<String, String>(4);
		map.put("data", new String(buff, "UTF-8"));
		map.put("hostName", dp.getAddress().getHostName());
		map.put("hostAddr", dp.getAddress().getHostAddress());
		map.put("hostPort", dp.getPort() + "");
		return map;
	}
}
