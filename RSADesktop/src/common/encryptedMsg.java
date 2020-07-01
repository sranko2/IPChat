package common;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
//To run program run server then run x amount of clientMenus for x amount of people
/*
* This class defines the different type of messages that will be exchanged between the
* Clients and the Server.
* When talking from a Java Client to a Java Server a lot easier to pass Java objects, no
* need to count bytes or to wait for a line feed at the end of the frame
* */

public class encryptedMsg implements Serializable {

		protected static final long serialVersionUID = 1112122200L;

		private int type;
		private ArrayList<BigInteger> message;

		// constructor
		public encryptedMsg(int type, ArrayList<BigInteger> message) {
			this.type = type;
			this.message = message;
		}

		// getters
		public int getType() {
			return type;
		}

		public ArrayList<BigInteger> getMessage() {
			return message;
		}
}
