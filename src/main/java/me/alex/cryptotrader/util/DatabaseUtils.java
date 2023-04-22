package me.alex.cryptotrader.util;

import javafx.collections.FXCollections;
import me.alex.cryptotrader.CryptoApplication;
import me.alex.cryptotrader.models.Instruction;
import me.alex.cryptotrader.models.Strategy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DatabaseUtils {

    public static final String DB_URL = "jdbc:sqlite:database.db";

    public static List<Strategy> loadStrategies(String username) {
        List<Strategy> strategies = new ArrayList<>();

        try (
                Connection connection = DriverManager.getConnection(DB_URL);
                PreparedStatement query = connection.prepareStatement("SELECT id, name, tokenPair FROM strategies WHERE user_id = ?")
        ) {
            query.setString(1, username);
            ResultSet result = query.executeQuery();

            // If we find an account that has persistent login enabled, then log that user in immediately.
            while (result.next()) {
                Strategy strategy = new Strategy(result.getInt("id"), result.getString("name"), result.getString("tokenPair"), FXCollections.observableArrayList());
                strategy.getInstructions().addAll(loadInstructions(strategy, username));
                strategy.updateStrategy();
                strategies.add(strategy);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return strategies;
    }

    private static List<Instruction> loadInstructions(Strategy strategy, String username) {
        List<Instruction> instructions = new ArrayList<>();

        try (
                Connection connection = DriverManager.getConnection(DB_URL);
                PreparedStatement query = connection.prepareStatement("SELECT id, priority, type, data FROM instructions WHERE user_id = ? AND strategy_id = ?")
        ) {
            query.setString(1, username);
            query.setInt(2, strategy.getId());
            ResultSet result = query.executeQuery();

            // If we find an account that has persistent login enabled, then log that user in immediately.
            while (result.next()) {
                Instruction instruction = new Instruction(result.getInt("id"), result.getInt("priority"), Instruction.InstructionType.valueOf(result.getString("type")), result.getString("data"), strategy);
                instructions.add(instruction);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        instructions.sort(Comparator.comparingInt(Instruction::getPriority));

        return instructions;
    }

    public static Strategy createStrategy(String username, String name, String tokenPair) {
        try (
                Connection connection = DriverManager.getConnection(DB_URL);
                PreparedStatement query = connection.prepareStatement("INSERT INTO strategies " +
                        "(user_id, name, tokenPair) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)
        ) {
            query.setString(1, username);
            query.setString(2, name);
            query.setString(3, tokenPair);

            query.executeUpdate();

            ResultSet resultSet = query.getGeneratedKeys();
            if (resultSet.next()) {
                int id = resultSet.getInt(1);
                return new Strategy(id, name, tokenPair, FXCollections.observableArrayList());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Instruction createInstruction(String username, int priority, Instruction.InstructionType type, String data, Strategy strategy) {
        try (
                Connection connection = DriverManager.getConnection(DB_URL);
                PreparedStatement query = connection.prepareStatement("INSERT INTO instructions " +
                        "(user_id, strategy_id, priority, type, data) VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)
        ) {
            query.setString(1, username);
            query.setInt(2, strategy.getId());
            query.setInt(3, priority);
            query.setString(4, type.name());
            query.setString(5, data);

            query.executeUpdate();

            ResultSet resultSet = query.getGeneratedKeys();
            if (resultSet.next()) {
                int id = resultSet.getInt(1);
                return new Instruction(id, priority, type, data, strategy);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void saveStrategy(Strategy strategy) {
        // We don't need to actually update any data for the strategy itself, just update the individual instructions.
        strategy.getInstructions().forEach(DatabaseUtils::saveInstruction);
    }

    public static void saveInstruction(Instruction instruction) {
        try (
                Connection connection = DriverManager.getConnection(DB_URL);
                PreparedStatement query = connection.prepareStatement("UPDATE instructions SET priority = ?, data = ? WHERE id = ?")
        ) {
            query.setInt(1, instruction.getPriority());
            query.setString(2, instruction.getRawData());
            query.setInt(3, instruction.getId());
            query.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteStrategy(Strategy strategy) {
        // Delete all the strategies instructions.
        strategy.getInstructions().forEach(DatabaseUtils::deleteInstruction);

        // Delete the strategy itself.
        try (
                Connection connection = DriverManager.getConnection(DB_URL);
                PreparedStatement query = connection.prepareStatement("DELETE FROM strategies WHERE id = ?")
        ) {
            query.setInt(1, strategy.getId());
            query.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteInstruction(Instruction instruction) {
        // Delete the instruction.
        try (
                Connection connection = DriverManager.getConnection(DB_URL);
                PreparedStatement query = connection.prepareStatement("DELETE FROM instructions WHERE id = ?")
        ) {
            query.setInt(1, instruction.getId());
            query.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean validateLogin(String username, String password, boolean stayLoggedIn) {
        try (
                Connection connection = DriverManager.getConnection(DB_URL);
                PreparedStatement query = connection.prepareStatement("SELECT password FROM users WHERE username = ?")
        ) {
            query.setString(1, username);
            ResultSet result = query.executeQuery();

            // If we find an account with the provided username, check that the provided password matches the hashed
            // password using the same algorithm we used to hash it originally.
            if (result.next()) {
                BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
                String fetchedPassword = result.getString("password");
                boolean isValid = encoder.matches(password, fetchedPassword);

                if (isValid) {
                    CryptoApplication.get().loadUserProfile(username, password, stayLoggedIn);
                }

                return isValid;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean registerUser(String username, String password, String apiKey, String secretKey) {
        try (
                Connection connection = DriverManager.getConnection(DB_URL);
                PreparedStatement query = connection.prepareStatement("INSERT INTO users " +
                        "(username, password, apiKey, secretKey, preferredToken, stayLoggedIn) " +
                        "VALUES (?, ?, ?, ?, ?, ?)")
        ) {

            // VERY IMPORTANT: For security, we will hash the password/api keys so that nobody else who accesses this
            // machine without permission can get into the account.

            query.setString(1, username);
            query.setString(2, new BCryptPasswordEncoder().encode(password));
            query.setString(3, Utilities.encryptStringUsingPassword(apiKey, password));
            query.setString(4, Utilities.encryptStringUsingPassword(secretKey, password));
            query.setString(5, "BTCUSDT");
            query.setBoolean(6, false);

            query.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static String checkForPersistentUser() {
        try (
                Connection connection = DriverManager.getConnection(DB_URL);
                PreparedStatement query = connection.prepareStatement("SELECT username FROM users WHERE stayLoggedIn = ?")
        ) {
            query.setBoolean(1, true);
            ResultSet result = query.executeQuery();

            // If we find an account that has persistent login enabled, then log that user in immediately.
            if (result.next()) {
                return result.getString("username");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void savePersistentUser(String username) {
        try (
                Connection connection = DriverManager.getConnection(DB_URL);
                PreparedStatement query = connection.prepareStatement("UPDATE users SET stayLoggedIn = ? WHERE username = ?")
        ) {
            query.setBoolean(1, false);
            query.setString(2, username);
            query.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void initializeDatabase() {
        // Setup users database.
        try (
                Connection connection = DriverManager.getConnection(DB_URL);
                PreparedStatement users = connection.prepareStatement("CREATE TABLE IF NOT EXISTS users " +
                        "(username TEXT PRIMARY KEY, " +
                        "password TEXT NOT NULL, " +
                        "apiKey TEXT NOT NULL, " +
                        "secretKey TEXT NOT NULL, " +
                        "preferredToken TEXT NOT NULL, " +
                        "stayLoggedIn BOOLEAN NOT NULL)"
                )
        ) {
            users.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Setup strategies database.
        try (
                Connection connection = DriverManager.getConnection(DB_URL);
                PreparedStatement users = connection.prepareStatement("CREATE TABLE IF NOT EXISTS strategies " +
                        "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_id TEXT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "tokenPair TEXT NOT NULL, " +
                        "FOREIGN KEY (user_id) REFERENCES users (username) ON UPDATE CASCADE ON DELETE SET NULL" +
                        ");")
        ) {
            users.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Setup instructions database.
        try (
                Connection connection = DriverManager.getConnection(DB_URL);
                PreparedStatement users = connection.prepareStatement("CREATE TABLE IF NOT EXISTS instructions " +
                        "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_id TEXT NOT NULL, " +
                        "strategy_id INTEGER NOT NULL, " +
                        "priority INTEGER NOT NULL," +
                        "type TEXT NOT NULL, " +
                        "data TEXT NOT NULL, " +
                        "FOREIGN KEY (user_id) REFERENCES users (username) ON UPDATE CASCADE ON DELETE SET NULL, " +
                        "FOREIGN KEY (strategy_id) REFERENCES strategies (id) ON UPDATE CASCADE ON DELETE SET NULL" +
                        ");")
        ) {
            users.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}
