import javax.swing.*;
import java.awt.event.*;

public class HomePage extends JFrame {
    public HomePage(String username) {
        setTitle("Welcome, " + username);
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(null);

        JLabel welcomeLabel = new JLabel("Welcome, " + username + "!", SwingConstants.CENTER);
        welcomeLabel.setBounds(50, 50, 300, 30);
        add(welcomeLabel);

        JButton manageDestinationsButton = new JButton("Manage Destinations");
        manageDestinationsButton.setBounds(100, 100, 200, 30);
        add(manageDestinationsButton);

        manageDestinationsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new DestinationManager(username);  // Open DestinationManager
                dispose();  // Close HomePage
            }
        });

        setVisible(true);
    }
}