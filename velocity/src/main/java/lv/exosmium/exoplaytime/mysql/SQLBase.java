package lv.exosmium.exoplaytime.mysql;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SQLBase {
    private final String tableUsername;
    private final String tablePassword;
    private final String tableUrl;

    private Connection connection = null;
    private Statement statement = null;

    public SQLBase(String tableDatabase, String tableHost, String tableUsername, String tablePassword) {
        this.tableUsername = tableUsername;
        this.tablePassword = tablePassword;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException classNotFoundException) { classNotFoundException.printStackTrace(); }
        this.tableUrl = "jdbc:mysql://" + tableHost + "/" + tableDatabase + "?useSSL=false" + "&allowPublicKeyRetrieval=true";
    }

    private Connection getSQLConnection() {
        try {
            return DriverManager.getConnection(this.tableUrl, this.tableUsername, this.tablePassword);
        }
        catch (SQLException sqlException) {
            sqlException.printStackTrace();
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
            try { if (connection != null) connection.close(); } catch (SQLException ignored) {}
            try { if (statement != null && !statement.isClosed()) statement.close(); } catch (SQLException ignored) {}
        }
    }

    public List<Map<String, Object>> executeQuery(String query) {
        ResultSet resultSet;
        List<Map<String, Object>> resultData = null;
        try {
            connection = getSQLConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);
            resultData = getListFromResultSet(resultSet);
            resultSet.close();
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        } finally {
            try { if (connection != null) connection.close(); } catch (SQLException ignored) {}
            try { if (statement != null && !statement.isClosed()) statement.close(); } catch (SQLException ignored) {}
        }
        return resultData;
    }

    private List<Map<String, Object>> getListFromResultSet(ResultSet resultSet) throws SQLException {
        List<Map<String, Object>> resultList = new ArrayList<>();
            Map<String, Object> row;
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (resultSet.next()) {
                row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), resultSet.getObject(i));
                }
                resultList.add(row);
            }
        return resultList;
    }
}
