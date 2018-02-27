package BabyBlockchain;

import java.security.*;
import java.util.ArrayList;

public class Transaction {
	public String transactionId;  // this is also the hash of the transaction.
	public PublicKey sender; // senders address/public key
	public PublicKey recipient; // recipient address/public key
	public float value;
	public byte[] signature; // to prevent anybody else from spending the funds
	
	public ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();
	public ArrayList<TransactionOutput> outputs = new ArrayList<TransactionOutput>();
	
	private static int sequence = 0; // a rough count of how many transactions have been generated. 

	//Constructor:
	public Transaction(PublicKey from, PublicKey to, float value, ArrayList<TransactionInput> inputs) {
		this.sender = from;
		this.recipient = to;
		this.value = value;
		this.inputs = inputs;
	}
	
	// This Calculates the transaction hash (which will be used as its Id)
	private String calculateHash() {
		sequence ++; //increase the sequence to avoid 2 identical transactions having the same hash
		return StringUtil.applySha256(
				StringUtil.getStringFromKey(sender) +
				StringUtil.getStringFromKey(recipient) +
				Float.toString(value) + sequence
				);
	}
	
	//Signs all the data we dont wish to be tampered with.
	public byte[] generateSignature(PrivateKey privateKey) {
		String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(recipient) + Float.toString(value);
		signature = StringUtil.applyECDSASig(privateKey, data);
		return signature;
	}
	//Verifies the data we signed hasnt been tampered with
	public boolean verifySignature() {
		String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(recipient) + Float.toString(value);
		return StringUtil.verifyECDSASig(sender, data, signature);		
	}
	
	public float getInputsValue() {
		float total = 0;
		for (TransactionInput i : inputs) {
			if (i.UTXO == null) continue; // if transaction can't be found in hash, continue
			total += i.UTXO.value;
		}
		return total;
	}
	public float getOutputsValue() {
		float total = 0;
		for(TransactionOutput o : outputs) {
			total += o.value;
		}
		return total;
	}	
	
	public boolean processTransaction() {
		if(verifySignature() == false) {
			System.out.print("#Transaction Signature failed to verify");
			return false;
		}
		
		// gather transaction inputs (make sure they are unspent)
		for (TransactionInput i : inputs) {
			i.UTXO = BabyBlockchain.UTXOs.get(i.transactionOutputId);
		}
		
		// check if transaction is valid:
		if(getInputsValue() < BabyBlockchain.minimumTransaction) {
			System.out.println("#Transaction Inputs too small: " + getInputsValue());
			return false;
		}
		
		// generate transaction outputs:
		float leftOver = getInputsValue() - value; 
		transactionId = calculateHash();
		outputs.add( new TransactionOutput( this.recipient, value, transactionId ) ); // send value to recipient
		outputs.add( new TransactionOutput( this.sender, leftOver, transactionId)); // give the sender the change)
		
		// remove transaction inputs from UTXO lists as spent:
		for (TransactionInput i : inputs) {
			if(i.UTXO == null) continue; // if Transaction can't be found, skip it.
			BabyBlockchain.UTXOs.remove(i.UTXO.id);
		}
		
		// add outputs to Unspent list
		for (TransactionOutput o : outputs) {
			BabyBlockchain.UTXOs.put(o.id, o);
		}
		
		return true;
	}
	
}
