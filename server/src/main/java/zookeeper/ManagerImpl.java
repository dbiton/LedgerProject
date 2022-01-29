package zookeeper;

import ledger.service.LedgerServiceClient;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ManagerImpl implements Manager {
    private static ZooKeeper zkeeper;
    private static Connection zkConnection;

    public ManagerImpl(String host) throws IOException, InterruptedException {
        initialize(host);
    }

    private void initialize(String host) throws IOException, InterruptedException {
        zkConnection = new Connection();
        zkeeper = zkConnection.connect(host);
    }

    public void closeConnection() throws InterruptedException {
        zkConnection.close();
    }

    public void create(String path, byte[] data)
            throws InterruptedException,
            KeeperException {
        create(path, data, CreateMode.PERSISTENT);
    }

    public void create(String path, byte[] data, CreateMode createMode)
            throws KeeperException,
            InterruptedException {

        zkeeper.create(
                path,
                data,
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                createMode);
    }

    public Object getZNodeData(String path, boolean watchFlag) {
        byte[] b = null;
        try {
            b = zkeeper.getData(path, null, null);
            return new String(b, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    public void update(String path, byte[] data) throws KeeperException,
            InterruptedException {
        int version = zkeeper.exists(path, true).getVersion();
        zkeeper.setData(path, data, version);
    }

    public List<String> getChildren(final String path) throws InterruptedException, KeeperException {
        return getChildren(path, null);
    }

    public List<String> getChildren(final String path, Watcher watcher) throws InterruptedException, KeeperException {
        return zkeeper.getChildren(path, watcher);
    }

    public boolean exists(final String path) throws InterruptedException, KeeperException {
        return zkeeper.exists(path, null) != null;
    }

    public BigInteger generateTransactionID(int shard) {
        BigInteger max = null;
        try {
            String shard_str = String.valueOf(shard);
            if (!exists("/transactions")) {
                create("/transactions", null, CreateMode.PERSISTENT);
            }
            create("/transactions/" + shard_str + "-", null, CreateMode.EPHEMERAL_SEQUENTIAL);
            List<String> timestamps = getChildren("/transactions");
            for (String t : timestamps) {
                if (t.startsWith(shard_str)) {
                    String ts = t.substring(shard_str.length() + 1);
                    BigInteger n = new BigInteger(ts);
                    if (max == null || max.compareTo(n) < 0) {
                        max = n;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return max;
    }

    public List<String> getLeaderOrder(int shard) {
        String shared_str = String.valueOf(shard);
        List<String> timestamps = new ArrayList<>();
        try {
            timestamps = getChildren("/leaders/" + shared_str);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        Map<Long, String> leaders_sorted = new TreeMap<>();
        for (String t : timestamps) {
            String[] strings = t.split("-");
            String address = strings[0];
            Long ts = Long.valueOf(strings[1]);
            leaders_sorted.put(ts, address);
        }
        return new ArrayList<>(leaders_sorted.values());
    }

    public void electLeader(String host, int port, int shard) {
        try {
            String shard_str = String.valueOf(shard);
            String port_str = String.valueOf(port);
            if (!exists("/leaders")) {
                create("/leaders", null, CreateMode.PERSISTENT);
            }
            if (!exists("/leaders/" + shard_str)) {
                create("/leaders/" + shard_str, null, CreateMode.PERSISTENT);
            }
            create("/leaders/" + shard_str + "/" + host + ":" + port_str + "-",
                    null, CreateMode.EPHEMERAL_SEQUENTIAL);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void registerServer(String host, int port) {
        try {
            if (!exists("/servers")) {
                create("/servers", null, CreateMode.PERSISTENT);
            }
            create("/servers/" + host + ":" + port, null, CreateMode.EPHEMERAL);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public List<String> getOtherServers() {
        List<String> servers = null;
        try {
            servers = getChildren("/servers");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
        return servers;
    }
}