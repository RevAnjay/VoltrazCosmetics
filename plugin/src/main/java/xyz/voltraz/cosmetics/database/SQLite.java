package xyz.voltraz.cosmetics.database;

import xyz.voltraz.cosmetics.api.CosmeticType;
import xyz.voltraz.cosmetics.cache.PlayerData;
import xyz.voltraz.cosmetics.events.PlayerDataLoadEvent;
import xyz.voltraz.cosmetics.nms.bag.EntityBag;
import xyz.voltraz.cosmetics.nms.balloon.EntityBalloon;
import xyz.voltraz.cosmetics.nms.balloon.PlayerBalloon;
import xyz.voltraz.cosmetics.nms.spray.CustomSpray;
import xyz.voltraz.cosmetics.utils.FoliaUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLite extends SQL {
    private final File fileSQL;
    private final HikariCP hikariCP;

    public SQLite() {
        hikariCP = new HikariCP();
        fileSQL = new File(plugin.getDataFolder(), "cosmetics.db");
        hikariCP.setProperties(this);
        createTable();
    }

    @Override
    public void createTable() {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = hikariCP.getHikariDataSource().getConnection();
            preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS player_cosmetics (id INTEGER PRIMARY KEY AUTOINCREMENT, UUID VARCHAR(255), Player VARCHAR(255), Hat VARCHAR(255), Bag VARCHAR(255), WStick VARCHAR(255), Balloon VARCHAR(255), Spray VARCHAR(255), Available VARCHAR(10000))");
            preparedStatement.executeUpdate();
            plugin.getLogger().info("SQLite table created successfully");
        } catch (SQLException throwable) {
            plugin.getLogger().severe("Could not create table: " + throwable.getMessage());
        } finally {
            closeConnections(preparedStatement, connection, null);
        }
    }

    @Override
    public void loadPlayer(Player player) {
        loadPlayerInfo(player);
    }

    @Override
    public CompletableFuture<PlayerData> loadPlayerAsync(Player player) {
        return loadPlayerInfoAsync(player);
    }

    @Override
    public void savePlayer(PlayerData playerData, boolean close) {
        savePlayerInfo(playerData, close);
    }

    @Override
    public CompletableFuture<Void> savePlayerAsync(PlayerData playerData) {
        return savePlayerInfoAsync(playerData);
    }

    @Override
    public void savePlayers() {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try{
            connection = hikariCP.getHikariDataSource().getConnection();
            for(PlayerData player : PlayerData.players.values()){
                try {
                    player.clearCosmeticsToSaveData();
                    String playerName = resolvePlayerName(player);
                    if(!checkInfo(player.getUniqueId())){
                        String query = "INSERT INTO player_cosmetics (id, UUID, Player, Hat, Bag, WStick, Balloon, Spray, Available) VALUES(NULL, ?, ?, ?, ?, ?, ?, ?, ?);";
                        preparedStatement = connection.prepareStatement(query);
                        preparedStatement.setString(1, player.getUniqueId().toString());
                        preparedStatement.setString(2, playerName);
                        preparedStatement.setString(3, player.getHat() == null ? "" : player.getHat().getId());
                        preparedStatement.setString(4, player.getBag() == null ? "" : player.getBag().getId());
                        preparedStatement.setString(5, player.getWStick() == null ? "" : player.getWStick().getId());
                        preparedStatement.setString(6, player.getBalloon() == null ? "" : player.getBalloon().getId());
                        preparedStatement.setString(7, player.getSpray() == null ? "" : player.getSpray().getId());
                        preparedStatement.setString(8, player.saveCosmetics());
                        preparedStatement.executeUpdate();

                    }else {
                        String query = "UPDATE player_cosmetics SET Player = ?, Hat = ?, Bag = ?, WStick = ?, Balloon = ?, Spray = ?, Available = ? WHERE UUID = ?";
                        preparedStatement = connection.prepareStatement(query);
                        preparedStatement.setString(1, playerName);
                        preparedStatement.setString(2, player.getHat() == null ? "" : player.getHat().getId());
                        preparedStatement.setString(3, player.getBag() == null ? "" : player.getBag().getId());
                        preparedStatement.setString(4, player.getWStick() == null ? "" : player.getWStick().getId());
                        preparedStatement.setString(5, player.getBalloon() == null ? "" : player.getBalloon().getId());
                        preparedStatement.setString(6, player.getSpray() == null ? "" : player.getSpray().getId());
                        preparedStatement.setString(7, player.saveCosmetics());
                        preparedStatement.setString(8, player.getUniqueId().toString());
                        preparedStatement.executeUpdate();
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to save data for player " + player.getUniqueId() + ": " + e.getMessage());
                }
            }
        }catch (SQLException throwable) {
            plugin.getLogger().severe("Failed to save player information: " + throwable.getMessage());
        }finally {
            closeConnections(preparedStatement, connection, null);
        }
    }

    private void savePlayerInfo(PlayerData player, boolean close){
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ensureOfflinePlayer(player);
        try{
            connection = hikariCP.getHikariDataSource().getConnection();
            String playerName = resolvePlayerName(player);
            if(!checkInfo(player.getUniqueId())){
                String query = "INSERT INTO player_cosmetics (id, UUID, Player, Hat, Bag, WStick, Balloon, Spray, Available) VALUES(NULL, ?, ?, ?, ?, ?, ?, ?, ?);";
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, player.getUniqueId().toString());
                preparedStatement.setString(2, playerName);
                preparedStatement.setString(3, player.getHat() == null ? "" : player.getHat().getId());
                preparedStatement.setString(4, player.getBag() == null ? "" : player.getBag().getId());
                preparedStatement.setString(5, player.getWStick() == null ? "" : player.getWStick().getId());
                preparedStatement.setString(6, player.getBalloon() == null ? "" : player.getBalloon().getId());
                preparedStatement.setString(7, player.getSpray() == null ? "" : player.getSpray().getId());
                preparedStatement.setString(8, player.saveCosmetics());
                preparedStatement.executeUpdate();

            }else {
                String query = "UPDATE player_cosmetics SET Player = ?, Hat = ?, Bag = ?, WStick = ?, Balloon = ?, Spray = ?, Available = ? WHERE UUID = ?";
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, playerName);
                preparedStatement.setString(2, player.getHat() == null ? "" : player.getHat().getId());
                preparedStatement.setString(3, player.getBag() == null ? "" : player.getBag().getId());
                preparedStatement.setString(4, player.getWStick() == null ? "" : player.getWStick().getId());
                preparedStatement.setString(5, player.getBalloon() == null ? "" : player.getBalloon().getId());
                preparedStatement.setString(6, player.getSpray() == null ? "" : player.getSpray().getId());
                preparedStatement.setString(7, player.saveCosmetics());
                preparedStatement.setString(8, player.getUniqueId().toString());
                preparedStatement.executeUpdate();
            }
        }catch (SQLException throwable) {
            plugin.getLogger().severe("Failed to save player information: " + throwable.getMessage());
        }finally {
            closeConnections(preparedStatement, connection, null);
        }
    }

    private CompletableFuture<Void> savePlayerInfoAsync(PlayerData player){
        // clearCosmeticsToSaveData() is already called by the listener before this method
        ensureOfflinePlayer(player);
        return checkInfoAsync(player.getUniqueId()).thenCompose(check -> CompletableFuture.runAsync(() -> {
            Connection connection = null;
            PreparedStatement preparedStatement = null;
            try{
                connection = hikariCP.getHikariDataSource().getConnection();
                String playerName = resolvePlayerName(player);
                if(!check){
                    String query = "INSERT INTO player_cosmetics (id, UUID, Player, Hat, Bag, WStick, Balloon, Spray, Available) VALUES(NULL, ?, ?, ?, ?, ?, ?, ?, ?);";
                    preparedStatement = connection.prepareStatement(query);
                    preparedStatement.setString(1, player.getUniqueId().toString());
                    preparedStatement.setString(2, playerName);
                    preparedStatement.setString(3, player.getHat() == null ? "" : player.getHat().getId());
                    preparedStatement.setString(4, player.getBag() == null ? "" : player.getBag().getId());
                    preparedStatement.setString(5, player.getWStick() == null ? "" : player.getWStick().getId());
                    preparedStatement.setString(6, player.getBalloon() == null ? "" : player.getBalloon().getId());
                    preparedStatement.setString(7, player.getSpray() == null ? "" : player.getSpray().getId());
                    preparedStatement.setString(8, player.saveCosmetics());
                    preparedStatement.executeUpdate();
                }else {
                    String query = "UPDATE player_cosmetics SET Player = ?, Hat = ?, Bag = ?, WStick = ?, Balloon = ?, Spray = ?, Available = ? WHERE UUID = ?";
                    preparedStatement = connection.prepareStatement(query);
                    preparedStatement.setString(1, playerName);
                    preparedStatement.setString(2, player.getHat() == null ? "" : player.getHat().getId());
                    preparedStatement.setString(3, player.getBag() == null ? "" : player.getBag().getId());
                    preparedStatement.setString(4, player.getWStick() == null ? "" : player.getWStick().getId());
                    preparedStatement.setString(5, player.getBalloon() == null ? "" : player.getBalloon().getId());
                    preparedStatement.setString(6, player.getSpray() == null ? "" : player.getSpray().getId());
                    preparedStatement.setString(7, player.saveCosmetics());
                    preparedStatement.setString(8, player.getUniqueId().toString());
                    preparedStatement.executeUpdate();
                }
            }catch (SQLException throwable) {
                plugin.getLogger().severe("Failed to save player information: " + throwable.getMessage());
            } finally {
                closeConnections(preparedStatement, connection, null);
            }
        }));
    }

    private void loadPlayerInfo(Player player){
        String queryBuilder = "SELECT * FROM player_cosmetics WHERE UUID = ?";
        CustomSpray.updateSpray(player);
        PlayerBalloon.updatePlayerBalloon(player);
        EntityBag.updateEntityBag(player);
        EntityBalloon.updateEntityBalloon(player);
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            connection = hikariCP.getHikariDataSource().getConnection();
            preparedStatement = connection.prepareStatement(queryBuilder);
            preparedStatement.setString(1, player.getUniqueId().toString());
            resultSet = preparedStatement.executeQuery();
            if(resultSet == null){
                return;
            }
            PlayerData playerData = PlayerData.getPlayer(player);
            if(resultSet.next()){
                String cosmetics = resultSet.getString("Available");
                String hat = resultSet.getString("Hat");
                String bag = resultSet.getString("Bag");
                String wStick = resultSet.getString("WStick");
                String balloon = resultSet.getString("Balloon");
                String spray = resultSet.getString("Spray");
                playerData.setOfflinePlayer(Bukkit.getOfflinePlayer(player.getUniqueId()));
                playerData.loadCosmetics(cosmetics);
                FoliaUtil.runTask(plugin, player, () -> {
                    playerData.setCosmetic(CosmeticType.HAT, playerData.getCosmeticById(hat));
                    playerData.setCosmetic(CosmeticType.BAG,playerData.getCosmeticById(bag));
                    playerData.setCosmetic(CosmeticType.WALKING_STICK,playerData.getCosmeticById(wStick));
                    playerData.setCosmetic(CosmeticType.BALLOON, playerData.getCosmeticById(balloon));
                    playerData.setCosmetic(CosmeticType.SPRAY, playerData.getCosmeticById(spray));
                    plugin.getServer().getPluginManager().callEvent(new PlayerDataLoadEvent(playerData, playerData.cosmeticsInUse()));
                });
            }
        }catch (SQLException throwable){
            plugin.getLogger().severe("Failed to load player information: " + throwable.getMessage());
        } finally {
            closeConnections(preparedStatement, connection, resultSet);
        }
    }

    private CompletableFuture<PlayerData> loadPlayerInfoAsync(Player player){
        return CompletableFuture.supplyAsync(() -> {
            String queryBuilder = "SELECT * FROM player_cosmetics WHERE UUID = ?";
            Connection connection = null;
            PreparedStatement preparedStatement = null;
            ResultSet resultSet = null;
            try{
                connection = hikariCP.getHikariDataSource().getConnection();
                preparedStatement = connection.prepareStatement(queryBuilder);
                preparedStatement.setString(1, player.getUniqueId().toString());
                resultSet = preparedStatement.executeQuery();
                PlayerData playerData = PlayerData.getPlayer(player);
                if(resultSet == null){
                    return playerData;
                }
                if(resultSet.next()){
                    String cosmetics = resultSet.getString("Available");
                    String hat = resultSet.getString("Hat");
                    String bag = resultSet.getString("Bag");
                    String wStick = resultSet.getString("WStick");
                    String balloon = resultSet.getString("Balloon");
                    String spray = resultSet.getString("Spray");

                    playerData.setOfflinePlayer(Bukkit.getOfflinePlayer(player.getUniqueId()));
                    playerData.loadCosmetics(cosmetics);
                    PlayerBalloon.updatePlayerBalloon(player);
                    CustomSpray.updateSpray(player);
                    FoliaUtil.runTask(plugin, player, () -> {
                        PlayerData current = PlayerData.getPlayerIfPresent(player);
                        if(current != playerData) return;
                        EntityBag.updateEntityBag(player);
                        EntityBalloon.updateEntityBalloon(player);
                        playerData.setCosmetic(CosmeticType.BALLOON, playerData.getCosmeticById(balloon));
                        playerData.setCosmetic(CosmeticType.SPRAY, playerData.getCosmeticById(spray));
                        playerData.setCosmetic(CosmeticType.BAG,playerData.getCosmeticById(bag));
                        playerData.setCosmetic(CosmeticType.HAT, playerData.getCosmeticById(hat));
                        playerData.setCosmetic(CosmeticType.WALKING_STICK,playerData.getCosmeticById(wStick));
                        plugin.getServer().getPluginManager().callEvent(new PlayerDataLoadEvent(playerData, playerData.cosmeticsInUse()));
                    });
                    //async plugin.getServer().getPluginManager().callEvent(new PlayerDataLoadEvent(playerData, playerData.cosmeticsInUse()));
                    return playerData;
                }
            }catch (SQLException throwable){
                plugin.getLogger().severe("Failed to load async player information: " + throwable.getMessage());
            } finally {
                closeConnections(preparedStatement, connection, resultSet);
            }
            return null;
        });
    }


    private boolean checkInfo(UUID uuid){
            Connection connection = null;
            PreparedStatement preparedStatement = null;
            ResultSet resultSet = null;
            String queryBuilder = "SELECT * FROM player_cosmetics WHERE UUID = ?";
            try {
                connection = hikariCP.getHikariDataSource().getConnection();
                preparedStatement = connection.prepareStatement(queryBuilder);
                preparedStatement.setString(1, uuid.toString());
                resultSet = preparedStatement.executeQuery();
                if(resultSet != null && resultSet.next()){
                    return true;
                }
            }catch (SQLException throwable){
                //plugin.getLogger().severe("Player information could not be verified.: " + throwable.getMessage());
            } finally {
                closeConnections(preparedStatement, connection, resultSet);
            }
            return false;
    }

    private CompletableFuture<Boolean> checkInfoAsync(UUID uuid){
        return CompletableFuture.supplyAsync(() -> {
            Connection connection = null;
            PreparedStatement preparedStatement = null;
            ResultSet resultSet = null;
            String queryBuilder = "SELECT * FROM player_cosmetics WHERE UUID = ?";
            try {
                connection = hikariCP.getHikariDataSource().getConnection();
                preparedStatement = connection.prepareStatement(queryBuilder);
                preparedStatement.setString(1, uuid.toString());
                resultSet = preparedStatement.executeQuery();
                if(resultSet != null && resultSet.next()){
                    return true;
                }
            }catch (SQLException throwable){
                //plugin.getLogger().severe("Player information could not be verified.: " + throwable.getMessage());
            } finally {
                closeConnections(preparedStatement, connection, resultSet);
            }
            return false;
        });
    }

    private void ensureOfflinePlayer(PlayerData player) {
        if (player.getOfflinePlayer() == null) {
            player.setOfflinePlayer(Bukkit.getOfflinePlayer(player.getUniqueId()));
        }
    }

    private String resolvePlayerName(PlayerData player) {
        OfflinePlayer op = player.getOfflinePlayer();
        if (op == null) {
            player.setOfflinePlayer(Bukkit.getOfflinePlayer(player.getUniqueId()));
            op = player.getOfflinePlayer();
        }
        String name = op != null ? op.getName() : null;
        return name != null ? name : player.getUniqueId().toString();
    }

    @Override
    public void close() {
        hikariCP.close();
    }

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.SQLITE;
    }

    public File getFileSQL() {
        return fileSQL;
    }
}
