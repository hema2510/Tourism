import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.NumberFormatter;

public class DestinationManager extends JFrame {
    private JTable destTable, bookingTable;
    private DefaultTableModel destTableModel, bookingTableModel;
    private JTextField nameField, countryField, searchField;
    private JFormattedTextField priceField;
    private JTextField bookingNameField, bookingEmailField, bookingPhoneField;
    private JComboBox<DestinationItem> destinationComboBox;
    private JSpinner travelerCountSpinner;
    private JSpinner departureDateSpinner, returnDateSpinner; // Replace JDateChooser with JSpinner
    private JButton addButton, updateButton, deleteButton, clearButton, searchButton, bookButton;
    private JButton editBookingButton, deleteBookingButton; // Add buttons for editing and deleting bookings
    private JTabbedPane tabbedPane;
    private JPanel viewPanel, addPanel, bookPanel;
    private Connection conn;
    private int selectedId = -1;

    // Class to hold destination items for combo box
    private class DestinationItem {
        private int id;
        private String name;
        private double price;

        public DestinationItem(int id, String name, double price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }

        public int getId() {
            return id;
        }

        public double getPrice() {
            return price;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public DestinationManager(String username) {
        setTitle("Travel Destination Manager - " + username);
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Create tabbed pane
        tabbedPane = new JTabbedPane();
        
        // Create panels for each tab
        createViewPanel();
        createAddPanel();
        createBookPanel();
        
        // Add tabs to the tabbed pane
        tabbedPane.addTab("View Destinations", new ImageIcon(), viewPanel, "View all destinations");
        tabbedPane.addTab("Add/Edit Destination", new ImageIcon(), addPanel, "Add or edit destination details");
        tabbedPane.addTab("Book Tour", new ImageIcon(), bookPanel, "Book a tour to a destination");
        
        // Add tabbed pane to frame
        add(tabbedPane);
        
        // Load initial data
        loadDestinations();
        loadBookings();
        
        setVisible(true);
        
        // Add window listener to close database connection
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (conn != null && !conn.isClosed()) {
                        conn.close();
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
    
    private void createViewPanel() {
        viewPanel = new JPanel(new BorderLayout(10, 10));
        viewPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Table setup
        String[] columns = {"ID", "Name", "Country", "Price"};
        destTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };
        destTable = new JTable(destTableModel);
        destTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        destTable.getTableHeader().setReorderingAllowed(false);
        destTable.setRowHeight(25);
        destTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && destTable.getSelectedRow() != -1) {
                selectedId = (int) destTableModel.getValueAt(destTable.getSelectedRow(), 0);
                loadSelectedDestination();
                tabbedPane.setSelectedIndex(1);
            }
        });
        
        // Create row sorter for filtering
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(destTableModel);
        destTable.setRowSorter(sorter);
        
        JScrollPane scrollPane = new JScrollPane(destTable);
        
        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(20);
        searchButton = new JButton("Search");
        JButton refreshButton = new JButton("Refresh");
        
        searchButton.addActionListener(e -> {
            String text = searchField.getText().trim();
            if (text.isEmpty()) {
                sorter.setRowFilter(null);
            } else {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
            }
        });
        
        refreshButton.addActionListener(e -> {
            loadDestinations();
            searchField.setText("");
            sorter.setRowFilter(null);
        });
        
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(refreshButton);
        
        // Button panel for view tab
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        deleteButton = new JButton("Delete Selected");
        JButton addNewButton = new JButton("Add New Destination");
        JButton bookTourButton = new JButton("Book This Destination");
        
