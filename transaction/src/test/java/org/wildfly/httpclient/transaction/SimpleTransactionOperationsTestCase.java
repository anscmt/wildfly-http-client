package org.wildfly.httpclient.transaction;

import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.common.annotation.NotNull;
import org.wildfly.httpclient.common.HTTPTestServer;
import org.wildfly.transaction.client.ImportResult;
import org.wildfly.transaction.client.LocalTransaction;
import org.wildfly.transaction.client.LocalTransactionContext;
import org.wildfly.transaction.client.RemoteUserTransaction;
import org.wildfly.transaction.client.SimpleXid;
import org.wildfly.transaction.client.XAImporter;
import org.wildfly.transaction.client.spi.LocalTransactionProvider;
import org.wildfly.transaction.client.spi.SubordinateTransactionControl;
import io.undertow.server.handlers.CookieImpl;

/**
 * TODO: this test needs a lot of work. It does not really test much at the moment.
 *
 * @author Stuart Douglas
 */
@RunWith(HTTPTestServer.class)
public class SimpleTransactionOperationsTestCase {

    static volatile Xid lastXid;
    static final Map<Xid, TestTransaction> transactions = new ConcurrentHashMap<>();

    @Before
    public void setup() {
        HTTPTestServer.registerServicesHandler("common/v1/affinity", exchange -> exchange.getResponseCookies().put("JSESSIONID", new CookieImpl("JSESSIONID", "foo")));
        HTTPTestServer.registerServicesHandler("txn", new HttpRemoteTransactionService(new LocalTransactionContext(new LocalTransactionProvider() {
            @Override
            public TransactionManager getTransactionManager() {
                return new TransactionManager() {
                    @Override
                    public void begin() throws NotSupportedException, SystemException {
                        
                    }

                    @Override
                    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {

                    }

                    @Override
                    public void rollback() throws IllegalStateException, SecurityException, SystemException {

                    }

                    @Override
                    public void setRollbackOnly() throws IllegalStateException, SystemException {

                    }

                    @Override
                    public int getStatus() throws SystemException {
                        return 0;
                    }

                    @Override
                    public Transaction getTransaction() throws SystemException {
                        return null;
                    }

                    @Override
                    public void setTransactionTimeout(int seconds) throws SystemException {

                    }

                    @Override
                    public Transaction suspend() throws SystemException {
                        return null;
                    }

                    @Override
                    public void resume(Transaction tobj) throws InvalidTransactionException, IllegalStateException, SystemException {

                    }
                };
            }

            @Override
            public XAImporter getXAImporter() {
                return new XAImporter() {

                    public ImportResult<?> findOrImportTransaction(Xid xid, int timeout) throws XAException {
                        TestTransaction existing = transactions.get(xid);

                        return new ImportResult<Transaction>(existing, new SubordinateTransactionControl() {
                            @Override
                            public void rollback() throws XAException {
                                try {
                                    existing.rollback();
                                } catch (SystemException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                            @Override
                            public void end(int flags) throws XAException {

                            }

                            @Override
                            public void beforeCompletion() throws XAException {

                            }

                            @Override
                            public int prepare() throws XAException {
                                return 0;
                            }

                            @Override
                            public void forget() throws XAException {

                            }

                            @Override
                            public void commit(boolean onePhase) throws XAException {

                            }
                        }, false);
                    }

                    @Override
                    public ImportResult<?> findOrImportTransaction(Xid xid, int i, boolean b) throws XAException {
                        if(b && !transactions.containsKey(xid)) {
                            return null;
                        }
                        return findOrImportTransaction(xid, i);
                    }

                    @Override
                    public Transaction findExistingTransaction(Xid xid) throws XAException {
                        return transactions.get(xid);
                    }

                    @Override
                    public void commit(Xid xid, boolean onePhase) throws XAException {
                        throw new RuntimeException();
                    }

                    @Override
                    public void forget(Xid xid) throws XAException {
                        throw new RuntimeException();
                    }

                    @Override
                    public Xid[] recover(int flag, String parentName) throws XAException {
                        throw new RuntimeException();
                    }
                };
            }

            @Override
            public Transaction createNewTransaction(int timeout) throws SystemException, SecurityException {
                TestTransaction testTransaction = new TestTransaction();
                transactions.put(testTransaction.getXid(), testTransaction);
                return testTransaction;
            }

            @Override
            public boolean isImported(@NotNull Transaction transaction) throws IllegalArgumentException {
                return false;
            }

            @Override
            public void registerInterposedSynchronization(@NotNull Transaction transaction, @NotNull Synchronization sync) throws IllegalArgumentException {

            }

            @Override
            public Object getResource(@NotNull Transaction transaction, @NotNull Object key) {
                return null;
            }

            @Override
            public void putResource(@NotNull Transaction transaction, @NotNull Object key, Object value) throws IllegalArgumentException {

            }

            @Override
            public boolean getRollbackOnly(@NotNull Transaction transaction) throws IllegalArgumentException {
                return false;
            }

            @Override
            public Object getKey(@NotNull Transaction transaction) throws IllegalArgumentException {
                return null;
            }

            @Override
            public void commitLocal(@NotNull Transaction transaction) throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {

            }

            @Override
            public void rollbackLocal(@NotNull Transaction transaction) throws IllegalStateException, SystemException {

            }

            @Override
            public void dropLocal(@NotNull Transaction transaction) {

            }

            @Override
            public int getTimeout(@NotNull Transaction transaction) {
                return 0;
            }

            @Override
            public Xid getXid(@NotNull Transaction transaction) {
                return null;
            }

            @Override
            public String getNodeName() {
                return null;
            }

            @Override
            public <T> T getProviderInterface(Transaction transaction, Class<T> providerInterfaceType) {
                return providerInterfaceType.isInstance(transaction) ? (T) transaction : null;
            }
        }), localTransaction -> localTransaction.getProviderInterface(TestTransaction.class).getXid()).createHandler());


    }

    private InitialContext createContext() throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        env.put(Context.PROVIDER_URL, HTTPTestServer.getDefaultServerURL());
        return new InitialContext(env);
    }

    @Test
    public void testCreateTransaction() throws Exception {
        InitialContext ic = createContext();
        RemoteUserTransaction result = (RemoteUserTransaction) ic.lookup("txn:UserTransaction");
        result.begin();
        result.commit();

    }


    private static final class TestTransaction implements Transaction {

        private final Xid xid;

        private TestTransaction() {
            byte[] global = new byte[10];
            byte[] branch = new byte[10];
            new Random().nextBytes(global);
            new Random().nextBytes(branch);
            xid = new SimpleXid(1, global, branch);
            lastXid = xid;
        }


        @Override
        public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, SystemException {

        }

        @Override
        public void rollback() throws IllegalStateException, SystemException {

        }

        @Override
        public void setRollbackOnly() throws IllegalStateException, SystemException {

        }

        @Override
        public int getStatus() throws SystemException {
            return 0;
        }

        @Override
        public boolean enlistResource(XAResource xaRes) throws RollbackException, IllegalStateException, SystemException {
            return false;
        }

        @Override
        public boolean delistResource(XAResource xaRes, int flag) throws IllegalStateException, SystemException {
            return false;
        }

        @Override
        public void registerSynchronization(Synchronization sync) throws RollbackException, IllegalStateException, SystemException {

        }

        public Xid getXid() {
            return xid;
        }
    }

}
