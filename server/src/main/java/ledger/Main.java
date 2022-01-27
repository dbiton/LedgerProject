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
    final static List<Integer> server_ports = Arrays.asList(3453, 12345, 2343);

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

        int num_shards = 3;
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
                LedgerServer server = new LedgerServer(localhost, server_port, server_shard, num_shards, zookeeper_host, other_servers);
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

        client.sendCoins(BigInteger.ONE, 1232);
        client.sendCoins(BigInteger.TWO, 1234);
        client.sendCoins(BigInteger.TWO, 234);
        client.sendCoins(BigInteger.TWO, 2134);
        client.sendCoins(BigInteger.TEN, 23512);
        client.sendCoins(BigInteger.TEN, 34432);
        client.sendCoins(BigInteger.TEN, 3424123);
        client.sendCoins(BigInteger.TEN, 33224);
        client.sendCoins(BigInteger.TWO, 32322);
        client.sendCoins(BigInteger.TEN, 123);
        client.sendCoins(BigInteger.ONE, 1231);

        List<UTxO> us = client.getUTxOs(BigInteger.ZERO);
        List<Transaction> ts = client.getAllTransactions(32);

        System.out.println(Arrays.toString(us.toArray()));
        System.out.println(Arrays.toString(ts.toArray()));
    }
}
