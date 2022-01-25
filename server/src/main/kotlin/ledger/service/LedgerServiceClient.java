package ledger.service;

import cs236351.ledger.LedgerServiceGrpc;
import cs236351.ledger.Transfer;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class LedgerServiceClient {
    private final int shard;
    private final LedgerServiceGrpc.LedgerServiceBlockingStub stub;

    public LedgerServiceClient(int shard, String host, int port){
        this.shard = shard;
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).build();
        this.stub = LedgerServiceGrpc.newBlockingStub(channel);
    }

    public int getShard(){
        return this.shard;
    }

    public LedgerServiceGrpc.LedgerServiceBlockingStub getStub(){
        return this.stub;
    }
}
