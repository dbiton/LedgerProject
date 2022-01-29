package zookeeper;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;

import java.math.BigInteger;
import java.util.List;

public interface Manager {
    public void create(String path, byte[] data)
            throws KeeperException, InterruptedException;
    public void create(String path, byte[] data, CreateMode createMode)
            throws KeeperException, InterruptedException;
    public Object getZNodeData(String path, boolean watchFlag);
    public void update(String path, byte[] data)
            throws KeeperException, InterruptedException;
    public List<String> getChildren(final String path)
            throws InterruptedException, KeeperException;
    public List<String> getChildren(final String path, Watcher watcher)
            throws InterruptedException, KeeperException;
    public boolean exists(final String path) throws InterruptedException, KeeperException;
    public void electLeader(String host, int port, int shard);
    public List<String> getLeaderOrder(int shard);
    public void registerServer(String host, int port);
    public List<String> getOtherServers();
    public BigInteger generateTransactionID(int shard);
}