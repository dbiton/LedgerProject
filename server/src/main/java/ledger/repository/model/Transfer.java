package ledger.repository.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.math.BigInteger;
import java.util.Objects;

@Entity
public class Transfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("address")
    BigInteger address;
    @JsonProperty("coins")
    long coins;

    public Transfer(BigInteger address, long coins){
        this.address = address;
        this.coins = coins;
    }

    public Transfer() {
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

    @Override
    public String toString() {
        return "Transfer{" +
                "address=" + address +
                ", coins=" + coins +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transfer transfer = (Transfer) o;
        return coins == transfer.coins && Objects.equals(address, transfer.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, coins);
    }
}
