import java.sql.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;

/**
 * Luxe Salon Appointment Scheduler
 * Enterprise Java Implementation with PostgreSQL
 * Features connection pooling, prepared statements, and rich CLI
 * 
 * @author Luxe Salon Development Team
 * @version 2.0.0
 */
public class SalonScheduler {
    
    // Database configuration
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/salon";
    private static final String DB_USER = "freecodecamp";
    private static final String DB_PASSWORD = "";
    
    // ANSI colors for terminal output
    private static final String RESET = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String GOLD = "\033[38;5;220m";
    private static final String WHITE = "\033[97m";
    private static final String GRAY = "\033[90m";
    private static final String GREEN = "\033[92m";
    private static final String RED = "\033[91m";
    
    private Connection conn;
    private Scanner scanner;
    private Customer currentCustomer;
    private Service selectedService;
    
    /**
     * Data model for Customer entity
     */
    public static class Customer {
        int customerId;
        String name;
        String phone;
        
        Customer(int id, String name, String phone) {
            this.customerId = id;
            this.name = name;
            this.phone = phone;
        }
    }
    
    /**
     * Data model for Service entity
     */
    public static class Service {
        int serviceId;
        String name;
        
        Service(int id, String name) {
            this.serviceId = id;
            this.name = name;
        }
    }
    
    /**
     * Data model for Appointment entity
     */
    public static class Appointment {
        int appointmentId;
        int customerId;
        int serviceId;
        String time;
        String serviceName;
        
        Appointment(int id, int custId, int svcId, String time, String svcName) {
            this.appointmentId = id;
            this.customerId = custId;
            this.serviceId = svcId;
            this.time = time;
            this.serviceName = svcName;
        }
    }
    
    public SalonScheduler() {
        this.scanner = new Scanner(System.in);
    }
    
    /**
     * Establish database connection
     */
    public boolean connect() {
        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            return true;
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL driver not found");
            return false;
        } catch (SQLException e) {
            System.err.println("Connection failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Close database resources
     */
    public void disconnect() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
            scanner.close();
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
    
    /**
     * Initialize database schema and seed data
     */
    public void initializeDatabase() {
        String[] schema = {
            "DROP TABLE IF EXISTS appointments CASCADE",
            "DROP TABLE IF EXISTS customers CASCADE",
            "DROP TABLE IF EXISTS services CASCADE",
            
            "CREATE TABLE customers (" +
                "customer_id SERIAL PRIMARY KEY," +
                "name VARCHAR(255) NOT NULL," +
                "phone VARCHAR(255) UNIQUE NOT NULL" +
            ")",
            
            "CREATE TABLE services (" +
                "service_id SERIAL PRIMARY KEY," +
                "name VARCHAR(255) NOT NULL" +
            ")",
            
            "CREATE TABLE appointments (" +
                "appointment_id SERIAL PRIMARY KEY," +
                "customer_id INTEGER REFERENCES customers(customer_id)," +
                "service_id INTEGER REFERENCES services(service_id)," +
                "time VARCHAR(255) NOT NULL" +
            ")",
            
            "INSERT INTO services (service_id, name) VALUES (1, 'cut')",
            "INSERT INTO services (service_id, name) VALUES (2, 'color')",
            "INSERT INTO services (service_id, name) VALUES (3, 'perm')",
            "INSERT INTO services (service_id, name) VALUES (4, 'style')",
            "INSERT INTO services (service_id, name) VALUES (5, 'trim')"
        };
        
        try (Statement stmt = conn.createStatement()) {
            for (String sql : schema) {
                stmt.execute(sql);
            }
            System.out.println(GREEN + "Database initialized successfully" + RESET);
        } catch (SQLException e) {
            System.err.println("Schema creation failed: " + e.getMessage());
        }
    }
    
    /**
     * Retrieve all services from database
     */
    public List<Service> getServices() throws SQLException {
        List<Service> services = new ArrayList<>();
        String sql = "SELECT service_id, name FROM services ORDER BY service_id";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                services.add(new Service(
                    rs.getInt("service_id"),
                    rs.getString("name")
                ));
            }
        }
        return services;
    }
    
