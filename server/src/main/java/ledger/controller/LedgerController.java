package ledger.controller;

import cs236351.ledger.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
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


    public LedgerController(String host, int port){
        this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        this.stub = LedgerServiceGrpc.newBlockingStub(channel);
    }

    public boolean sendCoins(BigInteger address_from , BigInteger address_to, long amount){
        Res res = stub.sendCoins(proto.toMessage(address_from, address_to, amount));
        return res.getRes().equals(ResCode.SUCCESS);
    }

    public boolean submitTransaction(Transaction transaction) {
        Res res = stub.submitTransaction(proto.toMessage(transaction));
        return res.getRes().equals(ResCode.SUCCESS);
    }

    public boolean submitTransactions(List<Transaction> transactions) {
        LedgerServiceGrpc.LedgerServiceStub streaming_stub = LedgerServiceGrpc.newStub(channel);
        final boolean[] success = {false};
        StreamObserver<cs236351.ledger.Transaction> so =
                streaming_stub.submitTransactions(
                        new StreamObserver<Res>() {
            @Override
            public void onNext(Res value) {
                success[0] = value.getRes().equals(ResCode.SUCCESS);
            }
            @Override
            public void onError(Throwable t) {
            }
            @Override
            public void onCompleted() {
            }
        });

        for (Transaction t : transactions){
            so.onNext(proto.toMessage(t));
        }
        so.onCompleted();

        return success[0];
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