        deleteButton.addActionListener(e -> deleteDestination());
        addNewButton.addActionListener(e -> {
            clearFields();
            selectedId = -1;
            tabbedPane.setSelectedIndex(1);
        });
        bookTourButton.addActionListener(e -> {
            if (destTable.getSelectedRow() != -1) {
                selectedId = (int) destTableModel.getValueAt(destTable.getSelectedRow(), 0);
                String destName = (String) destTableModel.getValueAt(destTable.getSelectedRow(), 1);
                prepareBookingForm(selectedId, destName);
                tabbedPane.setSelectedIndex(2);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a destination to book.", 
                        "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        // Add tooltips to buttons
        deleteButton.setToolTipText("Delete the selected destination");
        addNewButton.setToolTipText("Add a new destination");
        bookTourButton.setToolTipText("Book a tour for the selected destination");
        
        // Add icons to buttons
        deleteButton.setIcon(new ImageIcon("icons/delete.png"));
        addNewButton.setIcon(new ImageIcon("icons/add.png"));
        bookTourButton.setIcon(new ImageIcon("icons/book.png"));
        
        buttonPanel.add(deleteButton);
        buttonPanel.add(addNewButton);
        buttonPanel.add(bookTourButton);
        
        // Add components to view panel
        viewPanel.add(searchPanel, BorderLayout.NORTH);
        viewPanel.add(scrollPane, BorderLayout.CENTER);
        viewPanel.add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void createAddPanel() {
        addPanel = new JPanel(new BorderLayout(10, 10));
        addPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Form panel with input fields
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Destination Details"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Name field
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Destination Name:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        nameField = new JTextField(20);
        formPanel.add(nameField, gbc);
        
        // Country field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("Country:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        countryField = new JTextField(20);
        formPanel.add(countryField, gbc);
        
        // Price field
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("Price:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        
        // Create formatted text field for price
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        NumberFormatter formatter = new NumberFormatter(format);
        formatter.setValueClass(Double.class);
        formatter.setAllowsInvalid(false);
        priceField = new JFormattedTextField(formatter);
        priceField.setValue(0.0);
        formPanel.add(priceField, gbc);
        
        // Button panel for add/edit tab
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        addButton = new JButton("Save Destination");
        updateButton = new JButton("Update Destination");
        clearButton = new JButton("Clear Fields");
        JButton backButton = new JButton("Back to List");
        
        addButton.addActionListener(e -> saveDestination());
        updateButton.addActionListener(e -> updateDestination());
        clearButton.addActionListener(e -> clearFields());
        backButton.addActionListener(e -> tabbedPane.setSelectedIndex(0));
        
        // Add tooltips to buttons
        addButton.setToolTipText("Save the new destination");
        updateButton.setToolTipText("Update the selected destination");
        clearButton.setToolTipText("Clear all fields");
        backButton.setToolTipText("Go back to the destination list");
        
        // Add icons to buttons
        addButton.setIcon(new ImageIcon("icons/save.png"));
        updateButton.setIcon(new ImageIcon("icons/update.png"));
        clearButton.setIcon(new ImageIcon("icons/clear.png"));
        backButton.setIcon(new ImageIcon("icons/back.png"));
        
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(backButton);
        
        // Add form and button panels to add panel
        addPanel.add(formPanel, BorderLayout.CENTER);
        addPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Add instructions at the top
        JTextArea instructions = new JTextArea();
        instructions.setText("Instructions:\n" +
                "1. Enter the destination details in the fields below\n" +
                "2. Click 'Save Destination' to add a new destination\n" +
                "3. To edit an existing destination, select it from the View tab first\n" +
                "4. Click 'Update Destination' to save changes to an existing destination");
        instructions.setEditable(false);
        instructions.setBackground(new Color(240, 240, 240));
        instructions.setBorder(BorderFactory.createEtchedBorder());
        instructions.setFont(new Font("SansSerif", Font.PLAIN, 12));
        
        addPanel.add(instructions, BorderLayout.NORTH);
    }
    
    private void createBookPanel() {
        bookPanel = new JPanel(new BorderLayout(10, 10));
        bookPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Split pane for booking form and booking history
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.5);
        
        // Booking form panel
        JPanel bookingFormPanel = new JPanel(new BorderLayout(10, 10));
        bookingFormPanel.setBorder(BorderFactory.createTitledBorder("Book a Tour"));
        
        // Form panel with input fields
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Customer name field
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Customer Name:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        bookingNameField = new JTextField(20);
        formPanel.add(bookingNameField, gbc);
        
        // Email field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("Email:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        bookingEmailField = new JTextField(20);
        formPanel.add(bookingEmailField, gbc);
        
        // Phone field
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("Phone:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        bookingPhoneField = new JTextField(20);
        formPanel.add(bookingPhoneField, gbc);
        
        // Destination selection
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("Destination:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        destinationComboBox = new JComboBox<>();
        formPanel.add(destinationComboBox, gbc);
        
        // Number of travelers
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("Number of Travelers:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.weightx = 1.0;
        SpinnerNumberModel travelerModel = new SpinnerNumberModel(1, 1, 20, 1);
        travelerCountSpinner = new JSpinner(travelerModel);
        formPanel.add(travelerCountSpinner, gbc);
        
        // Departure date
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("Departure Date:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.weightx = 1.0;
        departureDateSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor departureEditor = new JSpinner.DateEditor(departureDateSpinner, "yyyy-MM-dd");
        departureDateSpinner.setEditor(departureEditor);
        formPanel.add(departureDateSpinner, gbc);
        
        // Return date
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("Return Date:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.weightx = 1.0;
        returnDateSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor returnEditor = new JSpinner.DateEditor(returnDateSpinner, "yyyy-MM-dd");
        returnDateSpinner.setEditor(returnEditor);
        formPanel.add(returnDateSpinner, gbc);
        
        // Total price label
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("Total Price:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.weightx = 1.0;
        JLabel totalPriceLabel = new JLabel("$0.00");
        totalPriceLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        formPanel.add(totalPriceLabel, gbc);
        
        // Add listener to update total price when selections change
        ItemListener priceUpdater = e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateTotalPrice(totalPriceLabel);
            }
        };
        
        destinationComboBox.addItemListener(priceUpdater);
        travelerCountSpinner.addChangeListener(e -> updateTotalPrice(totalPriceLabel));
        
        // Button panel for booking tab
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        bookButton = new JButton("Book Tour");
        JButton clearBookingButton = new JButton("Clear Form");
        editBookingButton = new JButton("Edit Booking");
        deleteBookingButton = new JButton("Delete Booking");
        
        bookButton.addActionListener(e -> bookTour());
        clearBookingButton.addActionListener(e -> clearBookingForm());
        editBookingButton.addActionListener(e -> editBooking());
        deleteBookingButton.addActionListener(e -> deleteBooking());
        
        // Add tooltips to buttons
        bookButton.setToolTipText("Book the tour");
        clearBookingButton.setToolTipText("Clear the booking form");
        editBookingButton.setToolTipText("Edit the selected booking");
        deleteBookingButton.setToolTipText("Delete the selected booking");
        
        // Add icons to buttons
        bookButton.setIcon(new ImageIcon("icons/book.png"));
        clearBookingButton.setIcon(new ImageIcon("icons/clear.png"));
        editBookingButton.setIcon(new ImageIcon("icons/edit.png"));
        deleteBookingButton.setIcon(new ImageIcon("icons/delete.png"));
        
        buttonPanel.add(bookButton);
        buttonPanel.add(clearBookingButton);
        buttonPanel.add(editBookingButton);
        buttonPanel.add(deleteBookingButton);
        
        // Add form and button panels to booking form panel
        bookingFormPanel.add(formPanel, BorderLayout.CENTER);
        bookingFormPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Booking history panel
        JPanel bookingHistoryPanel = new JPanel(new BorderLayout(10, 10));
        bookingHistoryPanel.setBorder(BorderFactory.createTitledBorder("Booking History"));
        
        // Table setup for bookings
        String[] bookingColumns = {"ID", "Customer", "Destination", "Travelers", "Departure", "Return", "Total Price"};
        bookingTableModel = new DefaultTableModel(bookingColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };
        bookingTable = new JTable(bookingTableModel);
        bookingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookingTable.getTableHeader().setReorderingAllowed(false);
        bookingTable.setRowHeight(25);
        
        JScrollPane bookingScrollPane = new JScrollPane(bookingTable);
        bookingHistoryPanel.add(bookingScrollPane, BorderLayout.CENTER);
        
        // Button to refresh booking history
        JButton refreshBookingsButton = new JButton("Refresh Bookings");
        refreshBookingsButton.addActionListener(e -> loadBookings());
        bookingHistoryPanel.add(refreshBookingsButton, BorderLayout.SOUTH);
        
        // Add panels to split pane
        splitPane.setTopComponent(bookingFormPanel);
        splitPane.setBottomComponent(bookingHistoryPanel);
        
        // Add split pane to book panel
        bookPanel.add(splitPane, BorderLayout.CENTER);
    }
    
    private void updateTotalPrice(JLabel priceLabel) {
        try {
            DestinationItem selectedDest = (DestinationItem) destinationComboBox.getSelectedItem();
            if (selectedDest != null) {
                int travelers = (Integer) travelerCountSpinner.getValue();
                double basePrice = selectedDest.getPrice();
                double totalPrice = basePrice * travelers;
                
                // Format price with two decimal places
                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
                priceLabel.setText(currencyFormat.format(totalPrice));
            }
        } catch (Exception e) {
            priceLabel.setText("$0.00");
        }
    }
    
    private void prepareBookingForm(int destinationId, String destinationName) {
        // Clear form fields
        clearBookingForm();
        
        // Select the destination in the combo box
        for (int i = 0; i < destinationComboBox.getItemCount(); i++) {
            DestinationItem item = destinationComboBox.getItemAt(i);
            if (item.getId() == destinationId) {
                destinationComboBox.setSelectedIndex(i);
                break;
            }
        }
    }
    
    private void clearBookingForm() {
        bookingNameField.setText("");
        bookingEmailField.setText("");
        bookingPhoneField.setText("");
        travelerCountSpinner.setValue(1);
        departureDateSpinner.setValue(new Date());
        Date defaultReturn = new Date();
        defaultReturn.setTime(defaultReturn.getTime() + 7 * 24 * 60 * 60 * 1000);
        returnDateSpinner.setValue(defaultReturn);
    }
    
    private void bookTour() {
        // Validate input
        String customerName = bookingNameField.getText().trim();
        String email = bookingEmailField.getText().trim();
        String phone = bookingPhoneField.getText().trim();
        
        if (customerName.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all customer details.", 
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        DestinationItem selectedDest = (DestinationItem) destinationComboBox.getSelectedItem();
        if (selectedDest == null) {
            JOptionPane.showMessageDialog(this, "Please select a destination.", 
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Date departureDate = (Date) departureDateSpinner.getValue();
        Date returnDate = (Date) returnDateSpinner.getValue();
        
        if (departureDate == null || returnDate == null) {
            JOptionPane.showMessageDialog(this, "Please select both departure and return dates.", 
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (returnDate.before(departureDate)) {
            JOptionPane.showMessageDialog(this, "Return date cannot be before departure date.", 
                    "Invalid Dates", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int travelers = (Integer) travelerCountSpinner.getValue();
        double totalPrice = selectedDest.getPrice() * travelers;
        
        try {
            connectToDatabase();
            
            // Create TourBookings table if it doesn't exist
            String createTableSQL = "CREATE TABLE IF NOT EXISTS TourBookings (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "customer_name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(100) NOT NULL, " +
                    "phone VARCHAR(20) NOT NULL, " +
                    "destination_id INT NOT NULL, " +
                    "travelers INT NOT NULL, " +
                    "departure_date DATE NOT NULL, " +
                    "return_date DATE NOT NULL, " +
                    "total_price DECIMAL(10,2) NOT NULL, " +
                    "booking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (destination_id) REFERENCES Destinations(id)" +
                    ")";
            Statement createStmt = conn.createStatement();
            createStmt.execute(createTableSQL);
            createStmt.close();
            
            // Insert booking
            String insertSQL = "INSERT INTO TourBookings (customer_name, email, phone, destination_id, travelers, " +
                    "departure_date, return_date, total_price) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(insertSQL);
            pstmt.setString(1, customerName);
            pstmt.setString(2, email);
            pstmt.setString(3, phone);
            pstmt.setInt(4, selectedDest.getId());
            pstmt.setInt(5, travelers);
            pstmt.setDate(6, new java.sql.Date(departureDate.getTime()));
            pstmt.setDate(7, new java.sql.Date(returnDate.getTime()));
            pstmt.setDouble(8, totalPrice);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "Tour booked successfully!", 
                        "Booking Confirmed", JOptionPane.INFORMATION_MESSAGE);
                clearBookingForm();
                loadBookings();
            }
            
            pstmt.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error booking tour: " + e.getMessage(), 
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void connectToDatabase() {
        try {
            if (conn == null || conn.isClosed()) {
                conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/TravelDB", "root", "Hema2510");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Connection Error: " + e.getMessage(), 
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void loadDestinations() {
        try {
            connectToDatabase();
            
            // Clear existing data
            destTableModel.setRowCount(0);
            
            // Fetch all destinations
            String sql = "SELECT id, name, country, price FROM Destinations ORDER BY name";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            // Clear destination combo box
            destinationComboBox.removeAllItems();
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String country = rs.getString("country");
                double price = rs.getDouble("price");
                
                Vector<Object> row = new Vector<>();
                row.add(id);
                row.add(name);
                row.add(country);
                row.add(price);
                destTableModel.addRow(row);
                
                // Add to combo box for booking
                destinationComboBox.addItem(new DestinationItem(id, name + " (" + country + ")", price));
            }
            
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading destinations: " + e.getMessage(), 
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void loadBookings() {
        try {
            connectToDatabase();
            
            // Clear existing data
            bookingTableModel.setRowCount(0);
            
            // Check if TourBookings table exists
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tables = meta.getTables(null, null, "TourBookings", null);
            if (!tables.next()) {
                // Table doesn't exist yet, nothing to load
                return;
            }
            
            // Fetch all bookings with destination name
            String sql = "SELECT b.id, b.customer_name, d.name, b.travelers, " +
                    "b.departure_date, b.return_date, b.total_price " +
                    "FROM TourBookings b " +
                    "JOIN Destinations d ON b.destination_id = d.id " +
                    "ORDER BY b.booking_date DESC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
            
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("id"));
                row.add(rs.getString("customer_name"));
                row.add(rs.getString("name"));
                row.add(rs.getInt("travelers"));
                row.add(dateFormat.format(rs.getDate("departure_date")));
                row.add(dateFormat.format(rs.getDate("return_date")));
                row.add(currencyFormat.format(rs.getDouble("total_price")));
                bookingTableModel.addRow(row);
            }
            
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading bookings: " + e.getMessage(), 
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void loadSelectedDestination() {
        try {
            connectToDatabase();
            
            String sql = "SELECT name, country, price FROM Destinations WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, selectedId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                nameField.setText(rs.getString("name"));
                countryField.setText(rs.getString("country"));
                priceField.setValue(rs.getDouble("price"));
                
                addButton.setEnabled(false);
                updateButton.setEnabled(true);
            }
            
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading destination details: " + e.getMessage(), 
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void saveDestination() {
        String name = nameField.getText().trim();
        String country = countryField.getText().trim();
        
        if (name.isEmpty() || country.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name and Country fields are required!", 
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        double price = 0.0;
        try {
            price = ((Number) priceField.getValue()).doubleValue();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Price must be a valid number!", 
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            connectToDatabase();
            
            String sql = "INSERT INTO Destinations (name, country, price) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, name);
            stmt.setString(2, country);
            stmt.setDouble(3, price);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "Destination added successfully!", 
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    clearFields();
                    loadDestinations();
                }
                rs.close();
            }
            stmt.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error saving destination: " + e.getMessage(), 
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void updateDestination() {
        String name = nameField.getText().trim();
        String country = countryField.getText().trim();
        
        if (name.isEmpty() || country.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name and Country fields are required!", 
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        double price = 0.0;
        try {
            price = ((Number) priceField.getValue()).doubleValue();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Price must be a valid number!", 
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            connectToDatabase();
            
            String sql = "UPDATE Destinations SET name = ?, country = ?, price = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, name);
            stmt.setString(2, country);
            stmt.setDouble(3, price);
            stmt.setInt(4, selectedId);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "Destination updated successfully!", 
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                clearFields();
                loadDestinations();
                tabbedPane.setSelectedIndex(0);
            }
            
            stmt.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating destination: " + e.getMessage(), 
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void deleteDestination() {
        if (selectedId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a destination to delete.", 
                    "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to delete the selected destination?", 
                "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                connectToDatabase();
                
                String sql = "DELETE FROM Destinations WHERE id = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, selectedId);
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(this, "Destination deleted successfully!", 
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    clearFields();
                    loadDestinations();
                }
                
                stmt.close();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting destination: " + e.getMessage(), 
                        "Database Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    private void clearFields() {
        nameField.setText("");
        countryField.setText("");
        priceField.setValue(0.0);
        addButton.setEnabled(true);
        updateButton.setEnabled(false);
        selectedId = -1;
    }
    
    private void editBooking() {
        if (bookingTable.getSelectedRow() == -1) {
            JOptionPane.showMessageDialog(this, "Please select a booking to edit.", 
                    "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int bookingId = (int) bookingTableModel.getValueAt(bookingTable.getSelectedRow(), 0);
        
        try {
            connectToDatabase();
            
            String sql = "SELECT customer_name, email, phone, destination_id, travelers, departure_date, return_date " +
                         "FROM TourBookings WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, bookingId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                bookingNameField.setText(rs.getString("customer_name"));
                bookingEmailField.setText(rs.getString("email"));
                bookingPhoneField.setText(rs.getString("phone"));
                travelerCountSpinner.setValue(rs.getInt("travelers"));
                departureDateSpinner.setValue(rs.getDate("departure_date"));
                returnDateSpinner.setValue(rs.getDate("return_date"));
                
                // Select the destination in the combo box
                for (int i = 0; i < destinationComboBox.getItemCount(); i++) {
                    DestinationItem item = destinationComboBox.getItemAt(i);
                    if (item.getId() == rs.getInt("destination_id")) {
                        destinationComboBox.setSelectedIndex(i);
                        break;
                    }
                }
                
                bookButton.setEnabled(false);
                editBookingButton.setEnabled(true);
            }
            
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading booking details: " + e.getMessage(), 
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void deleteBooking() {
        if (bookingTable.getSelectedRow() == -1) {
            JOptionPane.showMessageDialog(this, "Please select a booking to delete.", 
                    "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int bookingId = (int) bookingTableModel.getValueAt(bookingTable.getSelectedRow(), 0);
        
        int confirm = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to delete the selected booking?", 
                "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                connectToDatabase();
                
                String sql = "DELETE FROM TourBookings WHERE id = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, bookingId);
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(this, "Booking deleted successfully!", 
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadBookings();
                }
                
                stmt.close();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting booking: " + e.getMessage(), 
                        "Database Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DestinationManager("Admin"));
    }
}