package application;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Border;
import javafx.stage.Stage;

public class Main extends Application {

	public static ExecutorService threadpool;
	// 스레드풀은 작업처리에 사용되는 스레드의 숫자를 정해 놓고
	// 작업 큐(Queue)에 들어오는 작업들을 하나씩 맡아 처리하는 것을 말한다.
	// 즉, 스레드 갯수를 정해놓고 늘리지 않으면서 작업처리가 끝난 스레드가
	// 다시 작업 큐에서 새로운 작업을 가져와 처리하는 식이다.

	public static Vector<Client> clients = new Vector<Client>();
	// Vector - 조금 쉽게 사용할 수있는 배열 같은거

	ServerSocket serverSocket;

	// 서버를 구동시켜서 클라이언트의 연결을기다리는 메소드
	public void startServer(String IP, int port) {
		try {
			serverSocket = new ServerSocket();
			serverSocket.bind(new InetSocketAddress(IP, port)); // 특정한 클라이언트의 접속을 내 IP와PORT로 기다림
		} catch (Exception e) { // 서버소켓이 닫혀있는 상태가 아니라면 stop
			if (!serverSocket.isClosed())
				stopServer();
			return;
		}

		// 클라이언트가 접속할때까지 계속 기다리는 쓰레드
		Runnable trhead = new Runnable() {
			@Override
			public void run() {
				// 계속 새로운 클라이언트가 접속하도록 함
				while (true) {
					try {
						Socket socket = serverSocket.accept();
						clients.add(new Client(socket));
						System.out.println("[클라이언트접속]" + ": " + socket.getRemoteSocketAddress() + ": "
								+ Thread.currentThread().getName());
					} catch (Exception e) {
						if (!serverSocket.isClosed())
							stopServer();
						break; // 문제 발생시 break로 서버에서 빠저나옴
					}
				}
			}
		};
		threadpool = Executors.newCachedThreadPool(); // 스레드풀 초기화
		threadpool.submit(trhead);
	}

	// 서버 작동을 중단시키는 메소드
	public void stopServer() {
		try {
			// 현재 작동중인 모든 소켓 닫기
			Iterator<Client> iterator = clients.iterator();
			while (iterator.hasNext()) {// 한명한명씩 접근해서 닫기
				Client client = iterator.next();
				client.socket.close();
				iterator.remove();
			}
			// 서버소켓객체닫기
			if (serverSocket != null && !serverSocket.isClosed()) {
				serverSocket.close();
			}
			// 스레드풀 종료하기
			if (threadpool != null && !threadpool.isShutdown()) {
				threadpool.shutdown();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// UI를 생성하고 ,실질적으로 프로그램을 동작시키는 메소드
	@Override
	public void start(Stage primaryStage) {
		BorderPane root = new BorderPane();
		root.setPadding(new Insets(5));
		
		TextArea textArea = new TextArea();
		textArea.setEditable(false);
		textArea.setFont(new Font("나눔고딕",15));
		root.setCenter(textArea);
		
		Button toggleButton = new Button("시작하기");
		toggleButton.setMaxWidth(Double.MAX_VALUE);
		BorderPane.setMargin(toggleButton, new Insets(1,0,0,0));
		root.setBottom(toggleButton);
		
		String IP = "211.183.2.45";
		int port = 9876;
		
		toggleButton.setOnAction(event->{
			if(toggleButton.getText().equals("시작하기")) {
				startServer(IP, port);
				Platform.runLater(()->{
					String message = String .format("[서버시작]\n", IP, port);
					textArea.appendText(message);
					toggleButton.setText("종료하기");
				});
			}else {
				stopServer();
				Platform.runLater(()->{
					String message = String .format("[서버종료]\n", IP, port);
					textArea.appendText(message);
					toggleButton.setText("종료하기");						
			});
		}
	});
		
	Scene scene=new Scene(root, 400,400);
	primaryStage.setTitle("[채팅서버]");
	primaryStage.setOnCloseRequest(event->stopServer());
	primaryStage.setScene(scene);
	primaryStage.show();
}
	// 프로그램의 진입점
	public static void main(String[] args) {
		launch(args);
	}
}
