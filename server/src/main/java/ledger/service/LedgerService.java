package ledger.service;

import com.google.protobuf.Empty;
import cs236351.ledger.*;
import ledger.repository.LedgerRepository;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import ledger.util.proto;

public class LedgerService extends LedgerServiceGrpc.LedgerServiceImplBase {
    LedgerRepository repository = new LedgerRepository();
    int shard;
    int num_shards;
    List<LedgerServiceClient> other_servers;

    @Override
    public void submitTransaction(cs236351.ledger.Transaction request,
                                  io.grpc.stub.StreamObserver<cs236351.ledger.Res> responseObserver) {
        ledger.repository.model.Transaction transaction = proto.fromMessage(request);
        int shard_responsible = getShardResponsibleFor(transaction.getId());
        if (shard_responsible != this.shard){
            Res res = getClientForShard(shard_responsible).getStub().submitTransaction(request);
            responseObserver.onNext(res);
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
                    LedgerServiceGrpc.LedgerServiceBlockingStub stub = getClientForShard(shard_transfer).getStub();
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
            Iterator<UTxO> it = getClientForShard(shard_responsible).getStub().getUTxOs(proto.toUint128(id));
            while(it.hasNext()){
                responseObserver.onNext(it.next());
            }
        } else {
            List<ledger.repository.model.UTxO> us = repository.getUTxOs(proto.toBigInteger(request));
            for (ledger.repository.model.UTxO u : us){
                responseObserver.onNext(proto.toMessage(u));
            }
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getTransactions(cs236351.ledger.AddressAndMax request,
                                io.grpc.stub.StreamObserver<cs236351.ledger.Transaction> responseObserver) {
        BigInteger address = proto.toBigInteger(request.getAddress());
        int max = request.getMax();
        for (ledger.repository.model.Transaction t : repository.getTransactions(address, max)){
            responseObserver.onNext(proto.toMessage(t));
        }
        for (int curr_shard=0; curr_shard<num_shards; curr_shard++){
            if (curr_shard != this.shard){
                Iterator<Transaction> it = getClientForShard(curr_shard).getStub().getTransactions(request);
                while (it.hasNext()){
                    responseObserver.onNext(it.next());
                }
            }
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getAllTransactions(cs236351.ledger.Max request,
                                   io.grpc.stub.StreamObserver<cs236351.ledger.Transaction> responseObserver) {
        int max = request.getMax();
        for (ledger.repository.model.Transaction t : repository.getAllTransactions(max)){
            responseObserver.onNext(proto.toMessage(t));
        }
        for (int curr_shard=0; curr_shard<num_shards; curr_shard++){
            if (curr_shard != this.shard){
                Iterator<Transaction> it = getClientForShard(curr_shard).getStub().getAllTransactions(request);
                while (it.hasNext()){
                    responseObserver.onNext(it.next());
                }
            }
        }
        responseObserver.onCompleted();
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
        repository.submitTransfer(transfer, transaction_id);
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    public void setShard(int shard){
        this.shard = shard;
    }

    public void setNumShards(int num_shards){
        this.num_shards = num_shards;
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
