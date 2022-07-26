import java.io.*;
import java.net.*;
import java.util.*;

public class TextClient {
	private Socket socket;
	private String username;
	private BufferedWriter out;
	private BufferedReader in;
	private static Scanner input = new Scanner(System.in);
	private static final int PORT = 56700;

	public TextClient(Socket socket, String username) // add a client
	{
		try {
			this.socket = socket;
			this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			this.username = username;
		} catch (IOException e) {
			closeEverything(socket, in, out, input);
		}
	}
	public static void main(String[] args) throws IOException {
		System.out.println("Enter username: ");
		String username = input.nextLine();
		Socket socket = new Socket("localhost", PORT);
		TextClient client = new TextClient(socket, username);
		client.readMessage(); // while client is connected to the server,
		client.sendMessage(); // it will receive and be able to send messages
	}

	// messages client sends will be sent to the clientHandler
	public void sendMessage() 
	{
		try {
			out.write(username); // first message client types will be for the username
			out.newLine();
			out.flush();

			// clientHandler will sort through messages for potential commands
			while (socket.isConnected()) 
			{
				String messageToSend = input.nextLine();
				out.write(messageToSend);
				out.newLine();
				out.flush();
			}
		} catch (IOException e) {
			closeEverything(socket, in, out, input);
		}
	}

	 // reads message from the group chat and sends to client for client to see
	public void readMessage()
	{
		new Thread(new Runnable() {
			@Override
			public void run() {
				String messageFromChat;
				while (socket.isConnected()) {
					try {
						messageFromChat = in.readLine();
						System.out.println(messageFromChat);
					} catch (IOException e) {
						closeEverything(socket, in, out, input);
					}
				}
			}
		}).start();
	}

	// close everything when client disconnects from server
	public void closeEverything(Socket socket, BufferedReader in, 
							    BufferedWriter out, Scanner input) {
		try 
		{
			if (in != null)
				in.close();
			if (out != null)
				out.close();
			if (socket != null)
				socket.close();
			if (input != null)
				input.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
