package ledger.controller;

import cs236351.ledger.*;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import ledger.repository.model.Transaction;
import ledger.repository.model.UTxO;
import ledger.util.proto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LedgerController {
    private final LedgerServiceGrpc.LedgerServiceBlockingStub stub;
    private final ManagedChannel channel;

    public LedgerController(BigInteger address, String host, int port){
        this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        this.stub = LedgerServiceGrpc.newBlockingStub(channel);
    }

    public boolean sendCoins(BigInteger address, long amount){
        //Res res = stub.sendCoins(address, amount);
        //return res.getRes().equals(ResCode.SUCCESS);
        return false;
    }

    public boolean submitTransaction(Transaction transaction) {
        Res res = stub.submitTransaction(proto.toMessage(transaction));
        return res.getRes().equals(ResCode.SUCCESS);
    }

    public boolean submitTransactions(List<Transaction> transactions) {
        //Res res = stub.submitTransactions(transactions);
        //return res.getRes().equals(ResCode.SUCCESS);
        return false;
    }

    public List<UTxO> getUTxOs(BigInteger address){
        try {
            Iterator<cs236351.ledger.UTxO> it = stub.getUTxOs(proto.toUint128(address));
            List<UTxO> res = new ArrayList<>();
            while (it.hasNext()) {
                res.add(proto.fromMessage(it.next()));
            }
            return res;
        }
        catch (StatusRuntimeException e){
            System.out.println("RPC failed: " + e.getStatus());
            return new ArrayList<>();
        }
    }

    public List<Transaction> getTransactions(BigInteger address, int max) {
        try {
            Iterator<cs236351.ledger.Transaction> it = stub.getTransactions(proto.toMessage(address, max));
            List<Transaction> res = new ArrayList<>();
            while (it.hasNext()) {
                res.add(proto.fromMessage(it.next()));
            }
            return res;
        }
        catch (StatusRuntimeException e){
            return new ArrayList<>();
        }
    }

    public List<Transaction> getAllTransactions(int max){
        try {
            Iterator<cs236351.ledger.Transaction> it = stub.getAllTransactions(Max.newBuilder().setMax(max).build());
            List<Transaction> res = new ArrayList<>();
            while (it.hasNext()) {
                res.add(proto.fromMessage(it.next()));
            }
            return res;
        }
        catch (StatusRuntimeException e){
            return new ArrayList<>();
        }
    }
}
