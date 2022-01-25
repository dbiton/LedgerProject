package ledger;

import ledger.controller.LedgerController;
import ledger.repository.model.Transaction;
import ledger.repository.model.Transfer;
import ledger.repository.model.UTxO;
import ledger.service.LedgerServer;
import ledger.service.LedgerServiceClient;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    static int serverIndexToShard(int server_index, int num_shards){
        return server_index%num_shards;
    }

    public static void main(String[] args) {
        int num_shards = 2;
        List<Integer> ports = Arrays.asList(6666, 7777, 8888, 9999);
        List<LedgerServer> servers = new ArrayList<>();
        String localhost = "localhost";
        for (int i=0; i<ports.size(); i++){
            int server_port = ports.get(i);
            int server_shard = serverIndexToShard(i, num_shards);
            List<LedgerServiceClient> other_servers = new ArrayList<>();
            for (int j=0; j<ports.size(); j++){
                if (i != j){
                    int other_port = ports.get(i);
                    int other_shard = serverIndexToShard(j, num_shards);
                    other_servers.add(new LedgerServiceClient(other_shard, localhost, other_port));
                }
            }
            LedgerServer server = new LedgerServer(server_port, server_shard, other_servers);
            servers.add(server);
        }

        LedgerController client = new LedgerController(BigInteger.ZERO, localhost, ports.get(0));

        List<UTxO> inputs = List.of(new UTxO(BigInteger.ZERO, BigInteger.ZERO));
        List<Transfer> outputs = Arrays.asList(new Transfer(BigInteger.ONE, 32), new Transfer(BigInteger.TEN, 64), new Transfer(BigInteger.TWO, 128));
        Transaction transaction = new Transaction(BigInteger.ZERO, inputs, outputs);
        client.submitTransaction(transaction);
    }
}
