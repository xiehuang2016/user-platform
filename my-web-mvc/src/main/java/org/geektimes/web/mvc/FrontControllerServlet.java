package org.geektimes.web.mvc;

import org.apache.commons.lang.StringUtils;
import org.eclipse.microprofile.config.Config;
import org.geektimes.web.mvc.controller.Controller;
import org.geektimes.web.mvc.controller.PageController;
import org.geektimes.web.mvc.controller.RestController;
import org.geektimes.web.mvc.ioc.Container;
import org.geektimes.web.mvc.validator.ValidatorDelegate;

import javax.annotation.Resource;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.substringAfter;

public class FrontControllerServlet extends HttpServlet implements Container {

    /**
     * 请求路径和 Controller 的映射关系缓存
     */
    private Map<String, Controller> controllersMapping = new HashMap<>();

    /**
     * 请求路径和 {@link HandlerMethodInfo} 映射关系缓存
     */
    private Map<String, HandlerMethodInfo> handleMethodInfoMapping = new HashMap<>();

    /**
     * 初始化 Servlet
     *
     * @param servletConfig
     */
    @Override
    public void init(ServletConfig servletConfig) {
        ServletContext servletContext = servletConfig.getServletContext();
        Config config = (Config) servletContext.getAttribute("SERVLET_CONFIG");
        // 另外一个方式
        //ConfigProviderResolver configProviderResolver = ConfigProviderResolver.instance();
        //Config config = configProviderResolver.getConfig();
        System.out.println("web-mvc模块获取参数{application.name}:"+config.getValue("application.name",String.class));
        
        Container container = (Container) servletConfig.getServletContext().getAttribute("org.geektimes.projects.user.ioc.IocContainer");
        setParentContainer(container);
        initHandleMethods();
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 不包含应用上下文的路径，建立映射关系
        String requestURI = request.getRequestURI();
        // 应用的路径，contextPath  = /a or "/" or ""
        String servletContextPath = request.getContextPath();
        // 映射路径（子路径）
        String requestMappingPath = substringAfter(requestURI,
                StringUtils.replace(servletContextPath, "//", "/"));
        // 映射到 Controller
        Controller controller = controllersMapping.get(requestMappingPath);
        if (controller != null) {
            HandlerMethodInfo handlerMethodInfo = handleMethodInfoMapping.get(requestMappingPath);
            try {
                if (handlerMethodInfo != null) {
                    String httpMethod = request.getMethod();
                    // 不支持的http method
                    if (!handlerMethodInfo.getSupportedHttpMethods().contains(httpMethod)) {
                        // HTTP 方法不支持
                        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                        return;
                    }
                    if (controller instanceof PageController) {
                        pageControllerMethodHandler(request, response, controller);
                        return;
                    } else if (controller instanceof RestController) {
                        restControllerMethodHandler(request, response, controller, handlerMethodInfo);
                        return;
                    }

                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void pageControllerMethodHandler(HttpServletRequest request, HttpServletResponse response, Controller controller) throws Throwable {
        PageController pageController = PageController.class.cast(controller);
        String viewPath = pageController.execute(request, response);

        if(!viewPath.startsWith("/")) {
            viewPath = "/" + viewPath;
        }
        RequestDispatcher requestDispatcher = request.getRequestDispatcher(viewPath);
        requestDispatcher.forward(request, response);
    }

    private void restControllerMethodHandler(HttpServletRequest request, HttpServletResponse response, Controller controller, HandlerMethodInfo handlerMethodInfo) throws InvocationTargetException, IllegalAccessException, ServletException, IOException {
        RestController restController = RestController.class.cast(controller);
        Class<?>[] parameterTypes = handlerMethodInfo.getHandlerMethod().getParameterTypes();
        Object[] parameters = new Object[parameterTypes.length];
        for(int i=0; i<parameters.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if(parameterType.equals(HttpServletRequest.class)) {
                parameters[i] = request;
            } else if(parameterType.equals(HttpServletResponse.class)) {
                parameters[i] = response;
            } else {
                Object obj = convertRequestParamsToEntity(request.getParameterMap(), parameterType);
                parameters[i] = obj;
            }
        }
        Map<String, String> error = validateBean(handlerMethodInfo.getHandlerMethod(), parameters);
        Parameter[] methodParameters = handlerMethodInfo.getHandlerMethod().getParameters();
        for(int i=0; i<methodParameters.length; i++) {
            if("error".equals(methodParameters[i].getName()) &&
                    Map.class.isAssignableFrom(methodParameters[i].getType())) {
                parameters[i] = error;
                break;
            }
        }
        Object result = handlerMethodInfo.getHandlerMethod().invoke(restController, parameters);
        if(result instanceof String) {
            String str = String.class.cast(result);
            if(str.endsWith(".jsp") || str.endsWith(".html")) {
                request.getRequestDispatcher(str).forward(request, response);
                return;
            } else {
                response.setHeader("Content-type", "text/html;charset=UTF-8");
                response.getWriter().write(str);
                response.flushBuffer();
                return;
            }
        }
    }

    private Map<String, String> validateBean(Method handlerMethod, Object[] parameters) {
        Map<String, String> error = new HashMap<>();
        Annotation[][] parameterAnnotations = handlerMethod.getParameterAnnotations();
        ValidatorDelegate validatorDelegate = (ValidatorDelegate) getObject("bean/ValidatorDelegate");
        for(int i=0; i<parameters.length; i++) {
            Annotation[] annotations = parameterAnnotations[i];
            List<? extends Class<? extends Annotation>> an = Stream.of(annotations).map(a -> a.getClass()).collect(Collectors.toList());
            boolean isValidator = false;
            for (Class<? extends Annotation> aClass : an) {
                if(Valid.class.isAssignableFrom(aClass)) {
                    isValidator = true;
                    break;
                }
            }
            if(isValidator) {
                error.putAll(validatorDelegate.validate(parameters[i]));
            }
        }
        return error;
    }

    /**
     * 将 HttpServletRequest 请求中的参数，转换为对应的实体类
     * @param parameterMap 请求参数Map
     * @param parameterType 要转换的对象的类型
     * @return 转换后的对象
     */
    private Object convertRequestParamsToEntity(Map<String, String[]> parameterMap, Class<?> parameterType) {
        try {
            if(parameterType.isArray() || Collection.class.isAssignableFrom(parameterType) ||
                    Map.class.isAssignableFrom(parameterType)) {
                // TODO
                return null;
            } else {
                Object obj = parameterType.getConstructor().newInstance();
                BeanInfo beanInfo = Introspector.getBeanInfo(parameterType, Object.class);
                for (PropertyDescriptor property : beanInfo.getPropertyDescriptors()) {
                    String fieldName = property.getName();
                    // TODO
                    Object[] values = parameterMap.get(fieldName);
                    if (values != null && values.length > 0) {
                        Object val = values[0];
                        Method writeMethod = property.getWriteMethod();
                        writeMethod.invoke(obj, val);
                    }
                }
                return obj;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 读取所有的 RestController 的注解元信息 @Path
     * 利用 ServiceLoader 技术（Java SPI）
     */
    private void initHandleMethods() {
        try {
            for (Controller controller : ServiceLoader.load(Controller.class)) {
                // 方法解析
                Class<?> controllerClass = controller.getClass();
                for(Field field : controllerClass.getDeclaredFields()) {
                    if(field.isAnnotationPresent(Resource.class)) {
                        String name = field.getAnnotation(Resource.class).name();
                        Object filedVal = getObject(name);
                        field.setAccessible(true);
                        field.set(controller, filedVal);
                    }
                }
                Path controllerPath = controllerClass.getAnnotation(Path.class);
                String requestPath = controllerPath.value();
                Method[] publicMethods = controllerClass.getMethods();
                // 处理方法支持的 HTTP 方法集合
                for (Method method : publicMethods) {
                    Set<String> supportedHttpMethods = findSupportedHttpMethods(method);
                    Path methodPass = method.getAnnotation(Path.class);
                    if (methodPass != null) {
                        requestPath += methodPass.value();
                    }
                    handleMethodInfoMapping.put(requestPath,
                            new HandlerMethodInfo(requestPath, method, supportedHttpMethods));
                    // fix
                    controllersMapping.put(requestPath, controller);
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取 Method 方法上的 @Path 注解信息
     * 将方法上的注解的http方法信息保存到一个 set 中
     * 如果没有相应的注解，那么默认全部支持
     *
     * @param method 处理方法
     * @return
     */
    private Set<String> findSupportedHttpMethods(Method method) {
        Set<String> supportedHttpMethods = new LinkedHashSet<>();
        for (Annotation annotationFromMethod : method.getAnnotations()) {
            HttpMethod httpMethod = annotationFromMethod.annotationType().getAnnotation(HttpMethod.class);
            if (httpMethod != null) {
                supportedHttpMethods.add(httpMethod.value());
            }
        }

        if (supportedHttpMethods.isEmpty()) {
            supportedHttpMethods.addAll(asList(HttpMethod.GET, HttpMethod.POST,
                    HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.HEAD, HttpMethod.OPTIONS));
        }

        return supportedHttpMethods;
    }

    @Override
    public Object getObject(String name) {
        Object obj = controllersMapping.get(name);
        if(obj == null) {
            obj = getParentContainer().getObject(name);
        }
        return obj;
    }

    private Container parentContainer;

    @Override
    public Container getParentContainer() {
        return this.parentContainer;
    }

    @Override
    public void setParentContainer(Container container) {
        this.parentContainer = container;
    }

}
