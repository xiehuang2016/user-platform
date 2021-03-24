package org.geektimes.projects.user.transaction;


import org.geektimes.projects.user.ioc.IoCContainer;
import org.geektimes.projects.user.proxy.AfterInvoker;
import org.geektimes.projects.user.proxy.BeforeInvoker;
import org.geektimes.projects.user.proxy.ThrowableInvoker;
import org.geektimes.projects.user.sql.DBConnectionManager;
import org.geektimes.projects.user.transaction.annotation.LocalPropagation;
import org.geektimes.projects.user.transaction.annotation.LocalTransactional;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 事务代理类的回调方法，在这里实现事务的控制
 *
 **/
public class TransactionalCallBack implements BeforeInvoker, AfterInvoker, ThrowableInvoker {

    /**
     * 每个线程的连接
     */
    public static ThreadLocal<Connection> connectionThreadLocal = new ThreadLocal<Connection>() {
        @Override
        protected Connection initialValue() {
            Connection con = ((DBConnectionManager) IoCContainer.getInstance().getObject("bean/DBConnectionManager")).getConnection();
            return con;
        }
    };

    /**
     * 每个线程的方法执行的方法嵌套数量
     */
    public static ThreadLocal<Integer> nestedThreadLocal = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 0;
        }
    };

    private static Logger logger = Logger.getLogger(TransactionalCallBack.class.getName());

    private int isolation;
    private int propagation;
    private Class<? extends Throwable>[] rollbackFor;
    private Savepoint savepoint = null;

    public TransactionalCallBack() {
    }

    /**
     * {@link LocalTransactional} 注解方法的，前置方法
     * 在该方法中，会判断是否要开启事务，并记录事务的相关信息
     * 如果是嵌套事务，要记录相应的保存点
     *
     * @param method 要执行的方法
     * @throws SQLException
     */
    @Override
    public void before(Object proxyObj, Object targetObj, Method method, Object[] methodArgs) {
        try {
            LocalTransactional ta = method.getAnnotation(LocalTransactional.class);
            if (ta != null) {
                this.isolation = ta.isolation();
                this.propagation = ta.propagation();
                this.rollbackFor = ta.rollbackFor();
                if (propagation == LocalPropagation.REQUIRED || ta.propagation() == LocalPropagation.PROPAGATION_NESTED) {
                    TransactionalCallBack.nestedThreadLocal.set(TransactionalCallBack.nestedThreadLocal.get() + 1);
                    TransactionalCallBack.connectionThreadLocal.get().setAutoCommit(false);
                    if (ta.propagation() == LocalPropagation.PROPAGATION_NESTED) {
                        savepoint = TransactionalCallBack.connectionThreadLocal.get().setSavepoint(method.getName());
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    /**
     * {@link LocalTransactional} 注解方法的，后置方法
     * 在该方法中 如果之前开启了事务，会提交事务
     * 如果是嵌套事务，会在最外层的方法执行完成之后一起提交
     *
     * @param method 执行的目标方法
     * @throws SQLException
     */
    @Override
    public void after(Object proxyObj, Object targetObj, Method method, Object[] methodArgs, Object methodResult) {
        try {
            LocalTransactional ta = method.getAnnotation(LocalTransactional.class);
            if (ta != null) {
                int count = TransactionalCallBack.nestedThreadLocal.get();
                if (count > 1) {
                    TransactionalCallBack.nestedThreadLocal.set(count - 1);
                } else {
                    TransactionalCallBack.connectionThreadLocal.get().commit();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getCause());
        }
    }


    @Override
    public void throwable(Object proxyObj, Object targetObj, Method method, Object[] methodArgs, Throwable throwable) {
        try {
            boolean isRollback = false;
            // 目标方法执行抛出了错误，如果开启了事务，需要执行事物的回滚
            // 如果嵌套事务，就根据保存点回滚事务
            LocalTransactional ta = method.getAnnotation(LocalTransactional.class);
            if (ta != null) {
                if (rollbackFor == null || rollbackFor.length == 0) {
                    rollbackFor = new Class[1];
                    rollbackFor[0] = Exception.class;
                }
                for (Class<?> clazz : rollbackFor) {
                    if (clazz.isAssignableFrom(throwable.getClass())) {
                        isRollback = true;
                    }
                }
                if (isRollback) {
                    if (ta.propagation() == LocalPropagation.PROPAGATION_NESTED) {
                        TransactionalCallBack.connectionThreadLocal.get().rollback(savepoint);
                    } else {
                        TransactionalCallBack.connectionThreadLocal.get().rollback();
                    }
                    TransactionalCallBack.nestedThreadLocal.set(TransactionalCallBack.nestedThreadLocal.get() - 1);
                }
            }
        } catch (SQLException e) {
            //throwable;
            logger.log(Level.SEVERE, throwable.getMessage());
        }
    }
}
