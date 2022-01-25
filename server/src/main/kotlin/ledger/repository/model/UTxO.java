package ledger.repository.model;

import com.fasterxml.jackson.databind.JsonSerializer;

import java.math.BigInteger;

public class UTxO {
    BigInteger transaction_id;
    BigInteger address;
    Long coins = null;

    public UTxO(BigInteger transaction_id, BigInteger address){
        this.transaction_id = transaction_id;
        this.address = address;
    }

    public long getCoins(){
        return coins;
    }

    public BigInteger getAddress(){
        return address;
    }

    public BigInteger getTransaction_id(){
        return transaction_id;
    }

    public void setCoins(long coins){
        this.coins = coins;
    }

    public void setAddress(BigInteger address){
        this.address = address;
    }

    public void getTransaction_id(BigInteger transaction_id){
        this.transaction_id = transaction_id;
    }
}
