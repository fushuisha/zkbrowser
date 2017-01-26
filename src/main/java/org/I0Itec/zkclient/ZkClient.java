//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.I0Itec.zkclient;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import javax.security.auth.login.Configuration;
import org.I0Itec.zkclient.DataUpdater;
import org.I0Itec.zkclient.ExceptionUtil;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkConnection;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkConnection;
import org.I0Itec.zkclient.ZkEventThread;
import org.I0Itec.zkclient.ZkLock;
import org.I0Itec.zkclient.ZkEventThread.ZkEvent;
import org.I0Itec.zkclient.exception.ZkAuthFailedException;
import org.I0Itec.zkclient.exception.ZkBadVersionException;
import org.I0Itec.zkclient.exception.ZkException;
import org.I0Itec.zkclient.exception.ZkInterruptedException;
import org.I0Itec.zkclient.exception.ZkNoNodeException;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.I0Itec.zkclient.exception.ZkTimeoutException;
import org.I0Itec.zkclient.serialize.SerializableSerializer;
import org.I0Itec.zkclient.serialize.ZkSerializer;
import org.I0Itec.zkclient.util.ZkPathUtil;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Op;
import org.apache.zookeeper.OpResult;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.KeeperException.ConnectionLossException;
import org.apache.zookeeper.KeeperException.SessionExpiredException;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZkClient implements Watcher {
    private static final Logger LOG = LoggerFactory.getLogger(ZkClient.class);
    protected static final String JAVA_LOGIN_CONFIG_PARAM = "java.security.auth.login.config";
    protected static final String ZK_SASL_CLIENT = "zookeeper.sasl.client";
    protected static final String ZK_LOGIN_CONTEXT_NAME_KEY = "zookeeper.sasl.clientconfig";
    protected final IZkConnection _connection;
    protected final long _operationRetryTimeoutInMillis;
    private final Map<String, Set<IZkChildListener>> _childListener;
    private final ConcurrentHashMap<String, Set<IZkDataListener>> _dataListener;
    private final Set<IZkStateListener> _stateListener;
    private KeeperState _currentState;
    private final ZkLock _zkEventLock;
    private boolean _shutdownTriggered;
    private ZkEventThread _eventThread;
    private Thread _zookeeperEventThread;
    private ZkSerializer _zkSerializer;
    private volatile boolean _closed;
    private boolean _isZkSaslEnabled;

    public ZkClient(String serverstring) {
        this((String)serverstring, 2147483647);
    }

    public ZkClient(String zkServers, int connectionTimeout) {
        this((IZkConnection)(new ZkConnection(zkServers)), connectionTimeout);
    }

    public ZkClient(String zkServers, int sessionTimeout, int connectionTimeout) {
        this((IZkConnection)(new ZkConnection(zkServers, sessionTimeout)), connectionTimeout);
    }

    public ZkClient(String zkServers, int sessionTimeout, int connectionTimeout, ZkSerializer zkSerializer) {
        this(new ZkConnection(zkServers, sessionTimeout), connectionTimeout, zkSerializer);
    }

    public ZkClient(String zkServers, int sessionTimeout, int connectionTimeout, ZkSerializer zkSerializer, long operationRetryTimeout) {
        this(new ZkConnection(zkServers, sessionTimeout), connectionTimeout, zkSerializer, operationRetryTimeout);
    }

    public ZkClient(IZkConnection connection) {
        this((IZkConnection)connection, 2147483647);
    }

    public ZkClient(IZkConnection connection, int connectionTimeout) {
        this(connection, connectionTimeout, new SerializableSerializer());
    }

    public ZkClient(IZkConnection zkConnection, int connectionTimeout, ZkSerializer zkSerializer) {
        this(zkConnection, connectionTimeout, zkSerializer, -1L);
    }

    public ZkClient(IZkConnection zkConnection, int connectionTimeout, ZkSerializer zkSerializer, long operationRetryTimeout) {
        this._childListener = new ConcurrentHashMap();
        this._dataListener = new ConcurrentHashMap();
        this._stateListener = new CopyOnWriteArraySet();
        this._zkEventLock = new ZkLock();
        if(zkConnection == null) {
            throw new NullPointerException("Zookeeper connection is null!");
        } else {
            this._connection = zkConnection;
            this._zkSerializer = zkSerializer;
            this._operationRetryTimeoutInMillis = operationRetryTimeout;
            this._isZkSaslEnabled = this.isZkSaslEnabled();
            this.connect((long)connectionTimeout, this);
        }
    }

    public void setZkSerializer(ZkSerializer zkSerializer) {
        this._zkSerializer = zkSerializer;
    }

    public List<String> subscribeChildChanges(String path, IZkChildListener listener) {
        Map var3 = this._childListener;
        synchronized(this._childListener) {
            Object listeners = (Set)this._childListener.get(path);
            if(listeners == null) {
                listeners = new CopyOnWriteArraySet();
                this._childListener.put(path, (Set<IZkChildListener>)listeners);
            }

            ((Set)listeners).add(listener);
        }

        return this.watchForChilds(path);
    }

    public void unsubscribeChildChanges(String path, IZkChildListener childListener) {
        Map var3 = this._childListener;
        synchronized(this._childListener) {
            Set listeners = (Set)this._childListener.get(path);
            if(listeners != null) {
                listeners.remove(childListener);
            }

        }
    }

    public void subscribeDataChanges(String path, IZkDataListener listener) {
        ConcurrentHashMap var4 = this._dataListener;
        synchronized(this._dataListener) {
            Object listeners = (Set)this._dataListener.get(path);
            if(listeners == null) {
                listeners = new CopyOnWriteArraySet();
                this._dataListener.put(path, (Set<IZkDataListener>)listeners);
            }

            ((Set)listeners).add(listener);
        }

        this.watchForData(path);
        LOG.debug("Subscribed data changes for " + path);
    }

    public void unsubscribeDataChanges(String path, IZkDataListener dataListener) {
        ConcurrentHashMap var3 = this._dataListener;
        synchronized(this._dataListener) {
            Set listeners = (Set)this._dataListener.get(path);
            if(listeners != null) {
                listeners.remove(dataListener);
            }

            if(listeners == null || listeners.isEmpty()) {
                this._dataListener.remove(path);
            }

        }
    }

    public void subscribeStateChanges(IZkStateListener listener) {
        Set var2 = this._stateListener;
        synchronized(this._stateListener) {
            this._stateListener.add(listener);
        }
    }

    public void unsubscribeStateChanges(IZkStateListener stateListener) {
        Set var2 = this._stateListener;
        synchronized(this._stateListener) {
            this._stateListener.remove(stateListener);
        }
    }

    public void unsubscribeAll() {
        Map var1 = this._childListener;
        synchronized(this._childListener) {
            this._childListener.clear();
        }

        ConcurrentHashMap var8 = this._dataListener;
        synchronized(this._dataListener) {
            this._dataListener.clear();
        }

        Set var9 = this._stateListener;
        synchronized(this._stateListener) {
            this._stateListener.clear();
        }
    }

    public void createPersistent(String path) throws ZkInterruptedException, IllegalArgumentException, ZkException, RuntimeException {
        this.createPersistent(path, false);
    }

    public void createPersistent(String path, boolean createParents) throws ZkInterruptedException, IllegalArgumentException, ZkException, RuntimeException {
        this.createPersistent(path, createParents, Ids.OPEN_ACL_UNSAFE);
    }

    public void createPersistent(String path, boolean createParents, List<ACL> acl) throws ZkInterruptedException, IllegalArgumentException, ZkException, RuntimeException {
        try {
            this.create(path, (Object)null, acl, CreateMode.PERSISTENT);
        } catch (ZkNodeExistsException var6) {
            if(!createParents) {
                throw var6;
            }
        } catch (ZkNoNodeException var7) {
            if(!createParents) {
                throw var7;
            }

            String parentDir = path.substring(0, path.lastIndexOf(47));
            this.createPersistent(parentDir, createParents, acl);
            this.createPersistent(path, createParents, acl);
        }

    }

    public void setAcl(final String path, final List<ACL> acl) throws ZkException {
        if(path == null) {
            throw new NullPointerException("Missing value for path");
        } else if(acl != null && acl.size() != 0) {
            if(!this.exists(path)) {
                throw new RuntimeException("trying to set acls on non existing node " + path);
            } else {
                this.retryUntilConnected(new Callable() {
                    public Void call() throws Exception {
                        Stat stat = new Stat();
                        ZkClient.this._connection.readData(path, stat, false);
                        ZkClient.this._connection.setAcl(path, acl, stat.getAversion());
                        return null;
                    }
                });
            }
        } else {
            throw new NullPointerException("Missing value for ACL");
        }
    }

    public Entry<List<ACL>, Stat> getAcl(final String path) throws ZkException {
        if(path == null) {
            throw new NullPointerException("Missing value for path");
        } else if(!this.exists(path)) {
            throw new RuntimeException("trying to get acls on non existing node " + path);
        } else {
            return (Entry)this.retryUntilConnected(new Callable() {
                public Entry<List<ACL>, Stat> call() throws Exception {
                    return ZkClient.this._connection.getAcl(path);
                }
            });
        }
    }

    public void createPersistent(String path, Object data) throws ZkInterruptedException, IllegalArgumentException, ZkException, RuntimeException {
        this.create(path, data, CreateMode.PERSISTENT);
    }

    public void createPersistent(String path, Object data, List<ACL> acl) {
        this.create(path, data, acl, CreateMode.PERSISTENT);
    }

    public String createPersistentSequential(String path, Object data) throws ZkInterruptedException, IllegalArgumentException, ZkException, RuntimeException {
        return this.create(path, data, CreateMode.PERSISTENT_SEQUENTIAL);
    }

    public String createPersistentSequential(String path, Object data, List<ACL> acl) throws ZkInterruptedException, IllegalArgumentException, ZkException, RuntimeException {
        return this.create(path, data, acl, CreateMode.PERSISTENT_SEQUENTIAL);
    }

    public void createEphemeral(String path) throws ZkInterruptedException, IllegalArgumentException, ZkException, RuntimeException {
        this.create(path, (Object)null, CreateMode.EPHEMERAL);
    }

    public void createEphemeral(String path, List<ACL> acl) throws ZkInterruptedException, IllegalArgumentException, ZkException, RuntimeException {
        this.create(path, (Object)null, acl, CreateMode.EPHEMERAL);
    }

    public String create(String path, Object data, CreateMode mode) throws ZkInterruptedException, IllegalArgumentException, ZkException, RuntimeException {
        return this.create(path, data, Ids.OPEN_ACL_UNSAFE, mode);
    }

    public String create(final String path, Object data, final List<ACL> acl, final CreateMode mode) {
        if(path == null) {
            throw new NullPointerException("Missing value for path");
        } else if(acl != null && acl.size() != 0) {
            final byte[] bytes = data == null?null:this.serialize(data);
            return (String)this.retryUntilConnected(new Callable() {
                public String call() throws Exception {
                    return ZkClient.this._connection.create(path, bytes, acl, mode);
                }
            });
        } else {
            throw new NullPointerException("Missing value for ACL");
        }
    }

    public void createEphemeral(String path, Object data) throws ZkInterruptedException, IllegalArgumentException, ZkException, RuntimeException {
        this.create(path, data, CreateMode.EPHEMERAL);
    }

    public void createEphemeral(String path, Object data, List<ACL> acl) throws ZkInterruptedException, IllegalArgumentException, ZkException, RuntimeException {
        this.create(path, data, acl, CreateMode.EPHEMERAL);
    }

    public String createEphemeralSequential(String path, Object data) throws ZkInterruptedException, IllegalArgumentException, ZkException, RuntimeException {
        return this.create(path, data, CreateMode.EPHEMERAL_SEQUENTIAL);
    }

    public String createEphemeralSequential(String path, Object data, List<ACL> acl) throws ZkInterruptedException, IllegalArgumentException, ZkException, RuntimeException {
        return this.create(path, data, acl, CreateMode.EPHEMERAL_SEQUENTIAL);
    }

    public void process(WatchedEvent event) {
        LOG.debug("Received event: " + event);
        this._zookeeperEventThread = Thread.currentThread();
        boolean stateChanged = event.getPath() == null;
        boolean znodeChanged = event.getPath() != null;
        boolean dataChanged = event.getType() == EventType.NodeDataChanged || event.getType() == EventType.NodeDeleted || event.getType() == EventType.NodeCreated || event.getType() == EventType.NodeChildrenChanged;
        this.getEventLock().lock();

        try {
            if(this.getShutdownTrigger()) {
                LOG.debug("ignoring event \'{" + event.getType() + " | " + event.getPath() + "}\' since shutdown triggered");
                return;
            }

            if(stateChanged) {
                this.processStateChanged(event);
            }

            if(dataChanged) {
                this.processDataOrChildChange(event);
            }
        } finally {
            if(stateChanged) {
                this.getEventLock().getStateChangedCondition().signalAll();
                if(event.getState() == KeeperState.Expired) {
                    this.getEventLock().getZNodeEventCondition().signalAll();
                    this.getEventLock().getDataChangedCondition().signalAll();
                    this.fireAllEvents();
                }
            }

            if(znodeChanged) {
                this.getEventLock().getZNodeEventCondition().signalAll();
            }

            if(dataChanged) {
                this.getEventLock().getDataChangedCondition().signalAll();
            }

            this.getEventLock().unlock();
            LOG.debug("Leaving process event");
        }

    }

    private void fireAllEvents() {
        Iterator i$ = this._childListener.entrySet().iterator();

        Entry entry;
        while(i$.hasNext()) {
            entry = (Entry)i$.next();
            this.fireChildChangedEvents((String)entry.getKey(), (Set)entry.getValue());
        }

        i$ = this._dataListener.entrySet().iterator();

        while(i$.hasNext()) {
            entry = (Entry)i$.next();
            this.fireDataChangedEvents((String)entry.getKey(), (Set)entry.getValue());
        }

    }

    public List<String> getChildren(String path) {
        return this.getChildren(path, this.hasListeners(path));
    }

    protected List<String> getChildren(final String path, final boolean watch) {
        return (List)this.retryUntilConnected(new Callable() {
            public List<String> call() throws Exception {
                return ZkClient.this._connection.getChildren(path, watch);
            }
        });
    }

    public int countChildren(String path) {
        try {
            return this.getChildren(path).size();
        } catch (ZkNoNodeException var3) {
            return 0;
        }
    }

    protected boolean exists(final String path, final boolean watch) {
        return ((Boolean)this.retryUntilConnected(new Callable() {
            public Boolean call() throws Exception {
                return Boolean.valueOf(ZkClient.this._connection.exists(path, watch));
            }
        })).booleanValue();
    }

    public boolean exists(String path) {
        return this.exists(path, this.hasListeners(path));
    }

    private void processStateChanged(WatchedEvent event) {
        LOG.info("zookeeper state changed (" + event.getState() + ")");
        this.setCurrentState(event.getState());
        if(!this.getShutdownTrigger()) {
            this.fireStateChangedEvent(event.getState());
            if(event.getState() == KeeperState.Expired) {
                try {
                    this.reconnect();
                    this.fireNewSessionEvents();
                } catch (Exception var3) {
                    LOG.info("Unable to re-establish connection. Notifying consumer of the following exception: ", var3);
                    this.fireSessionEstablishmentError(var3);
                }
            }

        }
    }

    private void fireNewSessionEvents() {
        Iterator i$ = this._stateListener.iterator();

        while(i$.hasNext()) {
            final IZkStateListener stateListener = (IZkStateListener)i$.next();
            this._eventThread.send(new ZkEvent("New session event sent to " + stateListener) {
                public void run() throws Exception {
                    stateListener.handleNewSession();
                }
            });
        }

    }

    private void fireStateChangedEvent(final KeeperState state) {
        Iterator i$ = this._stateListener.iterator();

        while(i$.hasNext()) {
            final IZkStateListener stateListener = (IZkStateListener)i$.next();
            this._eventThread.send(new ZkEvent("State changed to " + state + " sent to " + stateListener) {
                public void run() throws Exception {
                    stateListener.handleStateChanged(state);
                }
            });
        }

    }

    private void fireSessionEstablishmentError(final Throwable error) {
        Iterator i$ = this._stateListener.iterator();

        while(i$.hasNext()) {
            final IZkStateListener stateListener = (IZkStateListener)i$.next();
            this._eventThread.send(new ZkEvent("Session establishment error(" + error + ") sent to " + stateListener) {
                public void run() throws Exception {
                    stateListener.handleSessionEstablishmentError(error);
                }
            });
        }

    }

    private boolean hasListeners(String path) {
        Set dataListeners = (Set)this._dataListener.get(path);
        if(dataListeners != null && dataListeners.size() > 0) {
            return true;
        } else {
            Set childListeners = (Set)this._childListener.get(path);
            return childListeners != null && childListeners.size() > 0;
        }
    }

    public boolean deleteRecursive(String path) {
        List children;
        try {
            children = this.getChildren(path, false);
        } catch (ZkNoNodeException var5) {
            return true;
        }

        Iterator i$ = children.iterator();

        String subPath;
        do {
            if(!i$.hasNext()) {
                return this.delete(path);
            }

            subPath = (String)i$.next();
        } while(this.deleteRecursive(path + "/" + subPath));

        return false;
    }

    private void processDataOrChildChange(WatchedEvent event) {
        String path = event.getPath();
        Set listeners;
        if(event.getType() == EventType.NodeChildrenChanged || event.getType() == EventType.NodeCreated || event.getType() == EventType.NodeDeleted) {
            listeners = (Set)this._childListener.get(path);
            if(listeners != null && !listeners.isEmpty()) {
                this.fireChildChangedEvents(path, listeners);
            }
        }

        if(event.getType() == EventType.NodeDataChanged || event.getType() == EventType.NodeDeleted || event.getType() == EventType.NodeCreated) {
            listeners = (Set)this._dataListener.get(path);
            if(listeners != null && !listeners.isEmpty()) {
                this.fireDataChangedEvents(event.getPath(), listeners);
            }
        }

    }

    private void fireDataChangedEvents(final String path, Set<IZkDataListener> listeners) {
        Iterator i$ = listeners.iterator();

        while(i$.hasNext()) {
            final IZkDataListener listener = (IZkDataListener)i$.next();
            this._eventThread.send(new ZkEvent("Data of " + path + " changed sent to " + listener) {
                public void run() throws Exception {
                    ZkClient.this.exists(path, true);

                    try {
                        Object e = ZkClient.this.readData(path, (Stat)null, true);
                        listener.handleDataChange(path, e);
                    } catch (ZkNoNodeException var2) {
                        listener.handleDataDeleted(path);
                    }

                }
            });
        }

    }

    private void fireChildChangedEvents(final String path, Set<IZkChildListener> childListeners) {
        try {
            Iterator e = childListeners.iterator();

            while(e.hasNext()) {
                final IZkChildListener listener = (IZkChildListener)e.next();
                this._eventThread.send(new ZkEvent("Children of " + path + " changed sent to " + listener) {
                    public void run() throws Exception {
                        try {
                            ZkClient.this.exists(path);
                            List e = ZkClient.this.getChildren(path);
                            listener.handleChildChange(path, e);
                        } catch (ZkNoNodeException var2) {
                            listener.handleChildChange(path, (List)null);
                        }

                    }
                });
            }
        } catch (Exception var5) {
            LOG.error("Failed to fire child changed event. Unable to getChildren.  ", var5);
        }

    }

    public boolean waitUntilExists(String path, TimeUnit timeUnit, long time) throws ZkInterruptedException {
        Date timeout = new Date(System.currentTimeMillis() + timeUnit.toMillis(time));
        LOG.debug("Waiting until znode \'" + path + "\' becomes available.");
        if(this.exists(path)) {
            return true;
        } else {
            this.acquireEventLock();

            try {
                while(true) {
                    boolean e;
                    if(!this.exists(path, true)) {
                        e = this.getEventLock().getZNodeEventCondition().awaitUntil(timeout);
                        if(e) {
                            continue;
                        }

                        boolean var7 = false;
                        return var7;
                    }

                    e = true;
                    return e;
                }
            } catch (InterruptedException var11) {
                throw new ZkInterruptedException(var11);
            } finally {
                this.getEventLock().unlock();
            }
        }
    }

    protected Set<IZkDataListener> getDataListener(String path) {
        return (Set)this._dataListener.get(path);
    }

    public void showFolders(OutputStream output) {
        try {
            output.write(ZkPathUtil.toString(this).getBytes());
        } catch (IOException var3) {
            var3.printStackTrace();
        }

    }

    private boolean isZkSaslEnabled() {
        boolean isSecurityEnabled = false;
        boolean zkSaslEnabled = Boolean.parseBoolean(System.getProperty("zookeeper.sasl.client", "true"));
        String zkLoginContextName = System.getProperty("zookeeper.sasl.clientconfig", "Client");
        if(!zkSaslEnabled) {
            LOG.warn("Client SASL has been explicitly disabled with zookeeper.sasl.client");
            return false;
        } else {
            String loginConfigFile = System.getProperty("java.security.auth.login.config");
            if(loginConfigFile != null && loginConfigFile.length() > 0) {
                LOG.info("JAAS File name: " + loginConfigFile);
                File configFile = new File(loginConfigFile);
                if(!configFile.canRead()) {
                    throw new IllegalArgumentException("File " + loginConfigFile + "cannot be read.");
                }

                try {
                    Configuration e = Configuration.getConfiguration();
                    isSecurityEnabled = e.getAppConfigurationEntry(zkLoginContextName) != null;
                } catch (Exception var7) {
                    throw new ZkException(var7);
                }
            }

            return isSecurityEnabled;
        }
    }

    public void waitUntilConnected() throws ZkInterruptedException {
        this.waitUntilConnected(2147483647L, TimeUnit.MILLISECONDS);
    }

    public boolean waitUntilConnected(long time, TimeUnit timeUnit) throws ZkInterruptedException {
        return this._isZkSaslEnabled?this.waitForKeeperState(KeeperState.SaslAuthenticated, time, timeUnit):this.waitForKeeperState(KeeperState.SyncConnected, time, timeUnit);
    }

    public boolean waitForKeeperState(KeeperState keeperState, long time, TimeUnit timeUnit) throws ZkInterruptedException {
        if(this._zookeeperEventThread != null && Thread.currentThread() == this._zookeeperEventThread) {
            throw new IllegalArgumentException("Must not be done in the zookeeper event thread.");
        } else {
            Date timeout = new Date(System.currentTimeMillis() + timeUnit.toMillis(time));
            LOG.info("Waiting for keeper state " + keeperState);
            this.acquireEventLock();

            try {
                boolean e = true;

                do {
                    boolean var7;
                    if(this._currentState == keeperState) {
                        LOG.debug("State is " + this._currentState);
                        var7 = true;
                        return var7;
                    }

                    if(!e) {
                        var7 = false;
                        return var7;
                    }

                    e = this.getEventLock().getStateChangedCondition().awaitUntil(timeout);
                } while(this._currentState != KeeperState.AuthFailed || !this._isZkSaslEnabled);

                throw new ZkAuthFailedException("Authentication failure");
            } catch (InterruptedException var11) {
                throw new ZkInterruptedException(var11);
            } finally {
                this.getEventLock().unlock();
            }
        }
    }

    private void acquireEventLock() {
        try {
            this.getEventLock().lockInterruptibly();
        } catch (InterruptedException var2) {
            throw new ZkInterruptedException(var2);
        }
    }

    public <T> T retryUntilConnected(Callable<T> callable) throws ZkInterruptedException, IllegalArgumentException, ZkException, RuntimeException {
        if(this._zookeeperEventThread != null && Thread.currentThread() == this._zookeeperEventThread) {
            throw new IllegalArgumentException("Must not be done in the zookeeper event thread.");
        } else {
            long operationStartTime = System.currentTimeMillis();

            while(!this._closed) {
                try {
                    return callable.call();
                } catch (ConnectionLossException var5) {
                    Thread.yield();
                    this.waitForRetry();
                } catch (SessionExpiredException var6) {
                    Thread.yield();
                    this.waitForRetry();
                } catch (KeeperException var7) {
                    throw ZkException.create(var7);
                } catch (InterruptedException var8) {
                    throw new ZkInterruptedException(var8);
                } catch (Exception var9) {
                    throw ExceptionUtil.convertToRuntimeException(var9);
                }

                if(this._operationRetryTimeoutInMillis > -1L && System.currentTimeMillis() - operationStartTime >= this._operationRetryTimeoutInMillis) {
                    throw new ZkTimeoutException("Operation cannot be retried because of retry timeout (" + this._operationRetryTimeoutInMillis + " milli seconds)");
                }
            }

            throw new IllegalStateException("ZkClient already closed!");
        }
    }

    private void waitForRetry() {
        if(this._operationRetryTimeoutInMillis < 0L) {
            this.waitUntilConnected();
        } else {
            this.waitUntilConnected(this._operationRetryTimeoutInMillis, TimeUnit.MILLISECONDS);
        }
    }

    public void setCurrentState(KeeperState currentState) {
        this.getEventLock().lock();

        try {
            this._currentState = currentState;
        } finally {
            this.getEventLock().unlock();
        }

    }

    public ZkLock getEventLock() {
        return this._zkEventLock;
    }

    public boolean delete(String path) {
        return this.delete(path, -1);
    }

    public boolean delete(final String path, final int version) {
        try {
            this.retryUntilConnected(new Callable() {
                public Object call() throws Exception {
                    ZkClient.this._connection.delete(path, version);
                    return null;
                }
            });
            return true;
        } catch (ZkNoNodeException var4) {
            return false;
        }
    }

    private byte[] serialize(Object data) {
        return this._zkSerializer.serialize(data);
    }

    private <T> T derializable(byte[] data) {
        Object t = data == null?null:this._zkSerializer.deserialize(data);
        return (T)t;
    }

    public <T> T readData(String path) {
        return this.readData(path, false);
    }

    public <T> T readData(String path, boolean returnNullIfPathNotExists) {
        Object data = null;

        try {
            data = this.readData(path, (Stat)null);
        } catch (ZkNoNodeException var5) {
            if(!returnNullIfPathNotExists) {
                throw var5;
            }
        }

        return (T)data;
    }

    public <T> T readData(String path, Stat stat) {
        return this.readData(path, stat, this.hasListeners(path));
    }

    protected <T> T readData(final String path, final Stat stat, final boolean watch) {
        byte[] data = (byte[])this.retryUntilConnected(new Callable() {
            public byte[] call() throws Exception {
                return ZkClient.this._connection.readData(path, stat, watch);
            }
        });
        return this.derializable(data);
    }

    public byte[] readDataBytes(final String path) {
        final boolean watch = this.hasListeners(path);
        byte[] data = (byte[])this.retryUntilConnected(new Callable() {
            public byte[] call() throws Exception {
                return ZkClient.this._connection.readData(path, null, watch);
            }
        });
        return data;
    }


    public void writeData(String path, Object object) {
        this.writeData(path, object, -1);
    }

    public <T> void updateDataSerialized(String path, DataUpdater<T> updater) {
        Stat stat = new Stat();

        boolean retry;
        do {
            retry = false;

            try {
                Object e = this.readData(path, stat);
                Object newData = updater.update((T)e);
                this.writeData(path, newData, stat.getVersion());
            } catch (ZkBadVersionException var7) {
                retry = true;
            }
        } while(retry);

    }

    public void writeData(String path, Object datat, int expectedVersion) {
        this.writeDataReturnStat(path, datat, expectedVersion);
    }

    public Stat writeDataReturnStat(final String path, Object datat, final int expectedVersion) {
        final byte[] data = this.serialize(datat);
        return (Stat)this.retryUntilConnected(new Callable() {
            public Object call() throws Exception {
                Stat stat = ZkClient.this._connection.writeDataReturnStat(path, data, expectedVersion);
                return stat;
            }
        });
    }

    public void watchForData(final String path) {
        this.retryUntilConnected(new Callable() {
            public Object call() throws Exception {
                ZkClient.this._connection.exists(path, true);
                return null;
            }
        });
    }

    public List<String> watchForChilds(final String path) {
        if(this._zookeeperEventThread != null && Thread.currentThread() == this._zookeeperEventThread) {
            throw new IllegalArgumentException("Must not be done in the zookeeper event thread.");
        } else {
            return (List)this.retryUntilConnected(new Callable() {
                public List<String> call() throws Exception {
                    ZkClient.this.exists(path, true);

                    try {
                        return ZkClient.this.getChildren(path, true);
                    } catch (ZkNoNodeException var2) {
                        return null;
                    }
                }
            });
        }
    }

    public void addAuthInfo(final String scheme, final byte[] auth) {
        this.retryUntilConnected(new Callable() {
            public Object call() throws Exception {
                ZkClient.this._connection.addAuthInfo(scheme, auth);
                return null;
            }
        });
    }

    public void connect(long maxMsToWaitUntilConnected, Watcher watcher) throws ZkInterruptedException, ZkTimeoutException, IllegalStateException {
        boolean started = false;
        this.acquireEventLock();

        try {
            this.setShutdownTrigger(false);
            this._eventThread = new ZkEventThread(this._connection.getServers());
            this._eventThread.start();
            this._connection.connect(watcher);
            LOG.debug("Awaiting connection to Zookeeper server");
            boolean waitSuccessful = this.waitUntilConnected(maxMsToWaitUntilConnected, TimeUnit.MILLISECONDS);
            if(!waitSuccessful) {
                throw new ZkTimeoutException("Unable to connect to zookeeper server \'" + this._connection.getServers() + "\' with timeout of " + maxMsToWaitUntilConnected + " ms");
            }

            started = true;
        } finally {
            this.getEventLock().unlock();
            if(!started) {
                this.close();
            }

        }

    }

    public long getCreationTime(String path) {
        this.acquireEventLock();

        long e;
        try {
            e = this._connection.getCreateTime(path);
        } catch (KeeperException var8) {
            throw ZkException.create(var8);
        } catch (InterruptedException var9) {
            throw new ZkInterruptedException(var9);
        } finally {
            this.getEventLock().unlock();
        }

        return e;
    }

    public void close() throws ZkInterruptedException {
        if(!this._closed) {
            LOG.debug("Closing ZkClient...");
            this.getEventLock().lock();

            try {
                this.setShutdownTrigger(true);
                this._eventThread.interrupt();
                this._eventThread.join(2000L);
                this._connection.close();
                this._closed = true;
            } catch (InterruptedException var5) {
                throw new ZkInterruptedException(var5);
            } finally {
                this.getEventLock().unlock();
            }

            LOG.debug("Closing ZkClient...done");
        }
    }

    private void reconnect() {
        this.getEventLock().lock();

        try {
            this._connection.close();
            this._connection.connect(this);
        } catch (InterruptedException var5) {
            throw new ZkInterruptedException(var5);
        } finally {
            this.getEventLock().unlock();
        }

    }

    public void setShutdownTrigger(boolean triggerState) {
        this._shutdownTriggered = triggerState;
    }

    public boolean getShutdownTrigger() {
        return this._shutdownTriggered;
    }

    public int numberOfListeners() {
        int listeners = 0;

        Iterator i$;
        Set dataListeners;
        for(i$ = this._childListener.values().iterator(); i$.hasNext(); listeners += dataListeners.size()) {
            dataListeners = (Set)i$.next();
        }

        for(i$ = this._dataListener.values().iterator(); i$.hasNext(); listeners += dataListeners.size()) {
            dataListeners = (Set)i$.next();
        }

        listeners += this._stateListener.size();
        return listeners;
    }

    public List<OpResult> multi(final Iterable<Op> ops) throws ZkException {
        if(ops == null) {
            throw new NullPointerException("ops must not be null.");
        } else {
            return (List)this.retryUntilConnected(new Callable() {
                public List<OpResult> call() throws Exception {
                    return ZkClient.this._connection.multi(ops);
                }
            });
        }
    }
}
