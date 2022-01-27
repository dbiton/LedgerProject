package ledger;

import ledger.controller.LedgerController;
import ledger.repository.model.Transaction;
import ledger.repository.model.Transfer;
import ledger.repository.model.UTxO;
import ledger.service.LedgerServer;
import ledger.service.LedgerServiceClient;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ServerMain {
    public static void main(String[] args) {
        int server_id = Integer.parseInt(args[0]);
        String host = args[1];
        int port = Integer.parseInt(args[2]);
        int num_shards = Integer.parseInt(args[3]);
        String zookeeper_host = args[4];
        List<LedgerServiceClient> other_servers = new ArrayList<>();
        for (int j=5; j<args.length; j+=3){
            int other_port = Integer.parseInt(args[j]);
            int other_id = Integer.parseInt(args[j+1]);
            String other_host = args[j+2];
            int other_shard = other_id % num_shards;
            other_servers.add(new LedgerServiceClient(other_shard, other_host, other_port));
        }
        int server_shard = server_id % num_shards;
        try {
            LedgerServer server = new LedgerServer(host, port, server_shard, num_shards, zookeeper_host, other_servers);
            server.start();
        } catch (Exception e) {
            System.out.println("Error while creating server: " + e);
        }
    }
}