package view;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import client.Client;

import java.awt.*;
import java.awt.event.*;
import common.*;

import java.util.Observable;
import java.util.Observer;
//To run program run server then run x amount of clientMenus for x amount of people
/*
 * The Client GUI
 */
public class ClientMenu extends JFrame implements ActionListener, Observer, ListSelectionListener {

	private static final long serialVersionUID = 1L;
	// will first hold "Username:", later on "Enter message"
	private JLabel label, users_label;
	// to hold the list of connected users
	private JList<String> userList;
	private DefaultListModel<String> userListModel;
	// to hold the Username and later on the messages
	private JTextField tf, un;
	// to hold the server address an the port number
	private JTextField tfServer, tfPort;
	// to Logout and get the list of the users
	private JButton login, logout, send;
	// for the chat room
	private JTextArea ta;
	// if it is for connection
	private boolean connected;
	// the default port number
	private int defaultPort;
	private Client client;
	private String defaultHost;

	private String recipient = "";
	private String mySelf = "";
	
	// Constructor connection receiving a socket number
	public ClientMenu(String host, int port, Client c) {

		super("Chat Client");
		defaultPort = port;
		defaultHost = host;
		client = c;

		// The NorthPanel with:
		JPanel northPanel = new JPanel(new GridLayout(3, 1));
		// the server name and the port number
		JPanel serverAndPort = new JPanel(new GridLayout(1, 5, 1, 3));
		// the two JTextField with default value for server address and port number
		tfServer = new JTextField(host);
		tfPort = new JTextField("" + port);
		tfPort.setHorizontalAlignment(SwingConstants.RIGHT);

		serverAndPort.add(new JLabel("Server Address:  "));
		serverAndPort.add(tfServer);
		serverAndPort.add(new JLabel("Port Number:  "));
		serverAndPort.add(tfPort);
		// serverAndPort.add(new JLabel(""));
		// adds the Server an port field to the GUI
		// northPanel.add(serverAndPort);

		// the Label and the TextField
		label = new JLabel("Username:", SwingConstants.CENTER);
		serverAndPort.add(label);
		un = new JTextField("Anonymous");
		tf = new JTextField("");
		tf.setBackground(Color.WHITE);
		un.setBackground(Color.WHITE);
		serverAndPort.add(un);
		northPanel.add(serverAndPort);

		add(northPanel, BorderLayout.NORTH);

		// The CenterPanel which is the chat room
		ta = new JTextArea("Welcome to the Chat room\n", 80, 80);
		JPanel centerPanel = new JPanel(new GridLayout(1, 1));
		centerPanel.add(new JScrollPane(ta));
		ta.setEditable(false);
		add(centerPanel, BorderLayout.CENTER);

		// the 3 buttons
		login = new JButton("Connect");
		login.setActionCommand("Connect");
		login.addActionListener(this);
		send = new JButton("Send");
		send.setActionCommand("Send");
		send.addActionListener(this);
		// logout = new JButton("Logout");
		// logout.addActionListener(this);
		// logout.setEnabled(false); // you have to login before being able to logout

		JPanel southPanel = new JPanel(new GridLayout(1, 3, 5, 5));
		serverAndPort.add(login);
		// southPanel.add(logout);
		southPanel.add(tf);
		southPanel.add(send);
		add(southPanel, BorderLayout.SOUTH);

		userListModel = new DefaultListModel<String>();
		userList = new JList<String>(userListModel);
		userList.setPreferredSize(new Dimension(100, 400));
		userList.addListSelectionListener(this);
		users_label = new JLabel("Users:");

		JPanel eastPanel = new JPanel();
		eastPanel.setPreferredSize(new Dimension(100, 50));
		users_label.setPreferredSize(new Dimension(100, 20));
		eastPanel.add(users_label);
		eastPanel.add(userList);
		add(eastPanel, BorderLayout.EAST);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(600, 600);
		setVisible(true);
		tf.requestFocus();

	}

