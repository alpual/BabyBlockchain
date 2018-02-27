package BabyBlockchain;

import java.util.ArrayList;
import java.util.Date;

public class Block {

	public String hash;
	public String previousHash;
	public String merkleRoot;
	public ArrayList<Transaction> transactions = new ArrayList<Transaction>(); // our data will be a simple message.
	private long timestamp;
	private int nonce;

	// Block constructor
	public Block(String previousHash) {
		this.previousHash = previousHash;
		this.timestamp = new Date().getTime();
		this.hash = calculateHash(); // Making sure we do this after we set the other values.
	}

	public String calculateHash() {
		String calculatedHash = StringUtil
				.applySha256(previousHash + Long.toString(timestamp) + Integer.toString(nonce) + merkleRoot);
		return calculatedHash;
	}

	public void mineBlock(int difficulty) {
		merkleRoot = StringUtil.getMerkleRoot(transactions);

		String target = new String(new char[difficulty]).replace('\0', '0'); // Create a string with difficulty * "0"
		while (!hash.substring(0, difficulty).equals(target)) {
			nonce++;
			hash = calculateHash();
		}
		System.out.println("Block Mined!! : " + hash);
	}

	// Add transactions to this block{
	public boolean addTransaction(Transaction transaction) {
		// process transaction and check if valid, unless block is genesis block, then ignore
		if (transaction == null ) return false;
		
		if(previousHash != "0" && transaction.processTransaction() != true)  {
			// fail if transaction is not processed correctly.  skip if this is the genesis block 
			System.out.println("#Transaction failed to process.  Discarded");
			return false;
		}
		transactions.add(transaction);
		System.out.println("Transaction successfully added to Block");
		return true;
	}
}
