package mini_project;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;
import java.math.BigDecimal;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.math.BigDecimal;


public class RestaurantManagementSystem extends JFrame {
    private JTabbedPane tabbedPane;

    // Menu Items components
    private JTable menuTable;
    private DefaultTableModel menuModel;
    private JTextField tfName, tfDescription, tfPrice, tfCategory;
    private JCheckBox cbAvailable;

    // Orders components
    private JTable ordersTable;
    private DefaultTableModel ordersModel;
    private JTextField tfTableNumber, tfServerName;
    private JComboBox<String> cbOrderStatus;

    // Order Lines components
    private JTable linesTable;
    private DefaultTableModel linesModel;
    private JComboBox<ComboItem> comboOrders;
    private JComboBox<ComboItem> comboMenu;
    private JTextField tfQuantity;

    public RestaurantManagementSystem() {
        setTitle("Restaurant Management System");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        tabbedPane = new JTabbedPane();
        initMenuItemsTab();
        initOrdersTab();
        initOrderLinesTab();
        initReportTab();    
    
        add(tabbedPane);
        setVisible(true);
    }

    
    private JTextField tfOrderId; // To input Order ID for report
    private JButton btnGenerateReport; // Button to generate report

    private void initReportTab() {
        JPanel panel = new JPanel(new BorderLayout());

        // Form for entering order ID
        JPanel form = new JPanel(new GridLayout(2, 2, 5, 5));
        tfOrderId = new JTextField();
        form.add(new JLabel("Order ID:"));
        form.add(tfOrderId);
        
        btnGenerateReport = new JButton("Generate Report");
        form.add(btnGenerateReport);
        panel.add(form, BorderLayout.NORTH);

        // Table for displaying report
        JTable reportTable = new JTable(new DefaultTableModel(new String[]{"Order ID", "Menu Item", "Quantity", "Unit Price", "Total"}, 0));
        JScrollPane reportScrollPane = new JScrollPane(reportTable);
        panel.add(reportScrollPane, BorderLayout.CENTER);
        
        btnGenerateReport.addActionListener(e -> {
            try {
                int orderId = Integer.parseInt(tfOrderId.getText().trim());
                generateOrderReport(orderId, reportTable); // We'll update this method
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid Order ID.");
            }
        });


        tabbedPane.addTab("Order Report", panel);
    }

    
    private void generateOrderReport(int orderId, JTable reportTable) {
        String fileName = "Order_Report_" + orderId + ".txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            try (Connection con = DBConnection.getConnection()) {

                String orderQuery = "SELECT table_number, server_name, total_amount, order_date FROM orders WHERE order_id = ?";
                try (PreparedStatement pst = con.prepareStatement(orderQuery)) {
                    pst.setInt(1, orderId);
                    ResultSet rs = pst.executeQuery();

                    if (!rs.next()) {
                        JOptionPane.showMessageDialog(this, "Order ID not found.");
                        return;
                    }

                    int tableNo = rs.getInt("table_number");
                    String server = rs.getString("server_name");
                    BigDecimal total = rs.getBigDecimal("total_amount");
                    Timestamp orderDate = rs.getTimestamp("order_date");

                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                    String formattedDate = sdf.format(orderDate);

                    writer.write("Order Report\n");
                    writer.write("=============================\n");
                    writer.write("Order ID     : " + orderId + "\n");
                    writer.write("Date & Time  : " + formattedDate + "\n");
                    writer.write("Table No.    : " + tableNo + "\n");
                    writer.write("Server       : " + server + "\n");
                    writer.write("\nOrdered Items:\n");
                    writer.write("-----------------------------------------\n");
                    writer.write(String.format("%-20s %-10s %-10s\n", "Item Name", "Unit Price", "Quantity"));
                    writer.write("-----------------------------------------\n");

                    // Populate JTable
                    DefaultTableModel model = (DefaultTableModel) reportTable.getModel();
                    model.setRowCount(0); // Clear existing data

                    String itemsQuery = """
                        SELECT mi.name, ol.unit_price, ol.quantity
                        FROM order_lines ol
                        JOIN menu_items mi ON ol.menu_item_id = mi.menu_item_id
                        WHERE ol.order_id = ?
                    """;

                    try (PreparedStatement pst2 = con.prepareStatement(itemsQuery)) {
                        pst2.setInt(1, orderId);
                        ResultSet rs2 = pst2.executeQuery();

                        while (rs2.next()) {
                            String itemName = rs2.getString("name");
                            BigDecimal unitPrice = rs2.getBigDecimal("unit_price");
                            int quantity = rs2.getInt("quantity");
                            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

                            // Add to file
                            writer.write(String.format("%-20s %-10s %-10d\n", itemName, unitPrice.toPlainString(), quantity));

                            // Add to JTable
                            model.addRow(new Object[]{orderId, itemName, quantity, unitPrice, lineTotal});
                        }
                    }

                    writer.write("-----------------------------------------\n");
                    writer.write("Total Amount: ₹ " + total.toPlainString() + "\n");

                    JOptionPane.showMessageDialog(this, "Report generated: " + fileName);
                }

            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
            }

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error writing file: " + e.getMessage());
        }
    }


    private void initMenuItemsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        // Table
        menuModel = new DefaultTableModel(new String[]{"ID","Name","Description","Price","Category","Available"}, 0);
        menuTable = new JTable(menuModel);
        panel.add(new JScrollPane(menuTable), BorderLayout.CENTER);

        // Form
        JPanel form = new JPanel(new GridLayout(2,6,5,5));
        tfName = new JTextField();
        tfDescription = new JTextField();
        tfPrice = new JTextField();
        tfCategory = new JTextField();
        cbAvailable = new JCheckBox("Available", true);
        form.add(new JLabel("Name:")); form.add(tfName);
        form.add(new JLabel("Description:")); form.add(tfDescription);
        form.add(new JLabel("Price:")); form.add(tfPrice);
        form.add(new JLabel("Category:")); form.add(tfCategory);
        form.add(cbAvailable);
        panel.add(form, BorderLayout.NORTH);

        // Buttons
        JPanel buttons = new JPanel();
        JButton btnAdd = new JButton("Add");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        JButton btnRefresh = new JButton("Refresh");
        buttons.add(btnAdd); buttons.add(btnUpdate); buttons.add(btnDelete); buttons.add(btnRefresh);
        panel.add(buttons, BorderLayout.SOUTH);

        // Actions
        btnAdd.addActionListener(e -> addMenuItem());
        btnUpdate.addActionListener(e -> updateMenuItem());
        btnDelete.addActionListener(e -> deleteMenuItem());
        btnRefresh.addActionListener(e -> loadMenuItems());

        tabbedPane.addTab("Menu Items", panel);
        loadMenuItems();
    }

    private void initOrdersTab() {
        JPanel panel = new JPanel(new BorderLayout());
        // Table
        ordersModel = new DefaultTableModel(new String[]{"ID","Date","Table #","Server","Status","Total"}, 0);
        ordersTable = new JTable(ordersModel);
        panel.add(new JScrollPane(ordersTable), BorderLayout.CENTER);

        // Form
        JPanel form = new JPanel(new GridLayout(2,4,5,5));
        tfTableNumber = new JTextField();
        tfServerName = new JTextField();
        cbOrderStatus = new JComboBox<>(new String[]{"Pending","In progress","Completed","CANCELLED"});
        form.add(new JLabel("Table #:")); form.add(tfTableNumber);
        form.add(new JLabel("Server:")); form.add(tfServerName);
        form.add(new JLabel("Status:")); form.add(cbOrderStatus);
        panel.add(form, BorderLayout.NORTH);

        // Buttons
        JPanel buttons = new JPanel();
        JButton btnAdd = new JButton("Add");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        JButton btnRefresh = new JButton("Refresh");
        buttons.add(btnAdd); buttons.add(btnUpdate); buttons.add(btnDelete); buttons.add(btnRefresh);
        panel.add(buttons, BorderLayout.SOUTH);

        // Actions
        btnAdd.addActionListener(e -> addOrder());
        btnUpdate.addActionListener(e -> updateOrder());
        btnDelete.addActionListener(e -> deleteOrder());
        btnRefresh.addActionListener(e -> loadOrders());

        tabbedPane.addTab("Orders", panel);
        loadOrders();
    }

    private void initOrderLinesTab() {
        JPanel panel = new JPanel(new BorderLayout());
        // Table
        linesModel = new DefaultTableModel(new String[]{"Line ID","Order ID","Menu ID","Qty","Unit Price","Total"}, 0);
        linesTable = new JTable(linesModel);
        panel.add(new JScrollPane(linesTable), BorderLayout.CENTER);

        // Form
        JPanel form = new JPanel(new GridLayout(2,4,5,5));
        comboOrders = new JComboBox<>();
        comboMenu = new JComboBox<>();
        tfQuantity = new JTextField();
        form.add(new JLabel("Order:")); form.add(comboOrders);
        form.add(new JLabel("Menu Item:")); form.add(comboMenu);
        form.add(new JLabel("Quantity:")); form.add(tfQuantity);
        panel.add(form, BorderLayout.NORTH);

        // Buttons
        JPanel buttons = new JPanel();
        JButton btnAdd = new JButton("Add");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        JButton btnRefresh = new JButton("Refresh");
        buttons.add(btnAdd); buttons.add(btnUpdate); buttons.add(btnDelete); buttons.add(btnRefresh);
        panel.add(buttons, BorderLayout.SOUTH);

        // Actions
        btnAdd.addActionListener(e -> addOrderLine());
        btnUpdate.addActionListener(e -> updateOrderLine());
        btnDelete.addActionListener(e -> deleteOrderLine());
        btnRefresh.addActionListener(e -> loadOrderLines());

        tabbedPane.addTab("Order Lines", panel);
        loadOrdersCombo();
        loadMenuCombo();
        loadOrderLines();
    }

   

    // CRUD & Load methods for Menu Items
    private void loadMenuItems() {
        menuModel.setRowCount(0);
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM menu_items")) {
            while (rs.next()) {
                menuModel.addRow(new Object[]{
                    rs.getInt("menu_item_id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getBigDecimal("price"),
                    rs.getString("category"),
                    rs.getBoolean("available")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addMenuItem() {
        String sql = "INSERT INTO menu_items(name,description,price,category,available) VALUES (?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, tfName.getText());
            pst.setString(2, tfDescription.getText());
            pst.setBigDecimal(3, new java.math.BigDecimal(tfPrice.getText()));
            pst.setString(4, tfCategory.getText());
            pst.setBoolean(5, cbAvailable.isSelected());
            pst.executeUpdate();
            loadMenuItems();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateMenuItem() {
        int row = menuTable.getSelectedRow();
        if (row == -1) return;
        int id = (int) menuModel.getValueAt(row, 0);
        String sql = "UPDATE menu_items SET name=?,description=?,price=?,category=?,available=? WHERE menu_item_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, tfName.getText());
            pst.setString(2, tfDescription.getText());
            pst.setBigDecimal(3, new java.math.BigDecimal(tfPrice.getText()));
            pst.setString(4, tfCategory.getText());
            pst.setBoolean(5, cbAvailable.isSelected());
            pst.setInt(6, id);
            pst.executeUpdate();
            loadMenuItems();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteMenuItem() {
        int row = menuTable.getSelectedRow();
        if (row == -1) return;
        int id = (int) menuModel.getValueAt(row, 0);
        String sql = "DELETE FROM menu_items WHERE menu_item_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.executeUpdate();
            loadMenuItems();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // CRUD & Load methods for Orders
    private void loadOrders() {
        ordersModel.setRowCount(0);
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM orders")) {
            while (rs.next()) {
                ordersModel.addRow(new Object[]{
                    rs.getInt("order_id"),
                    rs.getDate("date"),
                    rs.getInt("table_number"),
                    rs.getString("server_name"),
                    rs.getString("status"),
                    rs.getBigDecimal("total_amount")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addOrder() {
        String sql = "INSERT INTO orders(table_number, server_name, status, total_amount, date) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, Integer.parseInt(tfTableNumber.getText()));
            pst.setString(2, tfServerName.getText());
            pst.setString(3, (String) cbOrderStatus.getSelectedItem());
            pst.setBigDecimal(4, BigDecimal.ZERO); // Initial total is zero, gets updated as items are added
            pst.setDate(5, new java.sql.Date(System.currentTimeMillis())); // adds current date

            pst.executeUpdate();
            loadOrders();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void updateOrder() {
        int row = ordersTable.getSelectedRow();
        if (row == -1) return;
        int id = (int) ordersModel.getValueAt(row, 0);
        String sql = "UPDATE orders SET table_number=?, server_name=?, status=?, total_amount=? WHERE order_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, Integer.parseInt(tfTableNumber.getText()));
            pst.setString(2, tfServerName.getText());
            pst.setString(3, (String) cbOrderStatus.getSelectedItem());
            pst.setBigDecimal(4, BigDecimal.ZERO);
            pst.setInt(5, id);
            pst.executeUpdate();
            loadOrders();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteOrder() {
        int row = ordersTable.getSelectedRow();
        if (row == -1) return;
        int id = (int) ordersModel.getValueAt(row, 0);
        String sql = "DELETE FROM orders WHERE order_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.executeUpdate();
            loadOrders();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // CRUD & Load methods for Order Lines
    private void loadOrderLines() {
        linesModel.setRowCount(0);
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM order_lines")) {
            while (rs.next()) {
                linesModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getInt("order_id"),
                    rs.getInt("menu_item_id"),
                    rs.getInt("quantity"),
                    rs.getBigDecimal("unit_price"),
                    rs.getBigDecimal("total")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // ComboItem class for combo boxes
    public class ComboItem {
        private int id;
        private String name;
        private BigDecimal unitPrice;
        public ComboItem(int id, String name,BigDecimal unitPrice) {
            this.id = id;
            this.name = name;
            this.unitPrice = unitPrice;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
        
        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private void addOrderLine() {
        String sql = "INSERT INTO order_lines(order_id, menu_item_id, quantity, unit_price,  total) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            
            // Get the selected order and menu item
            ComboItem selectedOrder = (ComboItem) comboOrders.getSelectedItem();
            ComboItem selectedMenuItem = (ComboItem) comboMenu.getSelectedItem();
            
            // Debugging: Print selected items
            if (selectedOrder == null || selectedMenuItem == null) {
                System.out.println("Invalid selection for order or menu item!");
                return;
            }

            System.out.println("Selected Order ID: " + selectedOrder.getId());
            System.out.println("Selected Menu Item ID: " + selectedMenuItem.getId());
            System.out.println("Selected Menu Item: " + selectedMenuItem.getName());
            
            // Get the quantity entered by the user
            String quantityText = tfQuantity.getText();
            if (quantityText == null || quantityText.isEmpty()) {
                System.out.println("Quantity is empty or null!");
                return; // Prevent further execution if quantity is invalid
            }
            
            BigDecimal quantity = new BigDecimal(quantityText);
            
            // Fetch the unit price from the selected menu item
            BigDecimal unitPrice = selectedMenuItem.getUnitPrice();
            
            // Debugging: Check if unitPrice is null
            if (unitPrice == null) {
                System.out.println("Unit price is null for the selected menu item!");
                return;
            }
            
            System.out.println("Unit Price for " + selectedMenuItem.getName() + ": " + unitPrice);
            
            // Calculate the line total (quantity * unit price)
            BigDecimal lineTotal = quantity.multiply(unitPrice);
            System.out.println(" Total: " + lineTotal);
            
            // Set the parameters for the PreparedStatement
            pst.setInt(1, selectedOrder.getId());
            pst.setInt(2, selectedMenuItem.getId());
            pst.setBigDecimal(3, quantity);
            pst.setBigDecimal(4, unitPrice);
            pst.setBigDecimal(5, lineTotal);
            
            
            // Execute the update
            pst.executeUpdate();
            
            // Refresh the order lines display
            loadOrderLines();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }





    private void updateOrderLine() {
        int row = linesTable.getSelectedRow();
        if (row == -1) return;
        int id = (int) linesModel.getValueAt(row, 0);

        // Get the selected menu item and its actual unit price
        ComboItem selectedMenuItem = (ComboItem) comboMenu.getSelectedItem();
        BigDecimal unitPrice = selectedMenuItem.getUnitPrice(); // this must not be null
        int quantity = Integer.parseInt(tfQuantity.getText());
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(quantity));

        String sql = "UPDATE order_lines SET order_id=?,menu_item_id=?,quantity=?,unit_price=?,total=? WHERE id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, ((ComboItem) comboOrders.getSelectedItem()).getId());
            pst.setInt(2, selectedMenuItem.getId());
            pst.setInt(3, quantity);
            pst.setBigDecimal(4, unitPrice);
            pst.setBigDecimal(5, total);
            pst.setInt(6, id);
            pst.executeUpdate();
            loadOrderLines();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void deleteOrderLine() {
        int row = linesTable.getSelectedRow();
        if (row == -1) return;
        int id = (int) linesModel.getValueAt(row, 0);
        String sql = "DELETE FROM order_lines WHERE id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.executeUpdate();
            loadOrderLines();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadOrdersCombo() {
        comboOrders.removeAllItems();
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM orders")) {
            while (rs.next()) {
                comboOrders.addItem(new ComboItem(rs.getInt("order_id"), "Order #" + rs.getInt("order_id") ,null));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadMenuCombo() {
        comboMenu.removeAllItems();
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM menu_items")) {
            while (rs.next()) {
                comboMenu.addItem(new ComboItem(rs.getInt("menu_item_id"), rs.getString("name"),rs.getBigDecimal("price")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new RestaurantManagementSystem();
    }
}
