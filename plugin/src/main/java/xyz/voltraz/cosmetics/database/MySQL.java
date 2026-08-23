package xyz.voltraz.cosmetics.database;

import xyz.voltraz.cosmetics.api.CosmeticType;
import xyz.voltraz.cosmetics.cache.PlayerData;
import xyz.voltraz.cosmetics.events.PlayerDataLoadEvent;
import xyz.voltraz.cosmetics.files.FileCreator;
import xyz.voltraz.cosmetics.nms.bag.EntityBag;
import xyz.voltraz.cosmetics.nms.balloon.EntityBalloon;
import xyz.voltraz.cosmetics.nms.balloon.PlayerBalloon;
import xyz.voltraz.cosmetics.nms.spray.CustomSpray;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MySQL extends SQL{

    private final HikariCP hikariCP;
    private final String table;

    public MySQL() {
        FileCreator config = plugin.getConfig();
        String hostname = config.getString("MySQL.host");
        int port = config.getInt("MySQL.port");
        String username = config.getString("MySQL.user");
        String password = config.getString("MySQL.password");
        String database = config.getString("MySQL.database");
        String options = config.getString("MySQL.options");
        table = config.getString("MySQL.table");
        hikariCP = new HikariCP(hostname, port, username, password, database, options);
        hikariCP.setProperties(this);
        createTable();
    }

    @Override
    public void createTable() {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = hikariCP.getHikariDataSource().getConnection();
            preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `" + table + "` (id INT AUTO_INCREMENT, UUID VARCHAR(255), Player VARCHAR(255), Hat VARCHAR(255), Bag VARCHAR(255), WStick VARCHAR(255), Balloon VARCHAR(255), Spray VARCHAR(255), Available VARCHAR(10000), PRIMARY KEY (id), UNIQUE KEY `uk_uuid` (UUID))");
            preparedStatement.executeUpdate();
            plugin.getLogger().info("MySQL table created successfully");
        } catch (SQLException throwable) {
            throwable.printStackTrace();
            plugin.getLogger().severe("Could not create table: " + throwable.getMessage());
        } finally {
            closeConnections(preparedStatement, connection, null);
        }
    }

    @Override
    public void loadPlayer(Player player){
        loadPlayerInfo(player);
    }

    private void loadPlayerInfo(Player player){
        String queryBuilder = "SELECT * FROM " + table + " WHERE UUID = ?";
        EntityBag.updateEntityBag(player);
        EntityBalloon.updateEntityBalloon(player);
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = hikariCP.getHikariDataSource().getConnection();
            statement = connection.prepareStatement(queryBuilder);
            statement.setString(1, player.getUniqueId().toString());
            resultSet = statement.executeQuery();
            PlayerData playerData = PlayerData.getPlayer(player);
            if(resultSet == null){
                return;
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

                xyz.voltraz.cosmetics.utils.FoliaUtil.runTask(plugin, player, () -> {
                    playerData.setCosmetic(CosmeticType.HAT, playerData.getCosmeticById(hat));
                    playerData.setCosmetic(CosmeticType.BAG,playerData.getCosmeticById(bag));
                    playerData.setCosmetic(CosmeticType.WALKING_STICK,playerData.getCosmeticById(wStick));
                    playerData.setCosmetic(CosmeticType.BALLOON, playerData.getCosmeticById(balloon));
                    playerData.setCosmetic(CosmeticType.SPRAY, playerData.getCosmeticById(spray));
                    CustomSpray.updateSpray(player);
                    PlayerBalloon.updatePlayerBalloon(player);
                    plugin.getServer().getPluginManager().callEvent(new PlayerDataLoadEvent(playerData, playerData.cosmeticsInUse()));
                });
            }
        }catch (SQLException throwable){
            plugin.getLogger().severe("Failed to load player information: " + throwable.getMessage());
        } finally {
            closeConnections(statement, connection, resultSet);
        }
    }

    @Override
    public void savePlayer(PlayerData playerData, boolean close){
        savePlayerInfo(playerData, close);
    }

    private static final String UPSERT_SINGLE = "INSERT INTO %s (id, UUID, Player, Hat, Bag, WStick, Balloon, Spray, Available) VALUES(NULL, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE Player=VALUES(Player), Hat=VALUES(Hat), Bag=VALUES(Bag), WStick=VALUES(WStick), Balloon=VALUES(Balloon), Spray=VALUES(Spray), Available=VALUES(Available)";

    private void savePlayerInfo(PlayerData player, boolean close){
        Connection connection = null;
        PreparedStatement statement = null;
        try{
            connection = hikariCP.getHikariDataSource().getConnection();
            String query = String.format(UPSERT_SINGLE, table);
            statement = connection.prepareStatement(query);
            statement.setString(1, player.getUniqueId().toString());
            statement.setString(2, resolvePlayerName(player));
            statement.setString(3, player.getHat() == null ? "" : player.getHat().getId());
            statement.setString(4, player.getBag() == null ? "" : player.getBag().getId());
            statement.setString(5, player.getWStick() == null ? "" : player.getWStick().getId());
            statement.setString(6, player.getBalloon() == null ? "" : player.getBalloon().getId());
            statement.setString(7, player.getSpray() == null ? "" : player.getSpray().getId());
            statement.setString(8, player.saveCosmetics());
            statement.executeUpdate();
        }catch (SQLException throwable) {
            plugin.getLogger().severe("Failed to save player information: " + throwable.getMessage());
        } finally {
            closeConnections(statement, connection, null);
        }
    }

    @Override
    public void savePlayers() {
        Connection connection = null;
        PreparedStatement statement = null;
        try{
            connection = hikariCP.getHikariDataSource().getConnection();
            String query = String.format(UPSERT_SINGLE, table);
            statement = connection.prepareStatement(query);
            for(PlayerData player : PlayerData.players.values()){
                try {
                    player.clearCosmeticsToSaveData();
                    statement.setString(1, player.getUniqueId().toString());
                    statement.setString(2, resolvePlayerName(player));
                    statement.setString(3, player.getHat() == null ? "" : player.getHat().getId());
                    statement.setString(4, player.getBag() == null ? "" : player.getBag().getId());
                    statement.setString(5, player.getWStick() == null ? "" : player.getWStick().getId());
                    statement.setString(6, player.getBalloon() == null ? "" : player.getBalloon().getId());
                    statement.setString(7, player.getSpray() == null ? "" : player.getSpray().getId());
                    statement.setString(8, player.saveCosmetics());
                    statement.addBatch();
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to save data for player " + player.getUniqueId() + ": " + e.getMessage());
                }
            }
            statement.executeBatch();
        }catch (SQLException throwable) {
            plugin.getLogger().severe("Failed to save player information: " + throwable.getMessage());
        } finally {
            closeConnections(statement, connection, null);
        }
    }

    @Override
    public CompletableFuture<PlayerData> loadPlayerAsync(Player player) {
        return loadPlayerInfoAsync(player);
    }

    private CompletableFuture<PlayerData> loadPlayerInfoAsync(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            String queryBuilder = "SELECT * FROM " + table + " WHERE UUID = ?";
            Connection connection = null;
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                connection = hikariCP.getHikariDataSource().getConnection();
                statement = connection.prepareStatement(queryBuilder);
                statement.setString(1, player.getUniqueId().toString());
                resultSet = statement.executeQuery();
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
                    xyz.voltraz.cosmetics.utils.FoliaUtil.runTask(plugin, player, () -> {
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
                closeConnections(statement, connection, resultSet);
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> savePlayerAsync(PlayerData playerData) {
        return savePlayerInfoAsync(playerData);
    }

    private CompletableFuture<Void> savePlayerInfoAsync(PlayerData player){
        // clearCosmeticsToSaveData() is already called by the listener before this method
        ensureOfflinePlayer(player);
        return CompletableFuture.runAsync(() -> {
            Connection connection = null;
            PreparedStatement statement = null;
            try{
                connection = hikariCP.getHikariDataSource().getConnection();
                String query = String.format(UPSERT_SINGLE, table);
                statement = connection.prepareStatement(query);
                statement.setString(1, player.getUniqueId().toString());
                statement.setString(2, resolvePlayerName(player));
                statement.setString(3, player.getHat() == null ? "" : player.getHat().getId());
                statement.setString(4, player.getBag() == null ? "" : player.getBag().getId());
                statement.setString(5, player.getWStick() == null ? "" : player.getWStick().getId());
                statement.setString(6, player.getBalloon() == null ? "" : player.getBalloon().getId());
                statement.setString(7, player.getSpray() == null ? "" : player.getSpray().getId());
                statement.setString(8, player.saveCosmetics());
                statement.executeUpdate();
            }catch (SQLException throwable) {
                plugin.getLogger().severe("Failed to save player information: " + throwable.getMessage());
            } finally {
                closeConnections(statement, connection, null);
            }
        });
    }

    private CompletableFuture<Boolean> checkInfoAsync(UUID uuid){
        return CompletableFuture.supplyAsync(() -> {
            String queryBuilder = "SELECT * FROM " + table + " WHERE UUID = ?";
            Connection connection = null;
            PreparedStatement preparedStatement = null;
            ResultSet resultSet = null;
            try {
                connection = hikariCP.getHikariDataSource().getConnection();
                preparedStatement = connection.prepareStatement(queryBuilder);
                preparedStatement.setString(1, uuid.toString());
                resultSet = preparedStatement.executeQuery();
                if(resultSet != null && resultSet.next()){
                    return true;
                }
            }catch (SQLException throwable){
                plugin.getLogger().severe("Player information could not be verified.: " + throwable.getMessage());
            } finally {
                closeConnections(preparedStatement, connection, resultSet);
            }
            return false;
        });
    }

    private boolean checkInfo(UUID uuid){
        String queryBuilder = "SELECT * FROM " + table + " WHERE UUID = ?";
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            connection = hikariCP.getHikariDataSource().getConnection();
            preparedStatement = connection.prepareStatement(queryBuilder);
            preparedStatement.setString(1, uuid.toString());
            resultSet = preparedStatement.executeQuery();
            if(resultSet != null && resultSet.next()){
                return true;
            }
        }catch (SQLException throwable){
            plugin.getLogger().severe("Player information could not be verified.: " + throwable.getMessage());
        } finally {
            closeConnections(preparedStatement, connection, resultSet);
        }
        return false;
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
        return DatabaseType.MYSQL;
    }
}
