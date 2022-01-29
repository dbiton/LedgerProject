package ledger;

import ledger.service.LedgerServer;
import ledger.service.LedgerServiceClient;

import java.util.ArrayList;
import java.util.List;
/*
public class ServerMain {
    public static void main(String[] args) {
        String[] args1 = {"0", "127.0.0.1:66667", "6666", "4", "127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183", "1", "127.0.0.1:6668", "6666"};
        int server_id = Integer.parseInt(args1[0]);
        String host = args1[1];
        int port = Integer.parseInt(args1[2]);
        int num_shards = Integer.parseInt(args1[3]);
        String zookeeper_host = args1[4];
        List<LedgerServiceClient> other_servers = new ArrayList<>();
        for (int j=5; j<args1.length; j+=3){
            int other_port = Integer.parseInt(args1[j+2]);
            int other_id = Integer.parseInt(args1[j]);
            String other_host = args1[j+1];
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
}*/