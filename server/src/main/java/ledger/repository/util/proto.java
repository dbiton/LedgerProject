package ledger.repository.util;

import cs236351.ledger.AddressAndMax;
import cs236351.ledger.*;
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

    public static cs236351.ledger.AddressesAndAmount toMessage(BigInteger address_from, BigInteger address_to, long amount)
    {
        return cs236351.ledger.AddressesAndAmount.newBuilder()
                .setAddressFrom(toUint128(address_from))
                .setAddressTo(toUint128(address_to))
                .setAmount(amount)
                .build();
    }

    public static AddressAndMax toMessage(BigInteger address, int max){
        return AddressAndMax.newBuilder()
                .setAddress(toUint128(address))
                .setMax(max)
                .build();
    }

    public static TransferAndTransactionID toMessage(ledger.repository.model.Transfer transfer,
                                                                     BigInteger transaction_id){
        return TransferAndTransactionID.newBuilder()
                .setTransactionId(toUint128(transaction_id))
                .setTransfer(toMessage(transfer))
                .build();
    }

    public static rpcTransfer toMessage(ledger.repository.model.Transfer transfer){
        return rpcTransfer.newBuilder()
                .setAddress(toUint128(transfer.getAddress()))
                .setCoins(cs236351.ledger.Coins.newBuilder().setCoins(transfer.getCoins()))
                .build();
    }

    public static rpcUTxO toMessage(ledger.repository.model.UTxO utxo){
        return rpcUTxO.newBuilder()
                .setAddress(toUint128(utxo.getAddress()))
                .setTransactionId(toUint128(utxo.getTransaction_id()))
                .build();
    }

    public static rpcTransaction toMessage(ledger.repository.model.Transaction transaction){
        uint128 id = toUint128(transaction.getId());
        rpcTransaction.Builder builder = rpcTransaction.newBuilder().setId(id);
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

    public static ledger.repository.model.Transfer fromMessage(rpcTransfer msg){
        BigInteger address = toBigInteger(msg.getAddress());
        long coins = msg.getCoins().getCoins();
        return new ledger.repository.model.Transfer(address, coins);
    }

    public static ledger.repository.model.UTxO fromMessage(rpcUTxO msg){
        BigInteger transaction_id = toBigInteger(msg.getTransactionId());
        BigInteger address = toBigInteger(msg.getAddress());
        return new ledger.repository.model.UTxO(transaction_id, address);
    }

    public static Transaction fromMessage(rpcTransaction msg){
        BigInteger id = toBigInteger(msg.getId());
        List<UTxO> inputs = new ArrayList<>();
        List<ledger.repository.model.Transfer> outputs = new ArrayList<>();
        for (rpcUTxO u : msg.getInputsList()){
            inputs.add(fromMessage(u));
        }
        for (rpcTransfer t : msg.getOutputsList()){
            outputs.add(fromMessage(t));
        }
        return new Transaction(id, inputs, outputs);
    }
}
