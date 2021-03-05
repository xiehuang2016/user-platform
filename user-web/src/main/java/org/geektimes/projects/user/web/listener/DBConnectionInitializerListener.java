package org.geektimes.projects.user.web.listener;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@WebListener
public class DBConnectionInitializerListener implements ServletContextListener {

    private ServletContext servletContext;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        servletContext = sce.getServletContext();
        try {
            Context context = (Context) new InitialContext().lookup("java:comp/env");
            // 依赖查找
            DataSource dataSource = (DataSource) context.lookup("jdbc/UserPlatformDB");
            Connection connection = null;
            try {
                connection = dataSource.getConnection();
            } catch (SQLException e) {
                servletContext.log(e.getMessage());
            }
            if (connection != null) {
                servletContext.log("get JNDI success");
            } else {
                servletContext.log("get JNDI fail");
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