    /**
     * Get specific service by ID
     */
    public Service getServiceById(int id) throws SQLException {
        String sql = "SELECT * FROM services WHERE service_id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Service(rs.getInt("service_id"), rs.getString("name"));
                }
            }
        }
        return null;
    }
    
    /**
     * Find customer by phone number
     */
    public Customer findCustomerByPhone(String phone) throws SQLException {
        String sql = "SELECT * FROM customers WHERE phone = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, phone);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Customer(
                        rs.getInt("customer_id"),
                        rs.getString("name"),
                        rs.getString("phone")
                    );
                }
            }
        }
        return null;
    }
    
    /**
     * Insert new customer record
     */
    public int createCustomer(String name, String phone) throws SQLException {
        String sql = "INSERT INTO customers (name, phone) VALUES (?, ?) RETURNING customer_id";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, phone);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    conn.commit();
                    return rs.getInt("customer_id");
                }
            }
        }
        return -1;
    }
    
    /**
     * Create new appointment
     */
    public int createAppointment(int customerId, int serviceId, String time) throws SQLException {
        String sql = "INSERT INTO appointments (customer_id, service_id, time) VALUES (?, ?, ?) RETURNING appointment_id";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            pstmt.setInt(2, serviceId);
            pstmt.setString(3, time);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    conn.commit();
                    return rs.getInt("appointment_id");
                }
            }
        }
        return -1;
    }
    
    /**
     * Get all appointments for a customer
     */
    public List<Appointment> getCustomerAppointments(int customerId) throws SQLException {
        List<Appointment> appointments = new ArrayList<>();
        String sql = "SELECT a.*, s.name as service_name FROM appointments a " +
                     "JOIN services s ON a.service_id = s.service_id " +
                     "WHERE a.customer_id = ? ORDER BY a.appointment_id DESC";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    appointments.add(new Appointment(
                        rs.getInt("appointment_id"),
                        rs.getInt("customer_id"),
                        rs.getInt("service_id"),
                        rs.getString("time"),
                        rs.getString("service_name")
                    ));
                }
            }
        }
        return appointments;
    }
    
    // UI Helper methods
    
    private void printHeader(String text) {
        int width = 50;
        String line = "=".repeat(width);
        System.out.println("\n" + GOLD + line + RESET);
        System.out.println(BOLD + WHITE + center(text, width) + RESET);
        System.out.println(GOLD + line + RESET + "\n");
    }
    
    private void printSuccess(String message) {
        System.out.println(GREEN + "✓ " + message + RESET);
    }
    
    private void printError(String message) {
        System.out.println(RED + "✗ " + message + RESET);
    }
    
    private void printInfo(String message) {
        System.out.println(GRAY + "ℹ " + message + RESET);
    }
    
    private String center(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text;
    }
    
    /**
     * Format phone number to (XXX) XXX-XXXX
     */
    private String formatPhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 10) {
            return String.format("(%s) %s-%s", 
                digits.substring(0, 3), 
                digits.substring(3, 6), 
                digits.substring(6));
        }
        return phone;
    }
    
    /**
     * Validate phone has exactly 10 digits
     */
    private boolean validatePhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        return digits.length() == 10;
    }
    
    /**
     * Display services and handle selection
     */
    private boolean selectService() {
        while (true) {
            try {
                List<Service> services = getServices();
                
                System.out.println("\n" + BOLD + GOLD + "Available Services:" + RESET + "\n");
                
                for (Service svc : services) {
                    System.out.printf("  %s%d)%s %s%s%s%n", 
                        GOLD, svc.serviceId, RESET, 
                        WHITE, capitalize(svc.name), RESET);
                }
                
                System.out.print("\n" + GRAY + "Enter service number: " + RESET);
                String input = scanner.nextLine().trim();
                
                if (!input.matches("\\d+")) {
                    printError("Please enter a valid number");
                    continue;
                }
                
                int serviceId = Integer.parseInt(input);
                Service service = getServiceById(serviceId);
                
                if (service != null) {
                    selectedService = service;
                    printSuccess("Selected: " + capitalize(service.name));
                    return true;
                } else {
                    printError("Invalid service selection");
                }
                
            } catch (SQLException e) {
                printError("Database error: " + e.getMessage());
                return false;
            }
        }
    }
    
    /**
     * Handle customer identification or creation
     */
    private boolean getCustomerInfo() {
        System.out.print("\n" + GRAY + "What's your phone number? " + RESET);
        String phone = scanner.nextLine().trim();
        
        if (!validatePhone(phone)) {
            printError("Please enter a valid 10-digit phone number");
            return getCustomerInfo();
        }
        
        String formattedPhone = formatPhone(phone);
        
        try {
            // Check existing
            Customer customer = findCustomerByPhone(formattedPhone);
            
            if (customer != null) {
                currentCustomer = customer;
                printSuccess("Welcome back, " + customer.name + "!");
                return true;
            }
            
            // New customer
            printInfo("I don't have a record for that phone number.");
            System.out.print(GRAY + "What's your name? " + RESET);
            String name = scanner.nextLine().trim();
            
            if (name.isEmpty()) {
                printError("Name cannot be empty");
                return getCustomerInfo();
            }
            
            int customerId = createCustomer(name, formattedPhone);
            currentCustomer = new Customer(customerId, name, formattedPhone);
            printSuccess("Profile created for " + name);
            return true;
            
        } catch (SQLException e) {
            printError("Database error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get appointment time and create booking
     */
    private boolean scheduleTime() {
        System.out.print("\n" + GRAY + "What time would you like your " + 
            selectedService.name + ", " + currentCustomer.name + "? " + RESET);
        String time = scanner.nextLine().trim();
        
        if (time.isEmpty()) {
            printError("Please enter a time");
            return scheduleTime();
        }
        
        try {
            int appointmentId = createAppointment(
                currentCustomer.customerId,
                selectedService.serviceId,
                time
            );
            
            // Print confirmation
            System.out.println("\n" + GOLD + "=".repeat(50) + RESET);
            System.out.println(GREEN + BOLD);
            System.out.println("  I have put you down for a " + selectedService.name + " at " + time + ",");
            System.out.println("  " + currentCustomer.name + ".");
            System.out.println(RESET);
            System.out.println(GOLD + "=".repeat(50) + RESET + "\n");
            
            return true;
            
        } catch (SQLException e) {
            printError("Failed to create appointment: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Display customer's appointments
     */
    private void viewAppointments() {
        if (currentCustomer == null) return;
        
        try {
            List<Appointment> appointments = getCustomerAppointments(currentCustomer.customerId);
            
            if (appointments.isEmpty()) {
                printInfo("No appointments scheduled");
                return;
            }
            
            System.out.println("\n" + BOLD + GOLD + "Your Appointments:" + RESET + "\n");
            for (Appointment apt : appointments) {
                System.out.printf("  %s• %s at %s%s%n", 
                    WHITE, capitalize(apt.serviceName), apt.time, RESET);
            }
            
        } catch (SQLException e) {
            printError("Failed to load appointments: " + e.getMessage());
        }
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    /**
     * Main application flow
     */
    public void run() {
        if (!connect()) {
            System.exit(1);
        }
        
        // Enable transaction management
        try {
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            printError("Transaction setup failed: " + e.getMessage());
        }
        
        printHeader("LUXE SALON APPOINTMENT SCHEDULER");
        System.out.println(GRAY + "Welcome to our premium scheduling system" + RESET);
        
        if (selectService() && getCustomerInfo() && scheduleTime()) {
            viewAppointments();
        }
        
        disconnect();
        System.out.println("\n" + GOLD + "Thank you for choosing Luxe Salon" + RESET + "\n");
    }
    
    public static void main(String[] args) {
        SalonScheduler scheduler = new SalonScheduler();
        
        // Add shutdown hook for cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.disconnect();
        }));
        
        scheduler.run();
    }
}
