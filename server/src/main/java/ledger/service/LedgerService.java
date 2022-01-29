package ledger.service;

import com.google.protobuf.Empty;
import cs236351.ledger.*;
import io.grpc.stub.StreamObserver;
import ledger.repository.LedgerRepository;

import java.math.BigInteger;
import java.util.*;

import ledger.util.proto;
import org.apache.zookeeper.CreateMode;
import zookeeper.Manager;

public class LedgerService extends LedgerServiceGrpc.LedgerServiceImplBase {
    LedgerRepository repository = new LedgerRepository();

    // constants
    int shard;
    int num_shards;
    int port;
    String host;
    Manager zk;
    List<LedgerServiceClient> shard_clients = new ArrayList<>();
    List<LedgerServiceClient> other_clients = new ArrayList<>();
    List<LedgerServiceClient> leader_order;

    @Override
    public void submitTransactionInternal(cs236351.ledger.Transaction request,
                                          io.grpc.stub.StreamObserver<cs236351.ledger.Res> responseObserver) {
        ledger.repository.model.Transaction transaction = proto.fromMessage(request);
        repository.submitTransaction(transaction);
        responseObserver.onNext(Res.newBuilder().setRes(ResCode.SUCCESS).build());
        responseObserver.onCompleted();
    }

    @Override
    public void submitTransaction(cs236351.ledger.Transaction request,
                                  io.grpc.stub.StreamObserver<cs236351.ledger.Res> responseObserver){
        ledger.repository.model.Transaction transaction = proto.fromMessage(request);
        LedgerServiceClient client = null;
        try {
            client = getLeaderResponsibleForAddress(transaction.getInputAddress());
        } catch (Exception e) {
            responseObserver.onNext(Res.newBuilder().setRes(ResCode.FAILURE).build());
            responseObserver.onCompleted();
        }

        if (client != null){
            Res res = client.getStub().submitTransaction(request);
            responseObserver.onNext(res);
            responseObserver.onCompleted();
        }

        else{
            submitTransactionLeader(request);
            responseObserver.onNext(Res.newBuilder().setRes(ResCode.SUCCESS).build());
            responseObserver.onCompleted();
        }
    }

    private void submitTransactionLeader(cs236351.ledger.Transaction request){
        ledger.repository.model.Transaction transaction = proto.fromMessage(request);
        transaction.setId(generateTransactionID());
        // transfer coins
        BigInteger transaction_id = transaction.getId();
        for (ledger.repository.model.Transfer transfer : transaction.getOutputs()) {
            LedgerServiceClient client;
            try {
                client = getLeaderResponsibleForAddress(transfer.getAddress());
            } catch (Exception e) {
                // could not send transfer... this should not happen though! (according to assignment details)
                e.printStackTrace();
                return;
            }
            if (client == null){
                repository.submitTransfer(transfer, transaction_id);
            }
            else{
                client.getStub().submitTransfer(
                        cs236351.ledger.TransferAndTransactionID.newBuilder().
                                setTransfer(proto.toMessage(transfer)).
                                setTransactionId(proto.toUint128(transaction_id)).
                                build());
            }
        }
        repository.submitTransaction(transaction);
        for (LedgerServiceClient shard_client : shard_clients){
            shard_client.getStub().submitTransactionInternal(request);
        }
    }

