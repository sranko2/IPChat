package server;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import common.*;
//To run program run server then run x amount of clientMenus for x amount of people
/*
* The server handles the client connection requests and communication
*/
public class Server {
	// a unique ID for each connection
	private static int uniqueId;
	// an ArrayList to keep the list of the Client
	private ArrayList<ClientThread> al;
	// to display time
	private SimpleDateFormat sdf;
	// the port number to listen for connection
	private int port;
	// the boolean that will be turned of to stop the server
	private boolean keepGoing;

	public RSA emsg;

	/*
	 * server constructor that receive the port to listen to for connection as
	 * parameter in console
	 */

	public Server(int port) {
		// the port
		this.port = port;
		// to display hh:mm:ss
		sdf = new SimpleDateFormat("HH:mm:ss");
		// ArrayList for the Client list
		al = new ArrayList<ClientThread>();
		emsg = new RSA(true);
	}

	public void start() {
		keepGoing = true;
		/* create socket server and wait for connection requests */
		try {
			// the socket used by the server
			ServerSocket serverSocket = new ServerSocket(port);

			// infinite loop to wait for connections
			while (keepGoing) {
				// format message saying we are waiting
				display("Server waiting for Clients on port " + port + ".");

				Socket socket = serverSocket.accept(); // accept connection
				// if I was asked to stop
				if (!keepGoing)
					break;
				ClientThread t = new ClientThread(socket); // make a thread of it
				al.add(t); // save it in the ArrayList
				t.start();
			}
			// I was asked to stop
			try {
				serverSocket.close();
				for (int i = 0; i < al.size(); ++i) {
					ClientThread tc = al.get(i);
					try {
						tc.sInput.close();
						tc.sOutput.close();
						tc.socket.close();
					} catch (IOException ioE) {
						// not much I can do
					}
				}
			} catch (Exception e) {
				display("Exception closing the server and clients: " + e);
			}
		}
		// something went bad
		catch (IOException e) {
			String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
			display(msg);
		}
	}

	/*
	 * For the GUI to stop the server
	 */
	protected void stop() throws UnknownHostException, IOException {
		keepGoing = false;
		// connect to myself as Client to exit statement
		// Socket socket = serverSocket.accept();
		new Socket("localhost", port);
	}

	/*
	 * Display an event (not a message) to the console or the GUI
	 */
	private void display(String msg) {
		String time = sdf.format(new Date()) + " " + msg;
		System.out.println(time);
	}

	private synchronized void listUsers() {

		for (int i = al.size() - 1; i >= 0; i--) {
			ClientThread ct = al.get(i);
			display("Sending list of users to " + ct.username);
			ArrayList<BigInteger> list = emsg
					.encryptMessage("List of the users connected at " + sdf.format(new Date()) + "\n", ct.key, ct.N);
			encryptedMsg enc = new encryptedMsg(Constants.WHOISINFIRST, list);
			if (!ct.writeMsg(enc)) {
				al.remove(i);
				display("Disconnected Client " + ct.username + " removed from list.");
			}
			// scan al the users connected
			for (int j = 0; j < al.size(); j++) {
				ClientThread ct2 = al.get(j);
				list = emsg.encryptMessage((j + 1) + ") " + ct2.username + " since " + ct2.date, ct.key, ct.N);
				encryptedMsg enc2 = new encryptedMsg(Constants.WHOISIN, list);
				if (!ct.writeMsg(enc2)) {
					al.remove(i);
					display("Disconnected Client " + ct.username + " removed from list.");
				}
			}
			list = emsg.encryptMessage("Last", ct.key, ct.N);
			enc = new encryptedMsg(Constants.WHOISINLAST, list);
			ct.writeMsg(enc);
		}
	}

	/*
	 * to broadcast a message to all Clients
	 */
	private synchronized void broadcast(int type, String message) {
		// add HH:mm:ss and \n to the message
		String time = sdf.format(new Date());
		String messageLf = time + " " + message + "\n";
		// display message on console or GUI
		System.out.print(messageLf);

		// we loop in reverse order in case we would have to remove a Client
		// because it has disconnected
		for (int i = al.size(); --i >= 0;) {
			ClientThread ct = al.get(i);
			// try to write to the Client if it fails remove it from the list
			ArrayList<BigInteger> list = emsg.encryptMessage(message, ct.key, ct.N);
			encryptedMsg enc = new encryptedMsg(type, list);
			if (!ct.writeMsg(enc)) {
				al.remove(i);
				display("Disconnected Client " + ct.username + " removed from list.");
			}
		}
	}

	/*
	 * to send private message to particular client
	 */
	private synchronized void sendPrivate(String recipient, String sender, int type, String message) {
		// add HH:mm:ss and \n to the message
		String time = sdf.format(new Date());
		String messageLf = time + " " + message + "\n";
		display(message);
		// we loop in reverse order in case we would have to remove a Client
		// because it has disconnected
		for (int i = al.size() - 1; i >= 0; i--) {
			ClientThread ct = al.get(i);
			if (ct.username.equals(recipient) || ct.username.equals(sender)) {
				ArrayList<BigInteger> list = emsg.encryptMessage(message, ct.key, ct.N);
				encryptedMsg enc = new encryptedMsg(type, list);
				// try to write to the Client if it fails remove it from the list
				display("Sending message from " + sender + " to " + ct.username);
				if (!ct.writeMsg(enc)) {
					al.remove(i);
					display("Disconnected Client " + ct.username + " removed from list.");
				}
			}
		}
	}

	// for a client who logoff using the LOGOUT message
	synchronized void remove(int id) {
		// scan the array list until we found the Id
		for (int i = 0; i < al.size(); i++) {
			ClientThread ct = al.get(i);
			// found it
			if (ct.id == id) {
				al.remove(i);
				return;
			}
		}
	}

