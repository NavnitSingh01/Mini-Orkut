package Navnit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MiniOrkutWithDB extends JFrame {
    private String currentUser;
    private JTextArea profileTextArea;
    private JComboBox<String> friendComboBox;
    private Connection dbConnection;

    public MiniOrkutWithDB() {
        setTitle("Mini Orkut with DB");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        dbConnection = connectToDatabase();

        currentUser = "";

        profileTextArea = new JTextArea();
        profileTextArea.setEditable(false);
        add(new JScrollPane(profileTextArea), BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());

        JButton loginButton = new JButton("Login");
        JButton createProfileButton = new JButton("Create Profile");
        JButton viewFriendsButton = new JButton("View Friends");
        friendComboBox = new JComboBox<>();

        controlPanel.add(loginButton);
        controlPanel.add(createProfileButton);
        controlPanel.add(viewFriendsButton);
        controlPanel.add(friendComboBox);

        add(controlPanel, BorderLayout.SOUTH);

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = JOptionPane.showInputDialog("Enter your username:");
                if (username != null && !username.isEmpty()) {
                    if (userExists(username)) {
                        currentUser = username;
                        updateProfileText();
                        updateFriendComboBox();
                    } else {
                        JOptionPane.showMessageDialog(MiniOrkutWithDB.this, "User not found.");
                    }
                }
            }
        });

        createProfileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = JOptionPane.showInputDialog("Enter a username for your new profile:");
                if (username != null && !username.isEmpty()) {
                    createUser(username);
                    currentUser = username;
                    updateProfileText();
                    updateFriendComboBox();
                }
            }
        });

        viewFriendsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedFriend = (String) friendComboBox.getSelectedItem();
                if (selectedFriend != null && !selectedFriend.isEmpty()) {
                    viewFriendDetails(selectedFriend);
                }
            }
        });

        friendComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedFriend = (String) friendComboBox.getSelectedItem();
                if (selectedFriend != null && !selectedFriend.isEmpty()) {
                    addFriend(currentUser, selectedFriend);
                    updateProfileText();
                }
            }
        });
    }

    private Connection connectToDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String dbURL = "jdbc:mysql://localhost:3306/mini_orkut";
            String dbUser = "root";
            String dbPassword = "";
            return DriverManager.getConnection(dbURL, dbUser, dbPassword);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to connect to the database.");
            System.exit(1);
        }
        return null;
    }

    private boolean userExists(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement preparedStatement = dbConnection.prepareStatement(sql)) {
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void createUser(String username) {
        String sql = "INSERT INTO users (username, friends) VALUES (?, ?)";
        try (PreparedStatement preparedStatement = dbConnection.prepareStatement(sql)) {
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, "");
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to create user.");
        }
    }

    private void addFriend(String user, String friend) {
        String updateUserSQL = "UPDATE users SET friends = CONCAT(friends, ?) WHERE username = ?";
        String updateFriendSQL = "UPDATE users SET friends = CONCAT(friends, ?) WHERE username = ?";
        try (PreparedStatement updateUserStatement = dbConnection.prepareStatement(updateUserSQL);
             PreparedStatement updateFriendStatement = dbConnection.prepareStatement(updateFriendSQL)) {

            updateUserStatement.setString(1, friend + ",");
            updateUserStatement.setString(2, user);
            updateUserStatement.executeUpdate();

            updateFriendStatement.setString(1, user + ",");
            updateFriendStatement.setString(2, friend);
            updateFriendStatement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to add friend.");
        }
    }

    private void updateProfileText() {
        if (currentUser.isEmpty()) {
            profileTextArea.setText("No user logged in.");
        } else {
            profileTextArea.setText(getUserProfile(currentUser));
        }
    }

    private String getUserProfile(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement preparedStatement = dbConnection.prepareStatement(sql)) {
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String friends = resultSet.getString("friends");
                List<String> friendList = parseFriends(friends);
                return "User: " + resultSet.getString("username") +
                        "\nFull Name: " + resultSet.getString("full_name") +
                        "\nStatus: " + resultSet.getString("status") +
                        "\nFriends:\n" + getFriendsDetails(friendList);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "User not found.";
    }

    private List<String> parseFriends(String friends) {
        List<String> friendList = new ArrayList<>();
        if (friends != null) {
            String[] friendArray = friends.split(",");
            for (String friend : friendArray) {
                if (!friend.isEmpty()) {
                    friendList.add(friend);
                }
            }
        }
        return friendList;
    }

    private String getFriendsDetails(List<String> friendList) {
        StringBuilder friendDetails = new StringBuilder();
        for (String friend : friendList) {
            String sql = "SELECT * FROM users WHERE username = ?";
            try (PreparedStatement preparedStatement = dbConnection.prepareStatement(sql)) {
                preparedStatement.setString(1, friend);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    friendDetails.append("- ").append(resultSet.getString("username")).append("\n")
                            .append("  Full Name: ").append(resultSet.getString("full_name")).append("\n")
                            .append("  Status: ").append(resultSet.getString("status")).append("\n");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return friendDetails.toString();
    }

    private void updateFriendComboBox() {
        friendComboBox.removeAllItems();
        if (!currentUser.isEmpty()) {
            List<String> nonFriends = getNonFriends(currentUser);
            for (String friend : nonFriends) {
                friendComboBox.addItem(friend);
            }
        }
    }

    private List<String> getNonFriends(String user) {
        List<String> nonFriends = new ArrayList<>();
        String sql = "SELECT username FROM users WHERE username != ? AND friends NOT LIKE ?";
        try (PreparedStatement preparedStatement = dbConnection.prepareStatement(sql)) {
            preparedStatement.setString(1, user);
            preparedStatement.setString(2, "%" + user + "%");
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                nonFriends.add(resultSet.getString("username"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nonFriends;
    }

    private void viewFriendDetails(String friend) {
        String friendDetails = getUserProfile(friend);
        if (!friendDetails.equals("User not found.")) {
            JOptionPane.showMessageDialog(this, friendDetails, "Friend Details", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Friend not found.", "Friend Details", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                MiniOrkutWithDB app = new MiniOrkutWithDB();
                app.setVisible(true);
            }
        });
    }
}

