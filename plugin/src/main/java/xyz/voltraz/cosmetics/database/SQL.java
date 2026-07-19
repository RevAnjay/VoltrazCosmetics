package xyz.voltraz.cosmetics.database;

import xyz.voltraz.cosmetics.VoltrazCosmetics;
import xyz.voltraz.cosmetics.cache.PlayerData;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public abstract class SQL {
    protected VoltrazCosmetics plugin = VoltrazCosmetics.getInstance();

    public abstract void createTable();

    public abstract void loadPlayer(Player player);

    public abstract CompletableFuture<PlayerData> loadPlayerAsync(Player player);

    public abstract void savePlayer(PlayerData playerData, boolean closed);

    public abstract CompletableFuture<Void> savePlayerAsync(PlayerData playerData);

    public abstract void savePlayers();

    public abstract DatabaseType getDatabaseType();

    public abstract void close();

    protected void closeConnections(PreparedStatement preparedStatement, Connection connection, ResultSet resultSet){
        if(connection == null) return;
        try{
            if(connection.isClosed()) return;
            if(resultSet != null) {
                resultSet.close();
            }
            if(preparedStatement != null) {
                preparedStatement.close();
            }
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
