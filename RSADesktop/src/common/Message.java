package common;
import java.io.*;
//To run program run server then run x amount of clientMenus for x amount of people
/*
* This class defines the different type of messages that will be exchanged between the
* Clients and the Server.
* When talking from a Java Client to a Java Server a lot easier to pass Java objects, no
* need to count bytes or to wait for a line feed at the end of the frame
*/
public class Message implements Serializable {

	protected static final long serialVersionUID = 1112122200L;

	// The different types of message sent by the Client
	// WHOISIN to receive the list of the users connected
	// BROADCASTMESSAGE a message sent to everybody
	// PRIVATEMESSAGE a message sent to a particular recipient
	// LOGOUT to disconnect from the Server
	private int type;
	private String message;

	// constructor
	public Message(int type, String message) {
		this.type = type;
		this.message = message;
	}

	// getters
	public int getType() {
		return type;
	}

	public String getMessage() {
		return message;
	}
}