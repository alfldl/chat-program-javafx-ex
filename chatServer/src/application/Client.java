package application;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.Socket;

import javafx.stage.Stage;

public class Client {

	Socket socket;

	public Client(Socket socket) {
		this.socket = socket;
		receive();
	}

	// 클라이언트로부터 메시지를 전달받는 메소드
	public void receive() {
		// thread 에는 보통 Runnable 객체르 이용
		// Runnable이란 간단하게 말해서 Thread의 인터페이스화 된 형태이다.
		// 다른클래스의 상속도 받을수 있음 / 반드시 run 함수를 가지고 있어야함
		Runnable thread = new Runnable() {
			@Override
			public void run() {
				try {
					// 반복적으로 클라이언트에게 내용을 전달받는 while문
					while (true) {
						InputStream in = socket.getInputStream();
						byte[] buffer = new byte[512]; // 한번에 521바이트 전달을받을수있음
						int length = in.read(buffer); // 버퍼에 담음
						while (length == -1)
							throw new IOException();
						System.out.println("[메시지 수신성공]" + socket.getRemoteSocketAddress() // 접속한 클라이언트의 주소출력
								+ ": " + Thread.currentThread().getName()); // 스레드 고유정보출력
						String message = new String(buffer, 0, length, "UTF-8"); // 전달 받은값을 한글포함

						// 다른클라이언트에게 보이도록 보냄
						for (Client client : Main.clients) {
							client.send(message);
						}
					}
					// 오류발생 catch 중첩문이용
				} catch (Exception e) {
					try {
						System.out.println("[메시지 수신 오류]" + socket.getRemoteSocketAddress() + ": "
								+ Thread.currentThread().getName());
					} catch (Exception e2) {
						e2.printStackTrace();
					}
				}
			}

		};
		Main.threadpool.submit(thread);// Runnable thread을 생성시 안정적으로 관리하기위해 threadpool에 등록
	}

	// 클라이언트에게 메시지를 전송하는 메소드
	public void send(String message) {
		Runnable thread = new Runnable() {
			@Override
			public void run() {
				try {
					OutputStream out = socket.getOutputStream();
					byte[] buffer = message.getBytes("UTF-8");
					out.write(buffer);// 버퍼에 담긴것을 서버에 전송
					out.flush();
				} catch (Exception e) {
					try {
						System.out.println("[메시지 송신 오류]" + socket.getRemoteSocketAddress() + ": "
								+ Thread.currentThread().getName());
						Main.clients.remove(Client.this);// 오류난 클라이언트를 제거
						socket.close();
					} catch (Exception e2) {
						e2.printStackTrace();
					}
				}
			}
		};
		Main.threadpool.submit(thread);
	}
}