	/*
	 * To run as the console application just open a console window and: > java
	 * Server > java Server portNumber If the port number is not specified 1500 is
	 * used
	 */
	public static void main(String[] args) {
		// start server on port 7966 unless a PortNumber is specified
		int portNumber = 7966;
		switch (args.length) {
		case 1:
			try {
				portNumber = Integer.parseInt(args[0]);
			} catch (Exception e) {
				System.out.println("Invalid port number.");
				System.out.println("Usage is: > java Server [portNumber]");
				return;
			}
		case 0:
			break;
		default:
			System.out.println("Usage is: > java Server [portNumber]");
			return;

		}
		// create a server object and start it
		Server server = new Server(portNumber);
		server.start();
	}

	/** One instance of this thread will run for each client */
	class ClientThread extends Thread {
		// the socket where to listen/talk
		Socket socket;
		ObjectInputStream sInput;
		ObjectOutputStream sOutput;
		long key, N;
		// my unique id (easier for disconnection)
		int id;
		// the user name of the Client
		String username;

		encryptedMsg cm;
		// the date I connect
		String date;

		// Constructor
		ClientThread(Socket socket) {
			// a unique id
			id = ++uniqueId;
			this.socket = socket;
			/* Creating both Data Stream */
			System.out.println("Thread trying to create Object Input/Output Streams");
			try {
				// create output first
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput = new ObjectInputStream(socket.getInputStream());

				// read the user name
				username = (String) sInput.readObject();
				display(username + " just connected.");
			} catch (IOException e) {
				display("Exception creating new Input/output Streams: " + e);
				return;
			}
			// have to catch ClassNotFoundException
			// but I read a String, I am sure it will work
			catch (ClassNotFoundException e) {
			}
			date = new Date().toString() + "\n";
		}

		public long getKey() {

			long result;
			try {
				Message msg = (Message) sInput.readObject();
				if (msg.getType() == Constants.KEY) {
					result = Long.parseLong(msg.getMessage());
					return result;
				} else
					return -1;
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
				return -1;
			}

		}

		public void sendKey(long key) {
			try {
				String strKey = Long.toString(key);
				sOutput.writeObject(new Message(Constants.KEY, strKey));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public long getN() {

			long result;
			try {
				Message msg = (Message) sInput.readObject();
				if (msg.getType() == Constants.N) {
					result = Long.parseLong(msg.getMessage());
					return result;
				} else
					return -1;
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
				return -1;
			}

		}

		public void sendN(long N) {
			try {
				String strN = Long.toString(N);
				sOutput.writeObject(new Message(Constants.N, strN));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public String getUserName() {
			return username;
		}

		// what will run forever
		public void run() {
			// to loop until LOGOUT
			key = getKey();
			sendKey(emsg.getPublicKey());
			N = getN();
			sendN(emsg.getN());
			listUsers();
			boolean keepGoing = true;
			while (keepGoing) {
				// read a String (which is an object)
				try {
					cm = (encryptedMsg) sInput.readObject();
				} catch (IOException e) {
					display(username + " Exception reading Streams: " + e);
					break;
				} catch (ClassNotFoundException e2) {
					break;
				}
				// the message part of the ChatMessage
				String message = emsg.decryptMessage(cm.getMessage());

				// Switch on the type of message receive
				switch (cm.getType()) {

				case Constants.BROADCASTMESSAGE:
					broadcast(cm.getType(), username + ": " + message);
					break;
				case Constants.PRIVATEMESSAGE:
					String[] parts = message.split(":", 2);
					sendPrivate(parts[0], username, cm.getType(), username + ": " + parts[1]);
					break;
				case Constants.LOGOUT:
					display(username + " disconnected with a LOGOUT message.");
					keepGoing = false;
					break;
				case Constants.WHOISIN:
					ArrayList<BigInteger> list = emsg.encryptMessage(
							"List of the users connected at " + sdf.format(new Date()) + "\n", key, N);
					encryptedMsg enc = new encryptedMsg(Constants.WHOISINFIRST, list);
					writeMsg(enc);
					// scan al the users connected
					for (int j = 0; j < al.size(); j++) {
						ClientThread ct2 = al.get(j);
						list = emsg.encryptMessage((j + 1) + ") " + ct2.username + " since " + ct2.date, key, N);
						encryptedMsg enc2 = new encryptedMsg(Constants.WHOISIN, list);
						writeMsg(enc2);
					}
					list = emsg.encryptMessage("Last", key, N);
					enc = new encryptedMsg(Constants.WHOISINLAST, list);
					writeMsg(enc);
					break;
				}
			}
			// remove myself from the arrayList containing the list of the
			// connected Clients
			remove(id);
			listUsers();
			close();
		}

		// try to close everything
		private void close() {
			// try to close the connection
			try {
				if (sOutput != null)
					sOutput.close();
			} catch (Exception e) {
			}
			try {
				if (sInput != null)
					sInput.close();
			} catch (Exception e) {
			}
			;
			try {
				if (socket != null)
					socket.close();
			} catch (Exception e) {
			}
		}

		/*
		 * Write a String to the Client output stream
		 */
		private boolean writeMsg(encryptedMsg msg) {
			// if Client is still connected send the message to it
			if (!socket.isConnected()) {
				close();
				return false;
			}
			// write the message to the stream
			try {
				sOutput.writeObject(msg);
			}
			// if an error occurs, do not abort just inform the user
			catch (IOException e) {
				display("Error sending message to " + username);
				display(e.toString());
			}
			return true;
		}
	}
}