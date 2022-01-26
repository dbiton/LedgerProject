package zookeeper;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
        }
        catch (Exception e){
            return null;
        }
    }

    public void update(String path, byte[] data) throws KeeperException,
            InterruptedException {
        int version = zkeeper.exists(path, true).getVersion();
        zkeeper.setData(path, data, version);
    }

    public List<String> getChildren(final String path) throws InterruptedException, KeeperException {
        return zkeeper.getChildren(path, null);
    }
}