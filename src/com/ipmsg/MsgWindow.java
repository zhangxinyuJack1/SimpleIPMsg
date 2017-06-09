package com.ipmsg;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileFilter;

public class MsgWindow extends JFrame {
	private static final long serialVersionUID = 1L;

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private JLabel label1;
	private JLabel label2;
	private JTextArea textArea;
	private JButton btn_reply;
	private JButton btn_close;
	private JCheckBox chkBox;
	private JButton btn_file = new JButton("no file");

	private String filePath = "";
	private String fileName = "";
	private String hostAddr = null;

	private MainWindow mainWindow;

	private JFileChooser fileChooser = new JFileChooser();

	ReceiveFile receive = null;

	private MsgWindow() {
		this.setTitle("Receive Message");
		this.setLayout(null);
		this.setBounds(600, 300, 300, 300);
		this.setResizable(false);
		this.setVisible(false);
		label1 = new JLabel();
		this.add(label1);
		label1.setBounds(10, 0, 300, 20);
		label1.setHorizontalAlignment(JLabel.CENTER);
		label2 = new JLabel();
		this.add(label2);
		label2.setBounds(10, 20, 300, 20);
		label2.setHorizontalAlignment(JLabel.CENTER);
		textArea = new JTextArea();
		textArea.setBounds(0, 0, 295, 150);
		textArea.setLineWrap(true);
		textArea.setEditable(false);
		JScrollPane textScroll = new JScrollPane(textArea);
		textScroll.setBounds(0, 45, 295, 150);
		textScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		this.add(textScroll);
		btn_reply = new JButton("回复");
		this.add(btn_reply);
		btn_reply.setBounds(120, 230, 60, 30);
		chkBox = new JCheckBox("引用原文");
		this.add(chkBox);
		chkBox.setBounds(20, 230, 80, 30);
		btn_close = new JButton("关闭");
		this.add(btn_close);
		btn_close.setBounds(200, 230, 60, 30);
		this.add(btn_file);
		btn_file.setBounds(0, 195, 300, 30);
		btn_file.setEnabled(false);

		btn_reply.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				String[] txtArr = textArea.getText().split("\n");
				StringBuffer sb = new StringBuffer();
				for (String s : txtArr) {
					s = ">" + s;
					sb.append(s + "\n");
				}
				mainWindow.setInpuArea(sb.toString());
			}
		});

		btn_file.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// fileChooser.setFileSelectionMode(JFileChooser.SAVE_DIALOG |
				// JFileChooser.DIRECTORIES_ONLY);
				fileChooser.setAcceptAllFileFilterUsed(false);
				fileChooser.setFileFilter(new FileFilter() {
					@Override
					public String getDescription() {
						return "文件夹";
					}

					@Override
					public boolean accept(File f) {
						if (f.isDirectory()) {
							return true;
						}
						return false;
					}
				});
				fileChooser.setSelectedFile(new File(fileName));
				int option = fileChooser.showDialog(null, "保存文件");
				if (JFileChooser.APPROVE_OPTION == option) {
					File file = fileChooser.getSelectedFile();
					try {
						btn_file.setEnabled(false);
						receive = new ReceiveFile(file);
						receive.start();
					} catch (IOException e1) {
						e1.printStackTrace();
					}

				}
			}
		});

		btn_close.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				closeWindow();
			}
		});

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				closeWindow();
			}
		});
	}

	public static MsgWindow createWindow(MainWindow mainWindow) {
		MsgWindow msgWindow = new MsgWindow();
		msgWindow.mainWindow = mainWindow;
		return msgWindow;
	}

	public void showMsg(String name, String msg, String file, String hostAddr) {
		this.label1.setText("From : " + name);
		this.label2.setText("Date : " + sdf.format(new Date()));
		this.textArea.setText(msg);
		if (file.length() > 0) {
			filePath = file;
			fileName = filePath.substring(filePath.lastIndexOf("\\") + 1);
			btn_file.setText(fileName);
			btn_file.setEnabled(true);
			this.hostAddr = hostAddr;
		}
		this.setVisible(true);
	}

	private void closeWindow() {
		this.setVisible(false);
		this.mainWindow = null;
		if (receive != null)
			receive.isRun = false;
	}

	class ReceiveFile extends Thread {
		Socket socket;
		File file;
		boolean isRun = true;

		ReceiveFile(File file) throws IOException {
			socket = new Socket(hostAddr, TCPService.PORT);
			this.file = file;
		}

		@Override
		public void run() {
			DataOutputStream out = null;
			DataInputStream in = null;
			DataOutputStream fout = null;
			try {
				while (isRun) {
					while (!socket.isConnected()) {
					}

					if (!file.exists()) {
						file.createNewFile();
					}
					out = new DataOutputStream(socket.getOutputStream());
					out.write(filePath.getBytes("UTF-8"));
					out.flush();
					in = new DataInputStream(socket.getInputStream());
					long fileSize = in.readLong();
					btn_file.setText("0/" + fileSize + "K");
					fout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
					byte[] buff = new byte[8192];
					int len = 0;
					int size = 0;
					while ((len = in.read(buff)) > 0) {
						if (!isRun) {
							break;
						}
						fout.write(buff, 0, len);
						size += len;
						btn_file.setText(size + "/" + fileSize);
					}
					btn_file.setText(size + "/" + fileSize + "[完成]");
					isRun = false;
				}
			} catch (IOException e) {
				isRun = false;
				e.printStackTrace();
			} finally {
				try {
					if (fout != null) {
						fout.close();
						fout = null;
					}
					if (in != null) {
						in.close();
						in = null;
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

	public static void main(String[] args) {
		MsgWindow msgWindow = new MsgWindow();
		msgWindow.setVisible(true);
	}
}
