package ledger.service;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.List;

public class LedgerServer {
    int port;
    Server server;

    public LedgerServer(int port, int shard, List<LedgerServiceClient> other_servers){
        this(ServerBuilder.forPort(port), port, shard, other_servers);
    }

    public LedgerServer(ServerBuilder<?> serverBuilder, int port, int shard, List<LedgerServiceClient> other_servers){
        this.port = port;
        LedgerService service = new LedgerService();
        service.setShard(shard);
        service.setServers(other_servers);
        server = serverBuilder.addService(service)
                .build();
    }

    public void start() throws IOException {
        server.start();
    }
}
