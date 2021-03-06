package ledger.service;

import cs236351.ledger.LedgerServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class LedgerServiceClient {
    private final int shard;
    private final String host;
    private final int port;
    private ManagedChannel channel;

    private LedgerServiceGrpc.LedgerServiceBlockingStub stub = null;

    public LedgerServiceClient(int shard, String host, int port){
        this.shard = shard;
        this.host = host;
        this.port = port;
    }

    public int getShard(){
        return this.shard;
    }

    public LedgerServiceGrpc.LedgerServiceBlockingStub getStub(){
        if (stub == null){
            this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
            stub = LedgerServiceGrpc.newBlockingStub(channel);
        }
        return stub;
    }

    public int getPort(){
        return port;
    }

    public String getHost(){
        return host;
    }

    public boolean isAlive() {
        if (stub == null){
            this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
            stub = LedgerServiceGrpc.newBlockingStub(channel);
        }
       return channel.isTerminated() || channel.isShutdown();
    }
}
