package common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JOptionPane;
//To run program run server then run x amount of clientMenus for x amount of people
public class RSA {
	
	private long p;
	private long q;
	
	private long e;
	private long d;
	private long n;
	private long phi;
	
	int blockSize = 4;
	
	
	public RSA(boolean generatePrimes) {
		
		if (generatePrimes) {
			try {
				generatePrimes();
			} catch (IOException e) {
				System.out.println("primeNumbers file does not exist. Exiting...");
				System.exit(0);
			}
		}
		else {
			
			promptPrimes();
			
			while ((p * q) < Math.pow(128, blockSize) || (!isPrime(p) && isPrime(q))) {
				if ((p * q) < Math.pow(128, blockSize)) {
					JOptionPane.showMessageDialog(null, "Invalid primes.\n"
													  + "p * q > 128^" + blockSize + " must be true.");
				}
				if (!isPrime(p) && isPrime(q)) {
					JOptionPane.showMessageDialog(null, "Invalid primes. Both numbers must be prime.");
				}
				promptPrimes();
			}
			
		}
		
		calculateN();
		calculatePhi();
		calculateE();
		calculateD();
	}
	
	//
	// This main method for stand-alone testing purposes
	//
/*	
	public static void main(String[] args) {
		
		RSA RSAtest = new RSA(true);
		
		
		ArrayList<BigInteger> blocked_msg = RSAtest.encryptMessage("Meet me outside SCE at 10pm.");
			
		//
		// DEBUG
		//
		System.out.println("p = " + RSAtest.p);
		System.out.println("q = " + RSAtest.q);
		System.out.println("n = " + RSAtest.n);
		System.out.println("phi = " + RSAtest.phi);
		System.out.println("e = " + RSAtest.e);
		System.out.println("d = " + RSAtest.d);
		System.out.println();
					
		String result = RSAtest.decryptMessage(blocked_msg);
		System.out.println("result: ");
		System.out.println(result);
	}
	
*/	
	
	
	// These two functions, getPublicKey and getN, combined get the pair representing
	// the public key for this RSA instance.
	public long getPublicKey() {
		return this.e;
	}
	
	public long getN() {
		return this.n;
	}
	
	public ArrayList<BigInteger> encryptMessage(String msg, long key, long N) {
		
		ArrayList<Long> blocked_msg = blockMessage(msg);
		ArrayList<BigInteger> encrypted_msgs = new ArrayList<BigInteger>();
		
		for (Long block : blocked_msg) {
			encrypted_msgs.add(encrypt(block, key, N));
		}
		
		return encrypted_msgs;
	}
	
	public String decryptMessage(ArrayList<BigInteger> encrypted_msgs) {
		ArrayList<Long> decrypted_msgs = new ArrayList<Long>();
		
		for (BigInteger msg : encrypted_msgs) {
			decrypted_msgs.add(decrypt(msg).longValue());
		}
		
		String result = deblockMessage(decrypted_msgs);
		return result;
	}
	
	private void generatePrimes() throws IOException {
		
		String line;
		Random rand = new Random();
		int randomLine1;
		int randomLine2;
		int lineCount;
		
		// Count lines in the prime numbers file by iterating to the end
		BufferedReader primesIn = new BufferedReader(new FileReader(".\\primeNumbers.txt"));
		
		lineCount = 0;
		while ((line = primesIn.readLine()) != null) {
			lineCount++;
		}		
		
		primesIn.close();
		
		
		// Generate a random line number to read
		randomLine1 = Math.abs(rand.nextInt()) % lineCount;
		randomLine2 = Math.abs(rand.nextInt()) % lineCount;
		
		if (randomLine1 == randomLine2) // If lines are equal, use the next line
			randomLine2++;
		
		// Read to that line and store the corresponding prime number
		primesIn = new BufferedReader(new FileReader(".\\primeNumbers.txt"));
		
		// Determine which line is further down the file
		int furthestLine;
		if (randomLine1 > randomLine2)
			furthestLine = randomLine1;
		else
			furthestLine = randomLine2;
		
		// Read the primes at the randomly generated lines
		for (int i = 0; i <= furthestLine; i++) {
			line = primesIn.readLine();
			
			if (i == randomLine1) {
				this.p = Long.parseLong(line);
			}
			else if (i == randomLine2) {
				this.q = Long.parseLong(line);
			}
			
		}		
		
		primesIn.close();
	}
	
