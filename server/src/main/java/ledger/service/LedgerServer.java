package ledger.service;

import cs236351.ledger.Ledger;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import zookeeper.Connection;
import zookeeper.Manager;
import zookeeper.ManagerImpl;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LedgerServer {
    int port;
    Server server;
    Manager zk;

    public LedgerServer(String host, int port, int shard, int num_shards,
                        String zookeeper_host, List<LedgerServiceClient> other_servers)
            throws IOException, InterruptedException {
        this(ServerBuilder.forPort(port),host, port, shard, num_shards, zookeeper_host, other_servers);
    }

    public LedgerServer(ServerBuilder<?> serverBuilder, String host, int port, int shard, int num_shards,
                        String zookeeper_host, List<LedgerServiceClient> other_servers) throws IOException, InterruptedException {
        this.port = port;
        zookeeper.Connection connection = new Connection();
        Manager zk = new ManagerImpl(zookeeper_host);
        LedgerService service = new LedgerService();
        service.setServers(shard, other_servers);
        service.setNumShards(num_shards);
        service.setZooKeeper(zk);
        service.setPort(port);
        service.setHost(host);
        if (shard == 0){
            service.submitGenesis();
        }
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