    @Override
    public StreamObserver<Transaction> submitTransactions(StreamObserver<Res> responseObserver) {
        return new StreamObserver<>() {
            final List<Transaction> transactions = new ArrayList<Transaction>();

            @Override
            public void onNext(Transaction transaction) {
                transactions.add(transaction);
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("Error while reading transaction stream: " + t);
            }

            private boolean isIndependent(List<Transaction> transactions) {
                // inputs should not be in the outputs of other transactions
                Set<String> inputs = new HashSet<>();
                Set<String> outputs = new HashSet<>();
                for (Transaction t_msg : transactions) {
                    ledger.repository.model.Transaction t = proto.fromMessage(t_msg);
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

            @Override
            public void onCompleted() {
                if (isIndependent(transactions)) {
                    for (Transaction transaction : transactions) {
                        BigInteger address_sender = proto.fromMessage(transaction).getInputAddress();
                        LedgerServiceClient client = getLeaderResponsibleForAddress(address_sender);
                        if (client == null) {
                            // we are responsible for the commit
                            submitTransactionLeader(transaction);
                        } else {
                            client.getStub().submitTransaction(transaction);
                        }
                    }
                }
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void sendCoins(cs236351.ledger.AddressesAndAmount request,
                          io.grpc.stub.StreamObserver<cs236351.ledger.Res> responseObserver) {
        BigInteger address_from = proto.toBigInteger(request.getAddressFrom());
        LedgerServiceClient client = getLeaderResponsibleForAddress(address_from);
        // we are not responsible for this request, redirect it to someone else
        if (client != null){
            Res res = client.getStub().sendCoins(request);
            responseObserver.onNext(res);
            responseObserver.onCompleted();
            return;
        }
        BigInteger address_to = proto.toBigInteger(request.getAddressTo());
        long amount = request.getAmount();

        List<ledger.repository.model.UTxO> inputs = repository.consumeUTxOs(address_from, amount);
        // we don't have enough coins for the transaction!
        if (inputs.isEmpty()){
            responseObserver.onNext(Res.newBuilder().setRes(ResCode.FAILURE).build());
            responseObserver.onCompleted();
        }

        List<ledger.repository.model.Transfer> outputs = new ArrayList<>();
        ledger.repository.model.Transfer transfer_coins = new ledger.repository.model.Transfer(address_to, amount);
        outputs.add(transfer_coins);

        BigInteger transaction_id = generateTransactionID();
        LedgerServiceClient client_recv = getLeaderResponsibleForAddress(address_to);
        // we are also responsible for address_to
        if (client_recv == null){
            repository.submitTransfer(transfer_coins, transaction_id);
            // followers copy the leader...
            for (LedgerServiceClient follower : shard_clients){
                follower.getStub().submitTransferInternal(proto.toMessage(transfer_coins, transaction_id));
            }
        }
        else{
            client_recv.getStub().submitTransfer(proto.toMessage(transfer_coins, transaction_id));
        }

        long amount_input = 0;
        for (ledger.repository.model.UTxO input : inputs){
            amount_input += input.getCoins();
        }
        long change = amount_input - amount;
        if (change > 0){
            ledger.repository.model.Transfer transfer_change = new ledger.repository.model.Transfer(address_from, change);
            outputs.add(transfer_change);
            repository.submitTransfer(transfer_change, transaction_id);
            // followers copy the leader...
            for (LedgerServiceClient follower : shard_clients){
                follower.getStub().submitTransferInternal(proto.toMessage(transfer_coins, transaction_id));
            }
        }
        ledger.repository.model.Transaction transaction =
                new ledger.repository.model.Transaction(transaction_id, inputs, outputs);
        repository.submitTransaction(transaction);
        // followers copy the leader...
        for (LedgerServiceClient follower : shard_clients){
            follower.getStub().submitTransactionInternal(proto.toMessage(transaction));
        }
        responseObserver.onNext(Res.newBuilder().setRes(ResCode.SUCCESS).build());
        responseObserver.onCompleted();
    }

    @Override
    public void submitTransfer(cs236351.ledger.TransferAndTransactionID request,
                               io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        LedgerServiceClient leader_client = getLeader();
        if (leader_client == null) {
            ledger.repository.model.Transfer transfer = proto.fromMessage(request.getTransfer());
            BigInteger transaction_id = proto.toBigInteger(request.getTransactionId());
            repository.submitTransfer(transfer, transaction_id);
            for (LedgerServiceClient follower_client : shard_clients){
                follower_client.getStub().submitTransferInternal(request);
            }
        } else {
            leader_client.getStub().submitTransfer(request);
        }
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void submitTransferInternal(cs236351.ledger.TransferAndTransactionID request,
                               io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        ledger.repository.model.Transfer transfer = proto.fromMessage(request.getTransfer());
        BigInteger transaction_id = proto.toBigInteger(request.getTransactionId());
        repository.submitTransfer(transfer, transaction_id);
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void getUTxOs(cs236351.ledger.uint128 request,
                         io.grpc.stub.StreamObserver<cs236351.ledger.UTxO> responseObserver) {
        BigInteger address = proto.toBigInteger(request);
        LedgerServiceClient client = getClientResponsibleForAddress(address);
        if (client != null){
            Iterator<UTxO> it = client.getStub().getUTxOs(request);
            while(it.hasNext()){
                responseObserver.onNext(it.next());
            }
        } else {
            List<ledger.repository.model.UTxO> us = repository.getUTxOs(proto.toBigInteger(request));
            for (ledger.repository.model.UTxO u : us){
                responseObserver.onNext(proto.toMessage(u));
            }
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getTransactions(cs236351.ledger.AddressAndMax request,
                                io.grpc.stub.StreamObserver<cs236351.ledger.Transaction> responseObserver) {
        BigInteger address = proto.toBigInteger(request.getAddress());
        int max = request.getMax();
        LedgerServiceClient client = getClientResponsibleForAddress(address);
        if (client == null) {
            for (ledger.repository.model.Transaction t : repository.getTransactions(address, max)) {
                responseObserver.onNext(proto.toMessage(t));
            }
            responseObserver.onCompleted();
        }
        else {
            Iterator<Transaction> it = client.getStub().getTransactions(request);
            while(it.hasNext()){
                responseObserver.onNext(it.next());
            }
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getAllShardTransactions(cs236351.ledger.Max request,
                                   io.grpc.stub.StreamObserver<cs236351.ledger.Transaction> responseObserver) {
        int max = request.getMax();
        for (ledger.repository.model.Transaction t : repository.getAllTransactions(max)) {
            responseObserver.onNext(proto.toMessage(t));
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getAllTransactions(cs236351.ledger.Max request,
                                   io.grpc.stub.StreamObserver<cs236351.ledger.Transaction> responseObserver) {
        int max = request.getMax();
        List<Transaction> transactions = new ArrayList<>();
        for (ledger.repository.model.Transaction t : repository.getAllTransactions(max)){
            transactions.add(proto.toMessage(t));
        }
        for (int curr_shard=0; curr_shard<num_shards; curr_shard++){
            if (curr_shard != this.shard){
                LedgerServiceClient client = getClientResponsibleForShard(curr_shard);
                Iterator<Transaction> it = client.getStub().getAllShardTransactions(request);
                while (it.hasNext()){
                    transactions.add(it.next());
                }
            }
        }
        sortTransactions(transactions);
        for (Transaction t : transactions) {
            responseObserver.onNext(t);
        }
        responseObserver.onCompleted();
    }

    public void setZooKeeper(Manager zk){
        this.zk = zk;
    }

    public void setPort(int port){
        this.port = port;
    }

    public void setHost(String host){
        this.host = host;
    }

    public void setNumShards(int num_shards){
        this.num_shards = num_shards;
    }

    public void setServers(int shard, List<LedgerServiceClient> clients){
        this.shard = shard;
        for (LedgerServiceClient client : clients){
            if (client.getShard() == shard) {
                this.shard_clients.add(client);
            }
            else {
                this.other_clients.add(client);
            }
        }
    }

    private int getShardResponsibleFor(BigInteger address){
        return address.remainder(BigInteger.valueOf(this.num_shards)).intValue();
    }

    private LedgerServiceClient getLeader(){
        // leader not initialized
        if (leader_order == null) {
            electLeader();
            leader_order = getLeaderOrder();
        }
        while (leader_order.isEmpty()) {
            System.out.println("Checking leader order...");
            leader_order = getLeaderOrder();
        }
        return leader_order.get(0);
    }

    private LedgerServiceClient getLeaderResponsibleForShard(int shard) {
        if (shard == this.shard){
            return getLeader();
        }
        else {
            return getClientResponsibleForShard(shard);
        }
    }

    private LedgerServiceClient getLeaderResponsibleForAddress(BigInteger address) {
        int responsible_shard = getShardResponsibleFor(address);
        return getLeaderResponsibleForShard(responsible_shard);
    }

    private LedgerServiceClient getClientResponsibleForShard(int shard){
        if (shard == this.shard){
            return null;
        }
        else {
            List<LedgerServiceClient> dead_clients = new ArrayList<>();
            LedgerServiceClient responsible_client = null;
            for (LedgerServiceClient client : other_clients){
                if (client.getShard() == shard){
                    responsible_client = client;
                    break;
                }
            }
            for (LedgerServiceClient dead_client : dead_clients){
                other_clients.remove(dead_client);
            }
            if (responsible_client == null){
                throw new NoSuchElementException("No alive servers responsible for shard!");
            }
            return responsible_client;
        }
    }

    private LedgerServiceClient getClientResponsibleForAddress(BigInteger address){
        int shard_responsible = address.remainder(BigInteger.valueOf(this.num_shards)).intValue();
        return getClientResponsibleForShard(shard_responsible);
    }

    private BigInteger generateTransactionID(){
        BigInteger max = null;
        try {
            String shard_str = String.valueOf(this.shard);
            if (!zk.exists("/transactions")){
                zk.create("/transactions" , null, CreateMode.PERSISTENT);
            }
            zk.create("/transactions/" + shard_str+"-", null, CreateMode.EPHEMERAL_SEQUENTIAL);
            List<String> timestamps = zk.getChildren("/transactions");
            for (String t : timestamps){
                if (t.startsWith(shard_str)){
                    String ts = t.substring(shard_str.length()+1);
                    BigInteger n = new BigInteger(ts);
                    if (max == null || max.compareTo(n) < 0){
                        max = n;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return max;
    }

    private List<LedgerServiceClient> getLeaderOrder() {
        String shared_str = String.valueOf(this.shard);
        List<String> timestamps = new ArrayList<>();
        try {
            timestamps = zk.getChildren("/leaders/" + shared_str);
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
        Map<Long, String> leaders_sorted = new TreeMap<>();
        for (String t : timestamps) {
            String[] strings = t.split("-");
            String address = strings[0];
            Long ts = Long.valueOf(strings[1]);
            leaders_sorted.put(ts, address);
        }
        List<LedgerServiceClient> leader_order = new ArrayList<>();
        for (String leader_address : leaders_sorted.values()){
            String[] leader_address_data = leader_address.split(":");
            String leader_host = leader_address_data[0];
            int leader_port = Integer.parseInt(leader_address_data[1]);

            if (this.port == leader_port && this.host.equals(leader_host)){
                leader_order.add(null);
                break;
            }
            else {
                for (LedgerServiceClient client : shard_clients) {
                    if (client.getPort() == leader_port && client.getHost().equals(leader_host)) {
                        leader_order.add(client);
                        break;
                    }
                }
            }
        }
        return leader_order;
    }

    private void electLeader(){
        try {
            String shard_str = String.valueOf(this.shard);
            String port_str = String.valueOf(this.port);
            if (!zk.exists("/leaders")){
                zk.create("/leaders", null, CreateMode.PERSISTENT);
            }
            if (!zk.exists("/leaders/" + shard_str)){
                zk.create("/leaders/" + shard_str, null, CreateMode.PERSISTENT);
            }
            zk.create("/leaders/" + shard_str+ "/" + this.host + ":" + port_str +"-",
                    null, CreateMode.EPHEMERAL_SEQUENTIAL);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void sortTransactions(List<Transaction> transactions){
        transactions.sort(new Comparator<Transaction>() {
            public int compare(Transaction t0, Transaction t1) {
                return proto.toBigInteger(t0.getId()).compareTo(proto.toBigInteger(t1.getId()));
            }
        });
    }

    public void submitGenesis(){
        ledger.repository.model.Transfer transfer_genesis =
                new ledger.repository.model.Transfer(BigInteger.ZERO, Long.MAX_VALUE);
        repository.submitTransfer(transfer_genesis, BigInteger.ZERO);
    }
}
