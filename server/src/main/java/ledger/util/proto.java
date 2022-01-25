package ledger.util;

import cs236351.ledger.Transfer;
import cs236351.ledger.uint128;
import ledger.repository.model.Transaction;
import ledger.repository.model.UTxO;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class proto {
    public static BigInteger toBigInteger(cs236351.ledger.uint128 n){
        return BigInteger.valueOf(n.getLow()).add(BigInteger.valueOf(n.getHigh()).shiftLeft(64));
    }

    public static cs236351.ledger.uint128 toUint128(BigInteger n){
        return uint128.newBuilder().setLow(n.longValue()).setHigh(n.shiftRight(64).longValue()).build();
    }

    public static cs236351.ledger.Transfer toMessage(ledger.repository.model.Transfer transfer){
        return cs236351.ledger.Transfer.newBuilder()
                .setAddress(toUint128(transfer.getAddress()))
                .setCoins(cs236351.ledger.Coins.newBuilder().setCoins(transfer.getCoins()))
                .build();
    }

    public static cs236351.ledger.UTxO toMessage(ledger.repository.model.UTxO utxo){
        return cs236351.ledger.UTxO.newBuilder()
                .setAddress(toUint128(utxo.getAddress()))
                .setTransactionId(toUint128(utxo.getTransaction_id()))
                .build();
    }

    public static cs236351.ledger.Transaction toMessage(ledger.repository.model.Transaction transaction){
        uint128 id = toUint128(transaction.getId());
        cs236351.ledger.Transaction.Builder builder = cs236351.ledger.Transaction.newBuilder().setId(id);
        int n = 0;
        for (ledger.repository.model.UTxO u : transaction.getInputs()){
            builder.addInputs(n, toMessage(u));
            n += 1;
        }
        n = 0;
        for (ledger.repository.model.Transfer t : transaction.getOutputs()){
            builder.addOutputs(n, toMessage(t));
            n += 1;
        }
        return builder.build();
    }

    public static ledger.repository.model.Transfer fromMessage(cs236351.ledger.Transfer msg){
        BigInteger address = toBigInteger(msg.getAddress());
        long coins = msg.getCoins().getCoins();
        return new ledger.repository.model.Transfer(address, coins);
    }

    public static ledger.repository.model.UTxO fromMessage(cs236351.ledger.UTxO msg){
        BigInteger transaction_id = toBigInteger(msg.getTransactionId());
        BigInteger address = toBigInteger(msg.getAddress());
        return new ledger.repository.model.UTxO(transaction_id, address);
    }

    public static Transaction fromMessage(cs236351.ledger.Transaction msg){
        BigInteger id = toBigInteger(msg.getId());
        List<UTxO> inputs = new ArrayList<>();
        List<ledger.repository.model.Transfer> outputs = new ArrayList<>();
        for (cs236351.ledger.UTxO u : msg.getInputsList()){
            inputs.add(fromMessage(u));
        }
        for (Transfer t : msg.getOutputsList()){
            outputs.add(fromMessage(t));
        }
        return new Transaction(id, inputs, outputs);
    }
}
