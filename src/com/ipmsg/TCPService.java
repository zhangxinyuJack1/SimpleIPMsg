package com.ipmsg;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPService {

	public static final int PORT = 30000;
	private static final int BUFF_SIZE = 8192;

	private static Object lock = new Object();

	private ServerSocket serverSocket;

	private static boolean isInit = false;

	private TCPService() throws IOException {
		serverSocket = new ServerSocket(PORT);
		SendFileServer server = new SendFileServer();
		server.start();
	}

	public static void init() {
		synchronized (lock) {
			if (!isInit) {
				try {
					new TCPService();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private class SendFileServer extends Thread {
		@Override
		public void run() {
			try {
				while (true) {
					Socket socket = serverSocket.accept();
					SendFile st = new SendFile(socket);
					st.start();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private class SendFile extends Thread {
		Socket socket;

		SendFile(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			DataInputStream in = null;
			DataInputStream fin = null;
			DataOutputStream out = null;
			try {
				int len = 0;
				byte[] buff = new byte[BUFF_SIZE];
				in = new DataInputStream(socket.getInputStream());
				len = in.read(buff);
				String filePath = new String(buff, 0, len, "UTF-8");
				File file = new File(filePath);
				fin = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
				out = new DataOutputStream(socket.getOutputStream());
				out.writeLong(file.length());
				out.flush();
				buff = new byte[BUFF_SIZE];
				while ((len = fin.read(buff)) > 0) {
					out.write(buff, 0, len);
				}
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (in != null) {
						in.close();
						in = null;
					}
					if (fin != null) {
						fin.close();
						fin = null;
					}
					if (out != null) {
						out.close();
						out = null;
					}
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
