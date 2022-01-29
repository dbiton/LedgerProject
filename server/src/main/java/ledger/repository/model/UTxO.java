package ledger.repository.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonSerializer;

import javax.persistence.*;
import java.math.BigInteger;
import java.util.Objects;

@Entity
public class UTxO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("transaction_id")
    BigInteger transaction_id;
    @JsonProperty("address")
    BigInteger address;
    Long coins = null;

    public UTxO(){
    }

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

    public void setTransaction_id(BigInteger transaction_id){
        this.transaction_id = transaction_id;
    }

    @Override
    public String toString() {
        return "UTxO{" +
                "transaction_id=" + transaction_id +
                ", address=" + address +
                ", coins=" + coins +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UTxO uTxO = (UTxO) o;
        return Objects.equals(address, uTxO.address) && Objects.equals(this.transaction_id, uTxO.transaction_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, transaction_id);
    }
}
