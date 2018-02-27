package BabyBlockchain;

import java.security.Security;
import java.util.ArrayList;
import java.util.Base64;
import com.google.gson.GsonBuilder;
import java.util.HashMap;

public class BabyBlockchain {

	public static int difficulty = 5;
	public static float minimumTransaction = 0.1f;
	public static ArrayList<Block> blockchain = new ArrayList<Block>();
	public static HashMap<String,TransactionOutput> UTXOs = new HashMap<String, TransactionOutput>(); // list of all unspent transactions
	public static Wallet walletA;
	public static Wallet walletB;
	public static Transaction genesisTransaction;
	
	public static void main(String[] args) {
		//Setup Bouncey castle as a Security Provider
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider()); 
		
		// create two wallets
		walletA = new Wallet();
		walletB = new Wallet();
		Wallet coinbase = new Wallet();
		
		// create genesis transaction, which sends 100 BabyCoins to walletA:
		genesisTransaction = new Transaction(coinbase.publicKey, walletA.publicKey, 100f, null);
		genesisTransaction.generateSignature(coinbase.privateKey); //manually sign the genesis transaction	
		genesisTransaction.transactionId = "0"; //manually set the transaction id
		genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.recipient, genesisTransaction.value, genesisTransaction.transactionId)); //manually add the Transactions Output
		UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0)); //its important to store our first transaction in the UTXOs list.
		
		System.out.println("Creating and Mining Genesis block... ");
		Block genesis = new Block("0");
		genesis.addTransaction(genesisTransaction);
		addBlock(genesis);
		
		sendFunds(walletA, walletB, 40f);
		sendFunds(walletA, walletB, 1000f);
		sendFunds(walletB, walletA, 20f);
			
		System.out.println("\nChecking validity: " + isChainValid());
		
		String blockchainJson = new GsonBuilder().setPrettyPrinting().create().toJson(blockchain);
		System.out.println("\nThe block chain: ");
		System.out.println(blockchainJson);
	}
	
	public static boolean sendFunds( Wallet sender, Wallet recipient, float amount ) {
		Block theBlock = new Block(blockchain.get(blockchain.size() - 1).hash);
		System.out.println("\nSender's balance is: " + sender.getBalance());
		System.out.println("\nSender is Attempting to send funds (" + amount + ") to WalletB...");
		theBlock.addTransaction(sender.sendFunds(recipient.publicKey, amount));
		addBlock(theBlock);
		System.out.println("\nSender's balance is: " + sender.getBalance());
		System.out.println("Recipient's balance is: " + recipient.getBalance());
		return true;
	}

	public static void addBlock(Block newBlock) {
		newBlock.mineBlock(difficulty);
		blockchain.add(newBlock);
	}
	
	public static Boolean isChainValid() {
		Block currentBlock;
		Block previousBlock;
		String hashTarget = new String(new char[difficulty]).replace('\0', '0');
		HashMap<String,TransactionOutput> tempUTXOs = new HashMap<String,TransactionOutput>(); //a temporary working list of unspent transactions at a given block state.
		tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));
		
		//loop through blockchain to check hashes:
		for (int i =  1; i < blockchain.size(); i++) {
			currentBlock = blockchain.get(i);
			previousBlock = blockchain.get(i - 1);
			// compare regular hash and calculated hash
			if ( !currentBlock.hash.equals(currentBlock.calculateHash()) ) {
				System.out.println("Current Hashes not equal"); 
				return false;
			}
			// compare regular hash and calculated hash
			if ( !previousBlock.hash.equals(currentBlock.previousHash) ) {
				System.out.println("Previous Hashes not equal"); 
				return false;
			}
			// check if hash is solved
			if( !currentBlock.hash.substring( 0, difficulty ).equals(hashTarget) ) {
				System.out.println("This block hasn't been mined");
				return false;
			}
			//loop thru blockchains transactions:
			TransactionOutput tempOutput;
			for(int t=0; t <currentBlock.transactions.size(); t++) {
				Transaction currentTransaction = currentBlock.transactions.get(t);
				
				if(!currentTransaction.verifySignature()) {
					System.out.println("#Signature on Transaction(" + t + ") is Invalid");
					return false; 
				}
				if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
					System.out.println("#Inputs are note equal to outputs on Transaction(" + t + ")");
					return false; 
				}
				
				for(TransactionInput input: currentTransaction.inputs) {	
					tempOutput = tempUTXOs.get(input.transactionOutputId);
					
					if(tempOutput == null) {
						System.out.println("#Referenced input on Transaction(" + t + ") is Missing");
						return false;
					}
					
					if(input.UTXO.value != tempOutput.value) {
						System.out.println("#Referenced input Transaction(" + t + ") value is Invalid");
						return false;
					}
					
					tempUTXOs.remove(input.transactionOutputId);
				}
				
				for(TransactionOutput output: currentTransaction.outputs) {
					tempUTXOs.put(output.id, output);
				}
				
				if( (currentTransaction.outputs.get(0)).recipient != currentTransaction.recipient) {
					System.out.println("#Transaction(" + t + ") output reciepient is not who it should be");
					return false;
				}
				if( currentTransaction.outputs.get(1).recipient != currentTransaction.sender) {
					System.out.println("#Transaction(" + t + ") output 'change' is not sender.");
					return false;
				}
				
			}
			
		}
		System.out.println("Blockchain is valid");
		return true;
	}
}