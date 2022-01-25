package ledger.controller;

import cs236351.ledger.LedgerServiceGrpc;
import cs236351.ledger.Res;
import cs236351.ledger.ResCode;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import ledger.repository.model.Transaction;
import ledger.repository.model.Transfer;
import ledger.repository.model.UTxO;
import ledger.service.LedgerService;
import ledger.util.proto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class LedgerController {
    private final LedgerServiceGrpc.LedgerServiceBlockingStub stub;

    public LedgerController(BigInteger address, String host, int port){
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).build();
        this.stub = LedgerServiceGrpc.newBlockingStub(channel);
    }

    public boolean submitTransaction(Transaction transaction) {
        Res res = stub.submitTransaction(proto.toMessage(transaction));
        return res.getRes().equals(ResCode.SUCCESS);
    }

    public List<UTxO> getUTxOs(BigInteger address){
        return new ArrayList<>();
    }

    public List<Transaction> getTransactions(BigInteger address, int max) {
        return new ArrayList<>();
    }

    public List<Transaction> getAllTransactions(int max){
        return new ArrayList<>();
    }
}
