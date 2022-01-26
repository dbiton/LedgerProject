package ledger.service;

import com.google.protobuf.Empty;
import cs236351.ledger.*;
import ledger.repository.LedgerRepository;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import ledger.util.proto;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import zookeeper.Manager;

public class LedgerService extends LedgerServiceGrpc.LedgerServiceImplBase {
    LedgerRepository repository = new LedgerRepository();

    // constants
    int shard;
    int num_shards;
    int port;
    String host;
    Manager zk;
    List<LedgerServiceClient> other_servers;

    // variables
    boolean is_leader = false;
    LedgerServiceClient leader = null; // null if is_leader==true


    @Override
    public void submitTransaction(cs236351.ledger.Transaction request,
                                  io.grpc.stub.StreamObserver<cs236351.ledger.Res> responseObserver) {
        ledger.repository.model.Transaction transaction = proto.fromMessage(request);
        LedgerServiceClient client = getClientResponsibleFor(transaction.getId());
        if (client != null){
            Res res = client.getStub().submitTransaction(request);
            responseObserver.onNext(res);
            responseObserver.onCompleted();
        }
        // this shard is responsible
        else{
            // TODO: check everything is legal here...
            // transfer coins
            BigInteger transaction_id = transaction.getId();
            for (ledger.repository.model.Transfer transfer : transaction.getOutputs()) {
                int shard_transfer = getShardResponsibleFor(transfer.getAddress());
                if (this.shard == shard_transfer){
                    repository.submitTransfer(transfer, transaction_id);
                }
                else{
                    LedgerServiceGrpc.LedgerServiceBlockingStub stub = getClientForShard(shard_transfer).getStub();
                    stub.submitTransfer(
                            cs236351.ledger.TransferAndTransactionID.newBuilder().
                                    setTransfer(proto.toMessage(transfer)).
                                    setTransactionId(proto.toUint128(transaction_id)).
                                    build());
                }
            }
            repository.submitTransaction(transaction);
            responseObserver.onNext(Res.newBuilder().setRes(ResCode.SUCCESS).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getUTxOs(cs236351.ledger.uint128 request,
                         io.grpc.stub.StreamObserver<cs236351.ledger.UTxO> responseObserver) {
        BigInteger id = proto.toBigInteger(request);
        LedgerServiceClient client = getClientResponsibleFor(id);
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
        for (ledger.repository.model.Transaction t : repository.getTransactions(address, max)){
            responseObserver.onNext(proto.toMessage(t));
        }
        for (int curr_shard=0; curr_shard<num_shards; curr_shard++){
            if (curr_shard != this.shard){
                Iterator<Transaction> it = getClientForShard(curr_shard).getStub().getTransactions(request);
                while (it.hasNext()){
                    responseObserver.onNext(it.next());
                }
            }
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getAllTransactions(cs236351.ledger.Max request,
                                   io.grpc.stub.StreamObserver<cs236351.ledger.Transaction> responseObserver) {
        int max = request.getMax();
        for (ledger.repository.model.Transaction t : repository.getAllTransactions(max)){
            responseObserver.onNext(proto.toMessage(t));
        }
        for (int curr_shard=0; curr_shard<num_shards; curr_shard++){
            if (curr_shard != this.shard){
                Iterator<Transaction> it = getClientForShard(curr_shard).getStub().getAllTransactions(request);
                while (it.hasNext()){
                    responseObserver.onNext(it.next());
                }
            }
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getTransferCoins(cs236351.ledger.Transfer request,
                                 io.grpc.stub.StreamObserver<cs236351.ledger.Coins> responseObserver) {
    }

    @Override
    public void submitTransfer(cs236351.ledger.TransferAndTransactionID request,
                               io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        ledger.repository.model.Transfer transfer = proto.fromMessage(request.getTransfer());
        BigInteger transaction_id = proto.toBigInteger(request.getTransactionId());
        repository.submitTransfer(transfer, transaction_id);
        responseObserver.onNext(Empty.newBuilder().build());
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

    public void setShard(int shard){
        this.shard = shard;
    }

    public void setNumShards(int num_shards){
        this.num_shards = num_shards;
    }

    public void setServers(List<LedgerServiceClient> others_servers){
        this.other_servers = others_servers;
    }

    private int getShardResponsibleFor(BigInteger address){
        return address.remainder(BigInteger.valueOf(this.num_shards)).intValue();
    }

    private LedgerServiceClient getClientResponsibleFor(BigInteger address){
        int shard_responsible = address.remainder(BigInteger.valueOf(this.num_shards)).intValue();
        if (shard_responsible == this.shard){
            // leader not initialized
            if (!is_leader && leader==null){
                for (int i = 0; i<32; i++) {
                    try {
                        electLeader();
                        leader = getLeader();
                        is_leader = (leader == null);
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return leader;
        }
        else {
            // we should probably check who is alive first, maybe try all of them one by one...
            return getClientForShard(shard_responsible);
        }
    }

    private BigInteger generateTransactionID(){
        BigInteger max = null;
        try {
            String shard_str = String.valueOf(this.shard);
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

    private LedgerServiceClient getLeader() throws InterruptedException, KeeperException {
        String leader_address = null;
        String shared_str = String.valueOf(this.shard);
        List<String> timestamps = zk.getChildren("/leaders/" + shared_str);
        long ts_min = Long.MAX_VALUE;
        for (String t : timestamps) {
            String[] strings = t.split("-");
            String address = strings[0];
            long ts = Long.parseLong(strings[1]);
            if (ts < ts_min){
                ts_min = ts;
                leader_address = address;
            }
        }

        if (leader_address == null){
            throw new NoSuchElementException("leader not found in clients!");
        }

        String[] leader_data = leader_address.split(":");
        String leader_host = leader_data[0];
        int leader_port = Integer.parseInt(leader_data[1]);

        // this server is the leader!
        if (leader_host.equals(this.host) && leader_port == this.port){
            return null;
        }

        for (LedgerServiceClient client : other_servers){
            if (client.getHost().equals(leader_host) && client.getPort() == leader_port){
                return client;
            }
        }

        throw new NoSuchElementException("leader not found in clients!");
    }

    private void electLeader(){
        try {
            String shard_str = String.valueOf(this.shard);
            String port_str = String.valueOf(this.port);
            if (!zk.exists("/leaders/" + shard_str)){
                zk.create("/leaders/" + shard_str, null, CreateMode.PERSISTENT);
            }
            zk.create("/leaders/" + shard_str+ "/" + this.host + ":" + port_str +"-",
                    null, CreateMode.EPHEMERAL_SEQUENTIAL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private LedgerServiceClient getClientForShard(int shard){
        for (LedgerServiceClient client : this.other_servers){
            if (client.getShard() == shard){
                return client;
            }
        }
        throw new NoSuchElementException("shard doesn't exist");
    }
}
