package lv.exosmium.exoplaytimevelocity;

import java.io.File;
import java.io.IOException;
import java.sql.*;

public class SQlite {
    private final String tableDirectory;
    private final String tableName;
    private final String tableCreateQuery;

    private Connection connection = null;
    private Statement statement = null;

    public SQlite(String tableDirectory, String tableName, String tableCreateQuery) {
        this.tableDirectory = tableDirectory;
        this.tableName = tableName;
        this.tableCreateQuery = tableCreateQuery;
    }

    public void initSqlite() {
        try {
            connection = getSQLConnection();
            statement = connection.createStatement();
            statement.executeUpdate(tableCreateQuery);
            statement.close();
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Connection getSQLConnection() {
        File dataFolder = new File(tableDirectory, tableName + ".db");
        if (!dataFolder.exists()) {
            try {
                dataFolder.createNewFile();
            } catch (IOException e) {
                System.out.println("Ошибка записи файла: " + tableName + ".db");
            }
        }
        try {
            if (connection != null && !connection.isClosed()) {
                return connection;
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder);
            return connection;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void executeUpdate(String query) {
        try {
            connection = getSQLConnection();
            statement = connection.createStatement();
            statement.executeUpdate(query);
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        } finally {
            try { connection.close(); } catch (Exception ignored) {}
            try { statement.close(); } catch (Exception ignored) {}
        }
    }

    public ResultSet executeQuery(String query) {
        ResultSet resultSet = null;
        try {
            connection = getSQLConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
        return resultSet;
    }
}
