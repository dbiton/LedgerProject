package ledger.service;

import cs236351.ledger.*;
import io.grpc.stub.StreamObserver;
import ledger.repository.LedgerRepository;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import ledger.repository.model.Transaction;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import ledger.util.proto;

public class LedgerService extends LedgerServiceGrpc.LedgerServiceImplBase {
    LedgerRepository repository;
    int shard;
    int num_shards;
    List<LedgerServiceClient> other_servers;

    @Override
    public void submitTransaction(cs236351.ledger.Transaction request,
                                  io.grpc.stub.StreamObserver<cs236351.ledger.Res> responseObserver) {
        ledger.repository.model.Transaction transaction = proto.fromMessage(request);
        int shard_responsible = getShardResponsibleFor(transaction.getId());
        if (shard_responsible != this.shard){
            responseObserver.onNext(getClientForShard(shard_responsible).getStub().submitTransaction(request));
            responseObserver.onCompleted();
        }
        // this shard is responsible
        else{
            // TODO: check everything is legal here...

            // transfer coins
            BigInteger transaction_id = transaction.getId();
            for (ledger.repository.model.Transfer transfer : transaction.getOutputs()) {
                int shard_transfer = getShardResponsibleFor(transfer.getAddress());
                if (this.shard == shard_transfer){
                    repository.submitTransfer(transfer, transaction_id);
                }
                else{
                    LedgerServiceGrpc.LedgerServiceBlockingStub stub = getClientForShard(shard_responsible).getStub();
                    stub.submitTransfer(
                            cs236351.ledger.TransferAndTransactionID.newBuilder().
                                    setTransfer(proto.toMessage(transfer)).
                                    setTransactionId(proto.toUint128(transaction_id)).
                                    build());
                }
            }
            repository.submitTransaction(transaction);
            responseObserver.onNext(Res.newBuilder().setRes(ResCode.SUCCESS).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getUTxOs(cs236351.ledger.uint128 request,
                         io.grpc.stub.StreamObserver<cs236351.ledger.UTxO> responseObserver) {
        BigInteger id = proto.toBigInteger(request);
        int shard_responsible = getShardResponsibleFor(id);
        if (shard_responsible != this.shard){
        }
    }

    @Override
    public void getTransactions(cs236351.ledger.AddressAndMax request,
                                io.grpc.stub.StreamObserver<cs236351.ledger.Transaction> responseObserver) {
    }

    @Override
    public void getAllTransactions(cs236351.ledger.Max request,
                                   io.grpc.stub.StreamObserver<cs236351.ledger.Transaction> responseObserver) {
    }

    @Override
    public void getTransferCoins(cs236351.ledger.Transfer request,
                                 io.grpc.stub.StreamObserver<cs236351.ledger.Coins> responseObserver) {
    }

    @Override
    public void submitTransfer(cs236351.ledger.TransferAndTransactionID request,
                               io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        ledger.repository.model.Transfer transfer = proto.fromMessage(request.getTransfer());
        BigInteger transaction_id = proto.toBigInteger(request.getTransactionId());
    }

    public void setShard(int shard){
        this.shard = shard;
    }

    public void setServers(List<LedgerServiceClient> others_servers){
        this.other_servers = others_servers;
    }

    private int getShardResponsibleFor(BigInteger address){
        return address.remainder(BigInteger.valueOf(this.num_shards)).intValue();
    }

    private LedgerServiceClient getClientForShard(int shard){
        for (LedgerServiceClient client : this.other_servers){
            if (client.getShard() == shard){
                return client;
            }
        }
        throw new NoSuchElementException("shard doesn't exist");
    }
}
