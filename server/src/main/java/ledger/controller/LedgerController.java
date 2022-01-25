package ledger.controller;

import cs236351.ledger.LedgerServiceGrpc;
import cs236351.ledger.Res;
import cs236351.ledger.ResCode;
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

    public boolean submitTransaction(Transaction transaction) {
        Res res = stub.submitTransaction(proto.toMessage(transaction));
        return res.getRes().equals(ResCode.SUCCESS);
    }

    public List<UTxO> getUTxOs(BigInteger address){
        try {
            Iterator<cs236351.ledger.UTxO> it = stub.getUTxOs(proto.toUint128(address));
            List<UTxO> res = new ArrayList<>();
            while (it.hasNext()) {
                System.out.println("sending for next");
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
        return new ArrayList<>();
    }

    public List<Transaction> getAllTransactions(int max){
        return new ArrayList<>();
    }
}
