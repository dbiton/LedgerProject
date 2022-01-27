package ledger.repository.model;

import java.math.BigInteger;
import java.util.*;

public class Transaction {
    BigInteger id;
    List<UTxO> inputs;
    List<Transfer> outputs;

    public Transaction(BigInteger id, List<UTxO> inputs, List<Transfer> outputs){
        this.id = id;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public void setId(BigInteger id){
        this.id = id;
    }

    public void setInputs(List<UTxO> inputs){
        this.inputs = inputs;
    }

    public void setOutputs(List<Transfer> outputs){
        this.outputs = outputs;
    }

    public BigInteger getId(){
        return id;
    }

    public List<UTxO> getInputs(){
        return inputs;
    }

    public List<Transfer> getOutputs(){
        return outputs;
    }

    public long getTransferValue(BigInteger address){
        for (Transfer t : outputs){
            if (t.address.equals(address)){
                return t.coins;
            }
        }
        throw new NoSuchElementException("Transfer not found");
    }

    public long getTotalValue(){
        long v = 0;
        for (Transfer t : outputs){
            v += t.coins;
        }
        return v;
    }

    public BigInteger getInputAddress(){
        return inputs.get(0).address;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", inputs=" + Arrays.toString(inputs.toArray()) +
                ", outputs=" + Arrays.toString(outputs.toArray()) +
                '}';
    }
}
