package lv.exosmium.exoplaytimevelocity.managers;

import lv.exosmium.exoplaytimevelocity.SQlite;

import java.sql.*;
import java.util.HashMap;

public class DatabaseManager {
    private final String sqlTableName = "playtime";
    private final SQlite sqliteapi;

    public DatabaseManager(String dataDirectory) throws ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        sqliteapi = new SQlite(dataDirectory, sqlTableName,
                "CREATE TABLE IF NOT EXISTS " + sqlTableName + " "
                + "(ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " + "" + "username TEXT NOT NULL, server TEXT NOT NULL, time_played TEXT NOT NULL)");
        sqliteapi.initSqlite();
    }

    public void addPlaytime(String username, String server, long playtime) {
        long oldPlaytime = getPlaytime(username, server);
        if (oldPlaytime == 0) {
            sqliteapi.executeUpdate(String.format("INSERT INTO %s (`username`, `server`, `time_played`) VALUES ('%s', '%s', '%s');", sqlTableName, username, server, playtime));
        } else {
            long newPlaytime = oldPlaytime + playtime;
            sqliteapi.executeUpdate(String.format("UPDATE %s SET `time_played`='%s' WHERE `server`='%s' AND `username`='%s';", sqlTableName, newPlaytime, server, username));
        }
    }

    private long getPlaytime(String username, String server) {
        long playtime = 0;
        ResultSet resultSet = sqliteapi.executeQuery(String.format("SELECT * FROM %s WHERE `username`='%s' AND `server`='%s';", sqlTableName, username, server));
        try {
            if (resultSet.next()) playtime = Integer.parseInt(resultSet.getString(4));
            resultSet.close();
        } catch (SQLException sqlException) { sqlException.printStackTrace(); }
        return playtime;
    }

    public HashMap<String, Long> getGlobalPlaytime(String username) {
        HashMap<String, Long> globalPlaytime = new HashMap<>();
        ResultSet resultSet = sqliteapi.executeQuery(String.format("SELECT * FROM %s WHERE `username`='%s';", sqlTableName, username));
        try {
            while (resultSet.next()) {
                globalPlaytime.put(resultSet.getString(3), Long.parseLong(resultSet.getString(4)));
            }
            resultSet.close();
        } catch (SQLException sqlException) { sqlException.printStackTrace();}
        return globalPlaytime;
    }
}
