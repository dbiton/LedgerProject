package ledger.service;

import cs236351.ledger.*;
import ledger.repository.LedgerRepository;
import ledger.repository.model.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import ledger.repository.util.proto;
import zookeeper.Manager;
import zookeeper.ManagerImpl;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class LedgerServer {
    LedgerRepository repository;
    int shard;
    int num_shards;
    int port;
    String host;
    Server server;
    Manager zk;
    LedgerServer ledger;
    List<LedgerServiceClient> shard_clients;
    List<LedgerServiceClient> other_clients;
    List<LedgerServiceClient> leader_order;

    public LedgerServer(String host, int port, int num_shards, String zookeeper_host)
            throws IOException, InterruptedException {
        this(ServerBuilder.forPort(port),host, port, num_shards, zookeeper_host);
    }

    public LedgerServer(ServerBuilder<?> serverBuilder, String host, int port, int num_shards, String zookeeper_host)
            throws IOException, InterruptedException {
        this.zk = new ManagerImpl(zookeeper_host);

        this.host = host;
        this.port = port;
        this.num_shards = num_shards;

        zk.registerServer(host, port);
        TimeUnit.SECONDS.sleep(3);
        List<String> clients = zk.getOtherServers();
        setupClients(clients);

        if (shard == 0){
            submitGenesis();
        }

        zk.electLeader(host, port, shard);
        getLeaderOrder();
        LedgerService service = new LedgerService();
        server = serverBuilder.addService(service)
                .build();
    }

    private void getLeaderOrder(){
        leader_order.clear();
        List<String> leader_order_str = zk.getLeaderOrder(shard);
        for (String s : leader_order_str){
            String[] ss = s.split(":");
            String leader_host = ss[0];
            int leader_port = Integer.parseInt(ss[1]);
            if (leader_port == this.port && leader_host.equals(this.host)){
                leader_order.add(null);
            }
            else{
                for (LedgerServiceClient client : shard_clients){
                    if (leader_port == client.getPort() && leader_host.equals(client.getHost())){
                        leader_order.add(client);
                    }
                }
            }
        }
    }

    private void onClientDisconnect(LedgerServiceClient client){
        shard_clients.remove(client);
        other_clients.remove(client);
        leader_order.remove(client);
    }

    private int getShardFor(BigInteger address){
        return address.remainder(BigInteger.valueOf(num_shards)).intValue();
    }

    private LedgerServiceClient getClientResponsibleForShard(int address_shard){
        if (address_shard == this.shard){
            return null;
        } else {
            for (LedgerServiceClient client : other_clients){
                if (client.getShard() == address_shard){
                    return client;
                }
            }
        }
        // error! should not get here
        System.out.println("No client responsible for shard " + address_shard);
        System.exit(1);
        return null;
    }

    private LedgerServiceClient getLeader(){
        return leader_order.get(0);
    }

    private void setupClients(List<String> clients_string){
        shard_clients = new ArrayList<>();
        other_clients = new ArrayList<>();
        int counter = 0;
        for (String client_string : clients_string){
            String[] client_details = client_string.split(":");
            String client_host = client_details[0];
            int client_port = Integer.parseInt(client_details[1]);
            int client_shard = counter % num_shards;
            counter += 1;
            if (client_port == this.port && client_host.equals(client_host)){
                this.shard = client_shard;
            }
            else {
                LedgerServiceClient client = new LedgerServiceClient(client_shard, client_host, client_port);
                if (client.getShard() == this.shard) {
                    shard_clients.add(client);
                } else {
                    other_clients.add(client);
                }
            }
        }
    }

    public void submitGenesis() {
        Transfer transfer_genesis = new Transfer(BigInteger.ZERO, Long.MAX_VALUE);
        repository.submitTransfer(transfer_genesis, BigInteger.ZERO);
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

    private void sortTransactions(List<Transaction> transactions){
        transactions.sort(new Comparator<Transaction>() {
            public int compare(Transaction t0, Transaction t1) {
                return t0.getId().compareTo(t1.getId());
            }
        });
    }

    private void submitTransfer(Transfer transfer, BigInteger transaction_id) {
        BigInteger address = transfer.getAddress();
        int address_shard = getShardFor(address);
        if (address_shard == this.shard) {
            LedgerServiceClient leader = getLeader();
            if (leader == null) {
                repository.submitTransfer(transfer, transaction_id);
                for (LedgerServiceClient follower : shard_clients) {
                    follower.getStub().submitTransfer(proto.toMessage(transfer, transaction_id));
                }
            }
            else {
                leader.getStub().submitTransfer(proto.toMessage(transfer, transaction_id));
            }
        } else {
            LedgerServiceClient client = getClientResponsibleForShard(address_shard);
            client.getStub().submitTransfer(proto.toMessage(transfer, transaction_id));
        }
    }

    public boolean submitTransaction(Transaction transaction){
        BigInteger address = transaction.getInputAddress();
        int address_shard = getShardFor(address);
        if (address_shard == this.shard){
            LedgerServiceClient client = getLeader();
            if (client == null){
                BigInteger transaction_id = zk.generateTransactionID(shard);
                transaction.setId(transaction_id);
                repository.submitTransaction(transaction);
                for (Transfer transfer : transaction.getOutputs()){
                    submitTransfer(transfer, transaction_id);
                }
                for (LedgerServiceClient follower : shard_clients){
                    follower.getStub().submitTransactionInternal(proto.toMessage(transaction));
                }
                return true;
            }
            else{
                Res res = client.getStub().submitTransaction(proto.toMessage(transaction));
                return res.getRes() == ResCode.SUCCESS;
            }
        }
        else{
            LedgerServiceClient client = getClientResponsibleForShard(address_shard);
            Res res = client.getStub().submitTransaction(proto.toMessage(transaction));
            return res.getRes() == ResCode.SUCCESS;
        }
    }

    private boolean isIndependent(List<Transaction> transactions){
        // inputs should not be in the outputs of other transactions
        Set<String> inputs = new HashSet<>();
        Set<String> outputs = new HashSet<>();
        for (Transaction t : transactions) {
            for (ledger.repository.model.Transfer transfer : t.getOutputs()) {
                String in = transfer.getAddress() + ":" + t.getId();
                inputs.add(in);
            }
            for (ledger.repository.model.UTxO utxo : t.getInputs()) {
                String out = utxo.getAddress() + ":" + utxo.getTransaction_id();
                outputs.add(out);
            }
            // usually outputs is smaller...
            for (String out : outputs) {
                if (inputs.contains(out)) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    public boolean submitTransactions(List<Transaction> transactions){
        if (isIndependent(transactions)){
            for (Transaction transaction : transactions){
                submitTransaction(transaction);
            }
            return true;
        }
        else {
            return false;
        }
    }

    public boolean sendCoins(BigInteger address_from, BigInteger address_to, long amount){
        int address_shard = getShardFor(address_from);
        if (address_shard == this.shard){
            LedgerServiceClient client = getLeader();
            if (client == null){
                List<UTxO> inputs = repository.consumeUTxOs(address_from, amount);
                List<Transfer> outputs = new ArrayList<>();
                outputs.add(new Transfer(address_to, amount));
                long coins = 0;
                for (UTxO u : inputs) coins += u.getCoins();
                if (coins > amount) outputs.add(new Transfer(address_from, coins-amount));
                Transaction transaction = new Transaction(zk.generateTransactionID(shard), inputs, outputs);
                submitTransaction(transaction);
                return true;
            }
            else{
                Res res = client.getStub().sendCoins(proto.toMessage(address_from, address_to, amount));
                return res.getRes() == ResCode.SUCCESS;
            }
        }
        else{
            LedgerServiceClient client = getClientResponsibleForShard(address_shard);
            Res res = client.getStub().sendCoins(proto.toMessage(address_from, address_to, amount));
            return res.getRes() == ResCode.SUCCESS;
        }
    }

    public List<UTxO> getUTxOs(BigInteger address){
        int address_shard = getShardFor(address);
        if (address_shard == this.shard){
            return repository.getUTxOs(address);
        }
        else{
            LedgerServiceClient client = getClientResponsibleForShard(address_shard);
            Iterator<rpcUTxO> it = client.getStub().getUTxOs(proto.toUint128(address));
            List<UTxO> us = new ArrayList<>();
            while (it.hasNext()){
                us.add(proto.fromMessage(it.next()));
            }
            return us;
        }
    }

    public List<Transaction> getTransactions(BigInteger address, int max){
        int address_shard = getShardFor(address);
        if (address_shard == this.shard){
           return repository.getTransactions(address, max);
        }
        else{
            LedgerServiceClient client = getClientResponsibleForShard(address_shard);
            Iterator<rpcTransaction> it = client.getStub().getTransactions(proto.toMessage(address, max));
            List<Transaction> ts = new ArrayList<>();
            while (it.hasNext()){
                ts.add(proto.fromMessage(it.next()));
            }
            sortTransactions(ts);
            return ts;
        }
    }

    public List<Transaction> getAllTransactions(int max){
        List<Transaction> ts = new ArrayList<>(repository.getAllTransactions(max));
        for (int other_shard = 0; other_shard < num_shards; other_shard+=1){
            if (other_shard != this.shard){
                LedgerServiceClient client = getClientResponsibleForShard(other_shard);
                Iterator<rpcTransaction> it = client.getStub().getAllTransactions(Max.newBuilder().setMax(max).build());
                while (it.hasNext()){
                    ts.add(proto.fromMessage(it.next()));
                }
            }
        }
        sortTransactions(ts);
        return ts;
    }
}
