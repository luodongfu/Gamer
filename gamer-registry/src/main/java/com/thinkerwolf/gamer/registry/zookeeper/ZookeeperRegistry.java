package com.thinkerwolf.gamer.registry.zookeeper;

import com.thinkerwolf.gamer.common.URL;
import com.thinkerwolf.gamer.registry.AbstractRegistry;
import com.thinkerwolf.gamer.registry.ChildEvent;
import com.thinkerwolf.gamer.registry.DataEvent;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.lang.StringUtils;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;

import java.util.ArrayList;
import java.util.List;

/**
 * Zookeeper注册中心
 * <lu>
 * <li>/gamer/rpc</li>
 * <li>/eliminate/game/game_1001</li>
 * <li>/eliminate/login/login_1001</li>
 * <li>/eliminate/match/match_1001</li>
 * </lu>
 *
 * @author wukai
 * @since 2020-05-21
 */
public class ZookeeperRegistry extends AbstractRegistry implements IZkStateListener, IZkDataListener, IZkChildListener {

    private ZkClient zkClient;
    private String root;

    public ZookeeperRegistry(URL url) {
        super(url);
        init();
    }

    private void init() {
        int connectionTimeout = url.getInteger(URL.CONNECTION_TIMEOUT, 5000);
        int sessionTimeout = url.getInteger(URL.SESSION_TIMEOUT, 6000);
        String backup = url.getString(URL.BACKUP);
        String zkServers = url.toHostPort();
        if (backup != null) {
            zkServers = zkServers + ";" + backup;
        }
        this.zkClient = new ZkClient(zkServers, sessionTimeout, connectionTimeout, new AdaptiveZkSerializer());
        this.zkClient.subscribeStateChanges(this);
        this.root = ZkUtils.toPath(url);

    }

    private String toDataPath(URL url) {
        String p = ZkUtils.toPath(url);
        String nodeName = url.getString(URL.NODE_NAME);
        if (StringUtils.isBlank(nodeName)) {
            throw new RuntimeException("Node name is blank");
        }
        if ("/".equals(p)) {
            return root + "/" + nodeName;
        }
        return root + p + "/" + nodeName;
    }

    @Override
    protected void doRegister(URL url) {
        String path = toDataPath(url);
        boolean ephemeral = url.getBoolean(URL.NODE_EPHEMERAL, true);
        ZkUtils.createRecursive(zkClient, path);
        if (!zkClient.exists(path)) {
            List<ACL> acls = ZkUtils.createACLs(url);
            if (ephemeral) {
                zkClient.createEphemeral(path, url, acls);
            } else {
                zkClient.createPersistent(path, url, acls);
            }
        } else {
            zkClient.writeData(path, url);
        }
    }

    @Override
    public void doUnRegister(URL url) {
        String path = toDataPath(url);
        zkClient.delete(path);
    }

    @Override
    protected void doSubscribe(URL url) {
        String path = toDataPath(url);
        zkClient.subscribeDataChanges(path, this);
        zkClient.subscribeChildChanges(path, this);
    }

    @Override
    protected void doUnSubscribe(URL url) {
        String path = toDataPath(url);
        zkClient.unsubscribeChildChanges(path, this);
        zkClient.unsubscribeDataChanges(path, this);
    }


    @Override
    protected List<URL> doLookup(URL url) {
        String path = toDataPath(url);
        List<String> childrenPaths = zkClient.getChildren(path);
        List<URL> urls = new ArrayList<>();
        for (String cp : childrenPaths) {
            URL u = zkClient.readData(path + "/" + cp, false);
            urls.add(u);
        }
        return urls;
    }

    @Override
    public void close() {
        zkClient.close();
    }

    @Override
    public void handleStateChanged(Watcher.Event.KeeperState state) throws Exception {
    }

    @Override
    public void handleNewSession() throws Exception {

    }

    @Override
    public void handleSessionEstablishmentError(Throwable error) throws Exception {

    }

    @Override
    public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
        List<Object> childPaths = new ArrayList<>(currentChilds);
        ChildEvent event = new ChildEvent(internalToKey(parentPath), childPaths);
        notifyChild(event);
    }

    @Override
    public void handleDataChange(String dataPath, Object data) throws Exception {
        DataEvent event = new DataEvent(internalToKey(dataPath), (URL) data);
        notifyData(event);
    }

    @Override
    public void handleDataDeleted(String dataPath) throws Exception {
        DataEvent event = new DataEvent(internalToKey(dataPath), null);
        notifyData(event);
    }

    @Override
    protected String toPathKey(URL url) {
        return internalToKey(toDataPath(url));
    }

    private String internalToKey(String path) {
        String k = path.replace('/', '.');
        if (k.charAt(0) == '.') {
            return k.substring(1);
        }
        return k;
    }

}
