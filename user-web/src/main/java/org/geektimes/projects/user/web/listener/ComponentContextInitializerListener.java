package org.geektimes.projects.user.web.listener;

import org.geektimes.context.ComponentContext;
import org.geektimes.projects.user.ioc.IoCContainer;
import org.geektimes.projects.user.repository.DatabaseUserRepository;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * {@link ComponentContext} 初始化器
 * ContextLoaderListener
 */
public class ComponentContextInitializerListener implements ServletContextListener {

    private ServletContext servletContext;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
//        this.servletContext = sce.getServletContext();
//        servletContext.log("servletContext initialized");
//        ComponentContext context = new ComponentContext();
//        context.init(servletContext);
        IoCContainer container = new IoCContainer();
        sce.getServletContext().setAttribute(IoCContainer.IoC_NAME, container);
        IoCContainer.addServletContext(getClass().getClassLoader(), sce.getServletContext());
        container.init();
        new DatabaseUserRepository().initDatabase();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
//        ComponentContext context = ComponentContext.getInstance();
//        context.destroy();
    }

}
