import java.net.*;
import java.util.*;
import java.io.*;

public class TextServer {
	private static final int PORT = 56700;
	private ServerSocket server;

	public TextServer(ServerSocket server) {
		this.server = server;
	}

	public static void main(String[] args) throws IOException {

		ServerSocket serverSocket = new ServerSocket(PORT);
		TextServer server = new TextServer(serverSocket);
		server.initialize(); // open the server
	}

	public void initialize() // opens the server and accepts clients into chat
	{
		try {
			while (!server.isClosed()) {
				Socket client = server.accept();
				System.out.println("New client connected");
				ClientHandler clientHandler = new ClientHandler(client);
				Thread thread = new Thread(clientHandler);
				thread.start();
			}
			closeServer();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void closeServer() // closes the server socket
	{
		try {
			if (server != null) {
				server.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

//clientHandler handles every messages and commands from the client
	public static class ClientHandler implements Runnable {
		public static ArrayList<ClientHandler> usersConnected = new ArrayList<>();
		private Socket client;
		private BufferedWriter outPublic; // output for everyone to see
		private PrintWriter outPrivate; // output for personal viewing
		private Scanner in;
		private String clientUsername;

		public ClientHandler(Socket client) {
			try {
				this.in = new Scanner(client.getInputStream());
				this.outPrivate = new PrintWriter(client.getOutputStream(), true);
				this.client = client;
				this.outPublic = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));

				this.clientUsername = in.nextLine(); // if username is valid, add to list of connected users
				checkName(this.clientUsername.replaceAll("\\s", ""));
				usersConnected.add(this);

				outPrivate.println("Welcome to the chat room! Enter text to chat."); // private greeting message
				outPrivate.println("For list of commands, type <commands>");
				broadcastMessage("SERVER: " + clientUsername + " has entered the chat!"); // public message

			} catch (Exception e) {
				closeEverything(client, outPublic, outPrivate, in);
			}
		}
		
		// method checks name to avoid duplicates and empty string
		public void checkName(String name) throws IOException 
		{
			boolean goodName = true;
			for (ClientHandler clientHandler : usersConnected) {
				if (name.equals(clientHandler.clientUsername)) {
					outPrivate.println("Rejected. Screen name is taken.\nEnter another username: ");
					name = in.nextLine();
					goodName = false;
				}
				if (name.equals("")) {
					outPrivate.println("Rejected. Screen name invalid.\nEnter another username: ");
					name = in.nextLine();
					goodName = false;
				}
			}
			if (goodName != true) // continuously asks for and checks new username until it is valid
				checkName(name);
			else {
				outPrivate.println("Accepted.");
				this.clientUsername = name;
			}
		}

		// method checks string for commands, otherwise it will be a message for the
		// group to see
		@Override
		public void run()
							
		{
			String messageFromClient;
			while (client.isConnected()) {
				try {
					messageFromClient = in.nextLine();
					if (messageFromClient.equals("<commands>")) {
						commandList();
					} else if (messageFromClient.equals("<bye>")) {
						removeUser();
						break;
					} else if (messageFromClient.equals("<users>")) {
						printUserList();
					} else if (messageFromClient.startsWith("<private | ") && messageFromClient.contains(">")) {

						privateMessage(messageFromClient);
					} else
						broadcastMessage(messageFromClient);

				} catch (Exception e) {
					closeEverything(client, outPublic, outPrivate, in);
					break;
				}
			}
		}

		public void commandList() // method lists commands
		{
			outPrivate.println("Commands:\n<bye> - leave chat" 
							  + "\n<users> - list of currently connected users"
							  + "\n<private | user> message - send message to only specified user");
		}

		// disconnects client from server
		public void removeUser() 
		{
			broadcastMessage("SERVER: " + clientUsername + " has left.");
			usersConnected.remove(this);
			System.out.println("A client has disconnected");
		}

		// sends list of users privately to the client that typed the command
		public void printUserList() 
		{
			for (ClientHandler clientHandler : usersConnected) {
				try {
					if (clientHandler.clientUsername.equals(clientUsername)) {
						outPrivate.println("Current Connected Users:");
						for (int i = 0; i < usersConnected.size(); i++) {
							outPrivate.println((i + 1) + " - " + usersConnected.get(i).toString());
						}

					}
				} catch (Exception e) {
					closeEverything(client, outPublic, outPrivate, in);
				}
			}

		}

		// private message is sent to designated client
		public void privateMessage(String messageToSend) 
		{
			boolean userExist = false;
			String[] array = messageToSend.split("\\|");
			String[] array2 = array[1].split(">");
			if (array2.length <= 1) {
				broadcastMessage(messageToSend);
				return;
			}
			String message = array2[1];
			String name = array2[0].replaceAll("\\s", "");
			if (message.startsWith(" ", 0)) {
				message = message.replaceFirst(" ", "");
			}
			for (ClientHandler clientHandler : usersConnected) {
				try {
					if (clientHandler.clientUsername.equals(name)) {
						clientHandler.outPublic.write("(private)" + clientUsername + ": " + message);
						clientHandler.outPublic.newLine();
						clientHandler.outPublic.flush();
						userExist = true;

					}
				} catch (Exception e) {
					closeEverything(client, outPublic, outPrivate, in);
				}
			}
			if (userExist == false) // message will not be sent if name is not in the list of connected users
			{
				outPrivate.println("Unknown user");
			}

		}

		// message will become public for everyone to see
		public void broadcastMessage(String messageToSend) {
			for (ClientHandler clientHandler : usersConnected) {
				try {
					// everyone but the one writing the message will receive the message
					if (!clientHandler.clientUsername.equals(clientUsername)) {
						if (messageToSend.startsWith("SERVER:")) {
							clientHandler.outPublic.write(messageToSend);
						} else {
							clientHandler.outPublic.write(clientUsername + ": " + messageToSend);
						}
						clientHandler.outPublic.newLine();
						clientHandler.outPublic.flush();
					}
				} catch (Exception e) {
					closeEverything(client, outPublic, outPrivate, in);
				}
			}
		}

		public void closeEverything(Socket client, BufferedWriter outPublic, 
									PrintWriter outPrivate, Scanner in) {
			removeUser();
			try {
				if (outPublic != null)
					outPublic.close();
				if (outPrivate != null)
					outPrivate.close();
				if (in != null)
					in.close();
				if (client != null)
					client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		// name of the client will be returned instead of the address
		@Override
		public String toString() {
			return clientUsername;
		}
	}
}



