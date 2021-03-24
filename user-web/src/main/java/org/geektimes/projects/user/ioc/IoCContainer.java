package org.geektimes.projects.user.ioc;

import org.geektimes.projects.user.proxy.ProxyCallBack;
import org.geektimes.projects.user.proxy.ProxyUtils;
import org.geektimes.projects.user.transaction.TransactionalCallBack;
import org.geektimes.projects.user.transaction.annotation.LocalTransactional;
import org.geektimes.projects.user.validator.proxy.ValidatorCallBack;
import org.geektimes.web.mvc.ioc.Container;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.naming.*;
import javax.servlet.ServletContext;
import javax.validation.Valid;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  IoC容器，从jndi容器中取bean
 * @author xiehuang 11108901
 * @create 2021/3/17 18:05
 */
public class IoCContainer implements Container {
    /**
     * jndi根路径
     */
    private static final String JNDI_ROOT_NAME = "java:comp/env";
    /**
     * IoC容器名称，初始化，使用该名称放在servletContext中
     */
    public static final String IoC_NAME = IoCContainer.class.getName();

    /**
     * 需要创建代理的 annotation
     */
    private List<Class<? extends Annotation>> needProxyAnnotations = new ArrayList<>();

    private List<String> jndiNames = new ArrayList<>();

    /**
     * 早期的bean，刚从jndi容器中取出来，还没执行@PostConstruct方法，还没执行依赖注入
     */
    private Map<String, Object> earlySingletonObjects = new HashMap<>();

    /**
     * IoC初始化后，完整的bean容器
     */
    private Map<String, Object> singletonObjects = new HashMap<>();

    /**
     * servletContext缓存
     */
    public static final Map<ClassLoader, ServletContext> currentContextPerThread =
            new ConcurrentHashMap<>(1);

    public static void addServletContext(ClassLoader classLoader, ServletContext context) {
        IoCContainer.currentContextPerThread.put(classLoader, context);
    }

    @Override
    public Object getObject(String jndiName) {
        Object obj = singletonObjects.get(jndiName);
        if (obj == null) {
            obj = earlySingletonObjects.get(jndiName);
            if (obj == null) {
                obj = loadJndiBean(jndiName);
            }
            if (obj == null) {
                throw new RuntimeException("no such bean , bean name : " + jndiName);
            }
            doInitalionlized(obj);
            doInject(obj);
            obj = doProxy(jndiName, obj);
            earlySingletonObjects.remove(jndiName);
            singletonObjects.put(jndiName, obj);
        }
        return obj;
    }

    /**
     * 从jndi中根据名字查找bean
     *
     * @param jndiName 名称
     * @return bean实例
     */
    private Object loadJndiBean(String jndiName) {
        Context context = null;
        try {
            context = new InitialContext();
            return context.lookup(JNDI_ROOT_NAME + "/" + jndiName);
        } catch (NamingException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    @Override
    public Container getParentContainer() {
        return null;
    }

    @Override
    public void setParentContainer(Container container) {
        throw new RuntimeException("no ");
    }

    /**
     * 将传递进来的对象，实现依赖注入
     *
     * @param obj 注入的对象
     */
    private void doInject(Object obj) {
        try {
            for (Field field : obj.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Resource.class)) {
                    String jndiName = field.getAnnotation(Resource.class).name();
                    Object fieldObj = getObject(jndiName);
                    if (field != null) {
                        field.setAccessible(true);
                        field.set(obj, fieldObj);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    /**
     * 创建代理对象
     *
     * @param jndiName bean名称
     * @param obj      被代理对象
     * @return 代理对象
     */
    private Object doProxy(String jndiName, Object obj) {
        Object result = obj;
        Method[] methods = obj.getClass().getDeclaredMethods();
        boolean isNeedProxy = false;
        for (Method method : methods) {
            for(Class<? extends Annotation> an : needProxyAnnotations) {
                if (method.isAnnotationPresent(an)) {
                    isNeedProxy = true;
                    break;
                }
            }
        }
        if (isNeedProxy) {
            ProxyCallBack callBack = new ProxyCallBack(obj);
            // 添加事务
            if(isNeedTransactionCallBack(methods)) {
                TransactionalCallBack transactionalCallBack = new TransactionalCallBack();
                callBack.addBeforeInvoker(transactionalCallBack);
                callBack.addThrowableInvoker(transactionalCallBack);
                callBack.addAfterInvoker(transactionalCallBack);
            }
            // 添加校验
            if(isNeedValidatorCallBack(methods)) {
                callBack.addBeforeInvoker(new ValidatorCallBack());
            }
            result = ProxyUtils.createProxy(obj.getClass(), callBack);
        }
        return result;
    }

    private boolean isNeedValidatorCallBack(Method[] methods) {
        for(Method method : methods) {
            for(Parameter parameter : method.getParameters()) {
                if(parameter.isAnnotationPresent(Valid.class)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isNeedTransactionCallBack(Method[] methods) {
        for(Method method : methods) {
            if(method.isAnnotationPresent(LocalTransactional.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 初始化过程，调用 @PostConstruct 的方法
     *
     * @param obj bean初始化对象
     */
    private void doInitalionlized(Object obj) {
        try {
            Method[] methods = obj.getClass().getMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(PostConstruct.class) &&
                        method.getParameterTypes().length == 0 &&
                        Modifier.STATIC != method.getModifiers()) {

                    method.invoke(obj);

                }
            }
        } catch (Throwable e) {
            throw new RuntimeException(e.getCause());
        }
    }

    private Object getObjectFromEarly(String jndiName) {
        return earlySingletonObjects.get(jndiName);
    }

    /**
     * IoC容器的初始化
     */
    public void init() {
        Context context = null;
        try {
            context = new InitialContext();
            this.needProxyAnnotations.add(LocalTransactional.class);
            loadJndiNames(context, JNDI_ROOT_NAME);
            loadBean(context);
        } catch (NamingException e) {
            e.printStackTrace();
        }

    }

    /**
     * 根据bean名称，从jndi容器中加载所有的bean出来，放到earlySingletonObjects中
     *
     * @param context jndi上下文
     */
    private void loadBean(Context context) {
        try {
            for (String name : jndiNames) {
                earlySingletonObjects.put(name.substring(JNDI_ROOT_NAME.length() + 1), context.lookup(name));
            }
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从jndi中加载出所有的bean名称
     *
     * @param context jndi上下文
     * @param name    jndi bean路径
     */
    private void loadJndiNames(Context context, String name) {
        try {
            Object obj = context.lookup(name);
            if (obj instanceof Context) {
                NamingEnumeration<NameClassPair> naming = ((Context) obj).list("");
                while (naming.hasMore()) {
                    NameClassPair nameClassPair = naming.nextElement();
                    loadJndiNames(context, name + "/" + nameClassPair.getName());
                }
            } else {
                jndiNames.add(name);
            }
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取IoC容器实例
     *
     * @return
     */
    public static IoCContainer getInstance() {
        ServletContext context = currentContextPerThread.get(Thread.currentThread().getContextClassLoader());
        return (IoCContainer) context.getAttribute(IoCContainer.IoC_NAME);
    }
}
