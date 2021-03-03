package org.geektimes.projects.user.sql;

import javax.sql.DataSource;

/**
 * @create 2021/3/3 15:25
 */
public class DbDataSource {

    private static DataSource dataSource;

    public static DataSource getDataSource() {
        return dataSource;
    }

    public static void setDataSource(DataSource dataSource) {
        DbDataSource.dataSource = dataSource;
    }
}
