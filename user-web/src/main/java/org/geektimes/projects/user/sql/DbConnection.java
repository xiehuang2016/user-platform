package org.geektimes.projects.user.sql;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;

/**
 * @create 2021/3/3 14:40
 */
public class DbConnection {

    public static Connection getConnection() {
        try {
            Context ctx = new InitialContext();
            DataSource dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/UserPlatformDB");
            return dataSource.getConnection();
        } catch (Exception e) {
        }
        return null;
    }

}
