import java.awt.*;
import java.awt.event.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.swing.*;

public class SignupPage extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField, confirmPasswordField;
    private JButton signupButton, loginButton, showPasswordButton;
    private boolean isPasswordVisible = false;

    public SignupPage() {
        setTitle("Create Account");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Background Panel with Gradient Effect
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gradient = new GradientPaint(0, 0, new Color(59, 130, 246, 100),
                        getWidth(), getHeight(), new Color(220, 38, 38, 100));
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        backgroundPanel.setLayout(new GridBagLayout());

        // Inner Panel for Form
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.setPreferredSize(new Dimension(400, 350));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // Title Label
        JLabel titleLabel = new JLabel("Sign Up");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(59, 130, 246));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.add(titleLabel);
        formPanel.add(Box.createVerticalStrut(15));

        // Username
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        usernameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.add(usernameLabel);
        formPanel.add(Box.createVerticalStrut(5));

        usernameField = new JTextField();
        styleTextField(usernameField);
        usernameField.setMaximumSize(new Dimension(300, 30));
        usernameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.add(usernameField);
        formPanel.add(Box.createVerticalStrut(10));

        // Password
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        passwordLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.add(passwordLabel);
        formPanel.add(Box.createVerticalStrut(5));

        // Password field with show button
        JPanel passwordPanel = new JPanel();
        passwordPanel.setLayout(new BoxLayout(passwordPanel, BoxLayout.X_AXIS));
        passwordPanel.setBackground(Color.WHITE);
        passwordPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordPanel.setMaximumSize(new Dimension(300, 30));

        passwordField = new JPasswordField();
        styleTextField(passwordField);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        showPasswordButton = new JButton("👁");
        showPasswordButton.setPreferredSize(new Dimension(30, 30));
        showPasswordButton.setMaximumSize(new Dimension(30, 30));
        showPasswordButton.setFocusPainted(false);
        showPasswordButton.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        
        passwordPanel.add(passwordField);
        passwordPanel.add(Box.createHorizontalStrut(5));
        passwordPanel.add(showPasswordButton);
        
        formPanel.add(passwordPanel);
        formPanel.add(Box.createVerticalStrut(10));

        // Confirm Password
        JLabel confirmLabel = new JLabel("Confirm Password:");
        confirmLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        confirmLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.add(confirmLabel);
        formPanel.add(Box.createVerticalStrut(5));

        confirmPasswordField = new JPasswordField();
        styleTextField(confirmPasswordField);
        confirmPasswordField.setMaximumSize(new Dimension(300, 30));
        confirmPasswordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.add(confirmPasswordField);
        formPanel.add(Box.createVerticalStrut(20));

        // Sign Up Button
        signupButton = createStyledButton("Sign Up", new Color(59, 130, 246), new Color(37, 99, 235));
        signupButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.add(signupButton);
        formPanel.add(Box.createVerticalStrut(10));

        // Login Link
        JPanel loginPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        loginPanel.setBackground(Color.WHITE);
        
        JLabel accountLabel = new JLabel("Already have an account? ");
        accountLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        
        loginButton = new JButton("Login here");
        loginButton.setFont(new Font("Arial", Font.PLAIN, 12));
        loginButton.setBorderPainted(false);
        loginButton.setContentAreaFilled(false);
        loginButton.setForeground(new Color(220, 38, 38));
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        loginPanel.add(accountLabel);
        loginPanel.add(loginButton);
        loginPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.add(loginPanel);

        // Add Panels to Frame
        backgroundPanel.add(formPanel);
        add(backgroundPanel);

        // Event Listeners
        signupButton.addActionListener(e -> registerUser());
        loginButton.addActionListener(e -> {
            dispose();
            new LoginPage().setVisible(true);
        });

        showPasswordButton.addActionListener(e -> togglePasswordVisibility());

        setVisible(true);
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        char echoChar = isPasswordVisible ? (char) 0 : '*';
        passwordField.setEchoChar(echoChar);
        confirmPasswordField.setEchoChar(echoChar);
        showPasswordButton.setText(isPasswordVisible ? "🙈" : "👁");
    }

    private void registerUser() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)")) {

            ps.setString(1, username);
            ps.setString(2, hashPassword(password)); // Securely hash the password
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Account created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            new LoginPage().setVisible(true);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    private void styleTextField(JTextField field) {
        field.setFont(new Font("Arial", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
    }

    private JButton createStyledButton(String text, Color normal, Color hover) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(normal);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(250, 40));
        button.setMaximumSize(new Dimension(250, 40));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(normal);
            }
        });

        return button;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(SignupPage::new);
    }
}