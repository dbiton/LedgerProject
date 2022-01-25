package ledger.repository.model;

import java.math.BigInteger;

public class Transfer {
    BigInteger address;
    long coins;

    public Transfer(BigInteger address, long coins){
        this.address = address;
        this.coins = coins;
    }

    public void setAddress(BigInteger address){
        this.address = address;
    }

    public void setCoins(long coins){
        this.coins = coins;
    }

    public BigInteger getAddress(){
        return address;
    }

    public long getCoins(){
        return coins;
    }
}
