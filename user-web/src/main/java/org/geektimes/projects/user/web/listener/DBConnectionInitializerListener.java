package org.geektimes.projects.user.web.listener;

import org.apache.tomcat.dbcp.dbcp.BasicDataSource;
import org.geektimes.projects.user.sql.DBConnectionManager;
import org.geektimes.projects.user.sql.DbDataSource;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.ServiceLoader;

@WebListener
public class DBConnectionInitializerListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
//        initJNDIDataSource();

        ServletContext servletContext = sce.getServletContext();
        initDataSource(servletContext);
        initData();
    }



    private void initJNDIDataSource() {
        try {
            Context context = new InitialContext();
            Context envContext = (Context) context.lookup("java:/comp/env");

            DataSource dataSource = (DataSource) envContext.lookup("jdbc/UserPlatformDB");
            ServiceLoader<Driver> serviceLoader = ServiceLoader.load(Driver.class);
            Iterator<Driver> iterator = serviceLoader.iterator();
            if(iterator.hasNext()) {
                Driver driver = iterator.next();
                if(dataSource instanceof BasicDataSource) {
                    ((BasicDataSource) dataSource).setDriverClassLoader(driver.getClass().getClassLoader());
                }
            }
            Connection connection = dataSource.getConnection();
            DbDataSource.setDataSource(dataSource);
        } catch (NamingException | SQLException e) {
            e.printStackTrace();
        }
    }

    private void initDataSource(ServletContext servletContext) {
        servletContext.log("initDataSource start");
        String databaseURL = "jdbc:derby:user-platform;create=true";
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setUrl(databaseURL);

        DbDataSource.setDataSource(dataSource);
    }


    private void initData() {
        try {
            Statement statement = DbDataSource.getDataSource().getConnection().createStatement();
            statement.execute(DBConnectionManager.CREATE_USERS_TABLE_DDL_SQL);
            statement.executeUpdate(DBConnectionManager.INSERT_USER_DML_SQL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
