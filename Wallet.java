package BabyBlockchain;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Wallet {
	public PrivateKey privateKey;
	public PublicKey publicKey;

	public HashMap<String, TransactionOutput> UTXOs = new HashMap<String, TransactionOutput>(); // only UTXOs owned by
																								// this wallet.

	public Wallet() {
		generateKeyPair();
	}

	public void generateKeyPair() {
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1");
			// Initialize the key generator and generate a KeyPair
			keyGen.initialize(ecSpec, random); // 256 bytes provides an acceptable security level
			KeyPair keyPair = keyGen.generateKeyPair();
			// Set the public and private keys from the keyPair
			privateKey = keyPair.getPrivate();
			publicKey = keyPair.getPublic();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// returns balance and stores the UTXOs owned by this wallet in this.UTXOs
	public float getBalance() {
		float total = 0;
		for (Map.Entry<String, TransactionOutput> item : BabyBlockchain.UTXOs.entrySet()) { // iterate over entries in
																							// the hashmap
			TransactionOutput UTXO = item.getValue();
			if (UTXO.isMine(publicKey)) { // if output (coins) belong to me
				UTXOs.put(UTXO.id, UTXO); // add the Unspent Transaction Output to our list of unspent transactions.
				total += UTXO.value;
			}
		}
		return total;
	}

	// Generates an Returns a new transaction from this wallet.
	public Transaction sendFunds(PublicKey _recipient, float value) {
		if (getBalance() < value) { // make sure we have funds available
			System.out.println("#Not enough funds to send transaction. Transaction discarded.");
			return null;
		}
		// create ArrayList of inputs
		ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();

		float total = 0;
		for (Map.Entry<String, TransactionOutput> item : UTXOs.entrySet()) { // iterate over the UTXOs in the hashmap
			TransactionOutput UTXO = item.getValue(); // get the "value" part of the key, value pair.
			total += UTXO.value; // add the monetary "value" to the total
			inputs.add(new TransactionInput(UTXO.id)); // add this UTXO as part of the inputs required for this transaction
			if (total > value)
				break; // loop until the total amount being transfered is greater than or equal to the
						// amount needed. If there is an excess, the Transaction will return the change to us as a new UTXO
		}
		
		Transaction newTransaction = new Transaction (publicKey, _recipient, value, inputs);
		newTransaction.generateSignature(privateKey);
		
		for(TransactionInput input: inputs) {
			UTXOs.remove(input.transactionOutputId);
		}
		return newTransaction;
		
	}

}
