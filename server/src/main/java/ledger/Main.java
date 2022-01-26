package ledger;

import ledger.controller.LedgerController;
import ledger.repository.model.Transaction;
import ledger.repository.model.Transfer;
import ledger.repository.model.UTxO;
import ledger.service.LedgerServer;
import ledger.service.LedgerServiceClient;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import zookeeper.Connection;
import zookeeper.Manager;
import zookeeper.ManagerImpl;

public class Main {
    final static List<Integer> zookeeper_ports = Arrays.asList(2181, 2182, 2183);
    final static List<Integer> server_ports = Arrays.asList(6666, 7777, 8888, 9999);

    static int serverIndexToShard(int server_index, int num_shards){
        return server_index%num_shards;
    }

    private static String getHostZookeeper(){
        StringBuilder stringBuilder = new StringBuilder();
        for (Integer port : zookeeper_ports){
            stringBuilder.append("127.0.0.1:").append(String.valueOf(port)).append(",");
        }
        stringBuilder.deleteCharAt(stringBuilder.length()-1);
        return  stringBuilder.toString();
    }

    public static void main(String[] args) {
        String zookeeper_host = getHostZookeeper();

        int num_shards = 4;
        List<LedgerServer> servers = new ArrayList<>();
        String localhost = "localhost";
        for (int i=0; i<server_ports.size(); i++){
            int server_port = server_ports.get(i);
            int server_shard = serverIndexToShard(i, num_shards);
            List<LedgerServiceClient> other_servers = new ArrayList<>();
            for (int j=0; j<server_ports.size(); j++){
                if (i != j){
                    int other_port = server_ports.get(j);
                    int other_shard = serverIndexToShard(j, num_shards);
                    other_servers.add(new LedgerServiceClient(other_shard, localhost, other_port));
                }
            }
            try {
                LedgerServer server = new LedgerServer(server_port, server_shard, num_shards, zookeeper_host, other_servers);
                servers.add(server);
            }
            catch (Exception e){
                System.out.println("Could not create server!");
                break;
            }
        }

        for (LedgerServer server : servers){
            try {
                server.start();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

        LedgerController client = new LedgerController(BigInteger.ZERO, localhost, server_ports.get(0));

        List<UTxO> inputs = List.of(new UTxO(BigInteger.ZERO, BigInteger.ZERO));
        List<Transfer> outputs = Arrays.asList(new Transfer(BigInteger.ONE, 32), new Transfer(BigInteger.TEN, 64), new Transfer(BigInteger.TWO, 128));
        Transaction transaction = new Transaction(BigInteger.ZERO, inputs, outputs);
        client.submitTransaction(transaction);
        transaction.setId(BigInteger.ONE);
        client.submitTransaction(transaction);
        transaction.setId(BigInteger.TEN);
        client.submitTransaction(transaction);
        client.submitTransaction(transaction);
        transaction.setId(BigInteger.ONE);
        client.submitTransaction(transaction);
        transaction.setId(BigInteger.TWO);
        client.submitTransaction(transaction);
        transaction.setId(BigInteger.TEN);
        client.submitTransaction(transaction);
        List<UTxO> us = client.getUTxOs(BigInteger.ONE);
    }
}
