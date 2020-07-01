package client;

import java.net.*;
import java.io.*;
import java.math.BigInteger;
import java.util.*;
import common.*;
//To run program run server then run x amount of clientMenus for x amount of people
/*
 * The Client class implements the communication with the server
 */
public class Client extends Observable {

	// for I/O
	private ObjectInputStream sInput; // to read from the socket
	private ObjectOutputStream sOutput; // to write on the socket
	private Socket socket;
	private RSA emsg;
	long key, N;

	// the server, the port and the user name
	private String server, username;
	private int port;

	/*
	 * Constructor call when used from a GUI in console mode the ClienGUI parameter
	 * is null
	 */
	public Client() {
		emsg = new RSA(true);

	}

	public void connect(String server, int port, String username) {
		this.server = server;
		this.port = port;
		this.username = username;
		// save if we are in GUI mode or not
	}

	/*
	 * To start the dialog
	 */
	public boolean start() {
		// try to connect to the server
		try {
			socket = new Socket(server, port);
		}
		// if it failed not much I can so
		catch (Exception ec) {
			display(Constants.ERROR, "Error connectiong to server:" + ec);
			return false;
		}
		display(Constants.MESSAGE, "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort() + "\n");

		/* Creating both Data Stream */
		try {
			sInput = new ObjectInputStream(socket.getInputStream());
			sOutput = new ObjectOutputStream(socket.getOutputStream());

		} catch (IOException eIO) {
			display(Constants.ERROR, "Exception creating new Input/output Streams: " + eIO);
			return false;
		}

		// Send our user name to the server this is the only message that we
		// will send as a String. All other messages will be ChatMessage objects
		try {
			sOutput.writeObject(username);
			// creates the Thread to listen from the server
			new ListenFromServer().start();
		} catch (IOException eIO) {
			display(Constants.ERROR, "Exception doing login : " + eIO);
			disconnect();
			return false;
		}
		// success we inform the caller that it worked
		return true;
	}

	/*
	 * To send a message to the console or the GUI
	 */
	private void display(int type, String msg) {
		setChanged();
		notifyObservers(new Message(type, msg));
	}

	/*
	 * To send a message to the server
	 */

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

	public void sendMessage(Message msg) {
		try {
			ArrayList<BigInteger> list = emsg.encryptMessage(msg.getMessage(), key, N);
			encryptedMsg enc = new encryptedMsg(msg.getType(), list);
			sOutput.writeObject(enc);
		} catch (IOException e) {
			display(Constants.ERROR, "Exception writing to server: " + e);
		}
	}

	/*
	 * When something goes wrong Close the Input/Output streams and disconnect
	 */
	private void disconnect() {
		try {
			if (sInput != null)
				sInput.close();
		} catch (Exception e) {
		} // not much else I can do
		try {
			if (sOutput != null)
				sOutput.close();
		} catch (Exception e) {
		} // not much else I can do
		try {
			if (socket != null)
				socket.close();
		} catch (Exception e) {
		} // not much else I can do

		// inform the GUI
		setChanged();
		notifyObservers(null);

	}

	/*
	 * a class that waits for the message from the server and append them to the
	 * JTextArea if we have a GUI or simply System.out.println() it in console mode
	 */
	class ListenFromServer extends Thread {
		public void run() {
			sendKey(emsg.getPublicKey());
			key = getKey();
			sendN(emsg.getN());
			N = getN();
			while (true) {
				try {
					encryptedMsg msg = (encryptedMsg) sInput.readObject();
					String message = emsg.decryptMessage(msg.getMessage());
					switch (msg.getType()) {
					case Constants.BROADCASTMESSAGE:
					case Constants.PRIVATEMESSAGE:
						display(Constants.MESSAGE, message);
						break;
					case Constants.WHOISINFIRST:
						display(Constants.USERLISTSTART, message);
						break;
					case Constants.WHOISIN:
						display(Constants.USERLIST, message);
						break;
					case Constants.WHOISINLAST:
						display(Constants.USERLISTEND, message);
						break;
					}
				} catch (IOException e) {
					display(Constants.ERROR, "Server has closed the connection: " + e);
					setChanged();
					notifyObservers(null);
					break;
				}
				// can't happen with a String object but need the catch anyhow
				catch (ClassNotFoundException e2) {
				}
			}
		}
	}
}
