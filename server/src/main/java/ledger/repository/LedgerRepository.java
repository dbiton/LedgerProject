package ledger.repository;

import ledger.repository.model.Transaction;
import ledger.repository.model.UTxO;
import ledger.repository.model.Transfer;

import java.math.BigInteger;
import java.util.List;
import java.util.ArrayList;


public class LedgerRepository {
    List<Transaction> transactions;
    List<UTxO> UTxOs;

    public LedgerRepository() {
        this.transactions = new ArrayList<>();
        this.UTxOs = new ArrayList<>();
    }

    // returns UTxOs with total values above coins, or an empty list if we don't have enough
    public ArrayList<UTxO> consumeUTxOs(BigInteger address, long coins) {
        long coins_curr = 0;
        ArrayList<UTxO> us = new ArrayList<>();
        for (UTxO u : this.UTxOs) {
            if (u.getAddress().equals(address)) {
                us.add(u);
                coins_curr += u.getCoins();
                if (coins_curr >= coins) {
                    break;
                }
            }
        }
        // not enough coins to consume
        if (coins_curr < coins) {
            return new ArrayList<>();
        }
        for (UTxO u : us) {
            this.UTxOs.remove(u);
        }
        return us;
    }

    public void submitTransfer(Transfer transfer, BigInteger transaction_id) {
        UTxO u = new UTxO(transaction_id, transfer.getAddress());
        u.setCoins(transfer.getCoins());
        UTxOs.add(u);
    }

    public ArrayList<UTxO> getUTxOs(BigInteger address) {
        ArrayList<UTxO> us = new ArrayList<>();
        for (UTxO u : this.UTxOs) {
            if (u.getAddress().equals(address)) {
                us.add(u);
            }
        }
        return us;
    }

    public ArrayList<Transaction> getAllTransactions(long max) {
        ArrayList<Transaction> transactions = new ArrayList<>();
        long counter = 0;
        for (Transaction transaction : this.transactions) {
            transactions.add(transaction);
            counter += 1;
            if (max > 0 && counter == max) {
                break;
            }
        }
        return transactions;
    }

    public ArrayList<Transaction> getTransactions(BigInteger address, long max) {
        ArrayList<Transaction> transactions = new ArrayList<>();
        long counter = 0;
        for (Transaction transaction : this.transactions) {
            if (transaction.getInputAddress().equals(address)) {
                transactions.add(transaction);
                counter += 1;
                if (max > 0 && counter == max) {
                    break;
                }
            }
        }
        return transactions;
    }

    public long getCoinsTransfer(BigInteger transaction_id, BigInteger address){
        for (Transaction transaction : transactions){
            if (transaction.getId().equals(transaction_id)){
                for (Transfer transfer : transaction.getOutputs()){
                    if (transfer.getAddress().equals(address)){
                        return transfer.getCoins();
                    }
                }
                return 0;
            }
        }
        return 0;
    }

    public void submitTransaction(Transaction transaction){
        // consume UTxOs
        for (UTxO u : transaction.getInputs()){
            this.UTxOs.remove(u);
        }
        // add transaction
        transactions.add(transaction);
    }
}