	private void promptPrimes() {
		String p = JOptionPane.showInputDialog("Enter first prime (p):");
		String q = JOptionPane.showInputDialog("Enter second prime (q):");
		
		this.p = Integer.parseInt(p);
		this.q = Integer.parseInt(q);
	}
	
	private void calculateN() {
		this.n= this.p * this.q;
	}
	
	private void calculatePhi() {
		this.phi = (this.p - 1) * (this.q - 1);
	}
	
	private void calculateE() {
		if (gcd(3, phi) == 1) { this.e = 3; }
		else if (gcd(5, phi) == 1) { this.e = 5; }
		else if (gcd(17, phi) == 1) { this.e = 17; }
		else if (gcd(257, phi) == 1) { this.e = 257; }
		else {
			System.out.println("Error: e greater than 257. Exiting.");
			System.exit(0);
		}
	}
	
	private void calculateD() {
		long u = this.e;
		long v = this.phi;
		
		long inv, u1, u3, v1, v3, t1, t3, q;
	    int iter;
	    /* Step X1. Initialize */
	    u1 = 1;
	    u3 = u;
	    v1 = 0;
	    v3 = v;
	    /* Remember odd/even iterations */
	    iter = 1;
	    /* Step X2. Loop while v3 != 0 */
	    while (v3 != 0)
	    {
	        /* Step X3. Divide and "Subtract" */
	        q = u3 / v3;
	        t3 = u3 % v3;
	        t1 = u1 + q * v1;
	        /* Swap */
	        u1 = v1; v1 = t1; u3 = v3; v3 = t3;
	        iter = -iter;
	    }
	    /* Make sure u3 = gcd(u,v) == 1 */
	    if (u3 != 1) {
	        this.d = 0;   /* Error: No inverse exists */
	        System.out.println("Error: No inverse exists. Exiting.");
	        System.exit(0);
	    }
	    /* Ensure a positive result */
	    if (iter < 0)
	        inv = v - u1;
	    else
	        inv = u1;
	    
	    this.d = inv;
	}
	
	private long gcd(long a, long b) {
		if (b == 0) {
			return a;
		}
		
		return gcd(b, a % b);
	}
	
	private ArrayList<Long> blockMessage(String msg) {
		
		
		ArrayList<Long> blocked_msg  = new ArrayList<Long>();
		
		// if message is not a multiple of block size, add null characters until it is
		while (msg.length() % blockSize != 0) {
			msg += '\0';
		}
		
		int msg_length = msg.length();
		for (int i = 0; i < msg_length; i += 4) {
			
			char c1 = msg.charAt(i);
			char c2 = msg.charAt(i + 1);
			char c3 = msg.charAt(i + 2);
			char c4 = msg.charAt(i + 3);			
			
			long v1 = (long)c1;
			long v2 = ((long)c2 * 128);
			long v3 = ((long)c3 * 128 * 128);
			long v4 = ((long)c4 * 128 * 128 * 128);
			
			long block = v1 + v2 + v3 + v4;
			blocked_msg.add(block);
		}
		
		return blocked_msg;
	}
	
	private String deblockMessage(ArrayList<Long> blocked_msg) {
		
		String msg = new String();
		
		for (Long block : blocked_msg) {
			
			int mask = 0b1111111;
			
			char c1 = (char)(block & 127);
			char c2 = (char)((block >> 7) & 127);
			char c3 = (char)((block >> 14) & 127);
			char c4 = (char)((block >> 21) & 127);
			
			msg = msg + c1 + c2 + c3 + c4;
		}
		
		return msg;
	}
	
	private BigInteger encrypt(long msg, long key, long N) {
		BigInteger m = BigInteger.valueOf(msg);
		BigInteger e = BigInteger.valueOf(key);
		BigInteger n = BigInteger.valueOf(N);
		
		return m.modPow(e, n);
	}
	
	private BigInteger decrypt(BigInteger msg) {
		BigInteger d = BigInteger.valueOf(this.d);
		BigInteger n = BigInteger.valueOf(this.n);
		
		return msg.modPow(d, n);
	}
	
	private boolean isPrime(long a) {
		//
		// This method is from:
		// https://stackoverflow.com/questions/40199440/how-to-determine-if-a-number-is-prime
		//
		
		boolean prime = true;
	    for (int i = 2; i < a; i++) {
	        if (a % i == 0) {
	            prime = false;
	            break;
	        }
	    }
	    
	    return prime;
	}
	
}

