	// called by the Client to append text in the TextArea
	public void append(String str) {
		ta.append(str);
		ta.setCaretPosition(ta.getText().length() - 1);
	}

	// called by the Client to append text in the TextArea
	public void appendUser(String str) {
		DefaultListModel<String> listModel = (DefaultListModel<String>) userList.getModel();
		listModel.addElement(str);
	}

	// called by the GUI is the connection failed
	// we reset our buttons, label, text field
	public void connectionFailed() {
		login.setEnabled(true);
		// logout.setEnabled(false);
		// label.setText("Enter your username below");
		un.setText("Anonymous");
		// reset port number and host name as a construction time
		tfPort.setText("" + defaultPort);
		tfServer.setText(defaultHost);
		// let the user change them
		tfServer.setEditable(false);
		tfPort.setEditable(false);
		// don't react to a <CR> after the username
		un.removeActionListener(this);
		connected = false;
	}

	/*
	 * Button or JTextField clicked
	 */
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		// if it is the Logout button
		if (o == logout) {
			client.sendMessage(new Message(Constants.LOGOUT, ""));
			return;
		}
		if (e.getActionCommand().equals("Disconnect")) {
			client.sendMessage(new Message(Constants.LOGOUT, ""));
			login.setActionCommand("Connect");
			login.setText("Connect");
			return;
		}

		if (o == send || o == tf) {
			String msg = tf.getText();
			if (recipient.equals("")) {
				client.sendMessage(new Message(Constants.BROADCASTMESSAGE, msg + "\n"));
			} else {
				client.sendMessage(new Message(Constants.PRIVATEMESSAGE, recipient + ":" + msg + "\n"));
			}
			tf.setText("");
		}

		if (o == login) {
			// ok it is a connection request
			String username = un.getText().trim();
			mySelf = username;
			// empty username ignore it
			if (username.length() == 0)
				return;
			// empty serverAddress ignore it
			String server = tfServer.getText().trim();
			if (server.length() == 0)
				return;
			// empty or invalid port numer, ignore it
			String portNumber = tfPort.getText().trim();
			if (portNumber.length() == 0)
				return;
			int port = 0;
			try {
				port = Integer.parseInt(portNumber);
			} catch (Exception en) {
				return; // nothing I can do if port number is not valid
			}

			client.connect(server, port, username);
			if (!client.start()) {
				mySelf = "";
				return;
			}
			tf.setText("");
			// disable login button
			// login.setEnabled(false);
			login.setText("Disconnect");
			login.setActionCommand("Disconnect");
			// enable the 2 buttons
			// logout.setEnabled(true);
			// disable the Server and Port JTextField
			tfServer.setEditable(false);
			tfPort.setEditable(false);
			// Action listener for when the user enter a message
			tf.addActionListener(this);
		}
	}

	public void update(Observable o, Object arg) {
		if (arg == null)
			connectionFailed();
		else {
			Message msg = (Message) arg;
			switch (msg.getType()) {
			case Constants.MESSAGE:
			case Constants.ERROR:
				append(msg.getMessage());
				break;
			case Constants.USERLISTSTART:
				userListModel = new DefaultListModel<String>();
				userListModel.addElement("Broadcast to all");
				break;
			case Constants.USERLIST:
				userListModel.addElement(msg.getMessage());
				break;
			case Constants.USERLISTEND:
				userList.setModel(userListModel);
				userList.setSelectedIndex(0);
				break;
			}
		}
	}

	// to start the whole thing the server
	public static void main(String[] args) {
		Client client = new Client();
		ClientMenu cm = new ClientMenu("localhost", 7966, client);
		client.addObserver(cm);
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (userList.getSelectedValue() == null) {
			recipient = "";
			return;
		}
		String value = userList.getSelectedValue().toString();
		if (value.equals("Broadcast to all")) {
			recipient = "";
			return;
		}
		String[] parts = value.split(" ", 3);
		this.recipient = parts[1];
		if (recipient.equals(mySelf))
			recipient = "";
	}

}
