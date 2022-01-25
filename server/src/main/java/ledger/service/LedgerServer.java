package ledger.service;

import cs236351.ledger.Ledger;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LedgerServer {
    int port;
    Server server;

    public LedgerServer(int port, int shard, int num_shards, List<LedgerServiceClient> other_servers){
        this(ServerBuilder.forPort(port), port, shard, num_shards, other_servers);
    }

    public LedgerServer(ServerBuilder<?> serverBuilder, int port, int shard, int num_shards, List<LedgerServiceClient> other_servers){
        this.port = port;
        LedgerService service = new LedgerService();
        service.setShard(shard);
        service.setServers(other_servers);
        service.setNumShards(num_shards);
        server = serverBuilder.addService(service)
                .build();
    }

    public void start() throws IOException {
        try {
            server.start();
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    LedgerServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
            }
        });
    }

    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}
