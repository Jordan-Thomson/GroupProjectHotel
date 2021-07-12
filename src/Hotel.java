import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * Class to emulate the operation of a Hotel booking system
 * groupwork project CS253
 */
public class Hotel {

    private String userName;
    private String password;
    private Scanner scanner;
    private Connection con;
    private Driver driver;
    private static String url = "jdbc:postgresql://xxxxxx";

    /**
     * Default constructor for Hotel
     */
    public Hotel(String userName, String password) {
        this.userName = userName;
        this.password = password;
        scanner = new Scanner(System.in);
        displayWelcome();
        openCon();
        mainMenu();
    }

    /**
     * Attempt to open a connection to the database
     */
    private void openCon() {
        try {
            con = DriverManager.getConnection(url, userName, password);
            con.setAutoCommit(true);
        } catch (SQLException e) {
            System.out.println("Error connecting to database");
            System.out.println("Ensure VPN is enabled");
            //e.printStackTrace();
            Menu m = new Menu("Error - restart required", new String[] {"1. Exit application"});
            int opt = m.choose();
            System.exit(1);
        }
    }

    /**
     * Attempt to close the connection.
     */
    public void closeCon() {
        try {
            con.close();
        } catch (SQLException e) {
            System.out.println("Error closing connection");
            e.printStackTrace();
        }
    }

    /**
     * Main menu, with option to quit application
     */
    private void mainMenu() {
        Menu main = new Menu("Main Menu", new String[] {"1. Create new Booking","2. Invoice options","3. Hotel Maintenance" ,"4. Reports", "5. Quit"});
        int opt = main.choose();
        switch (opt) {
            case 1:
                createBookingMenu();
                break;
            case 2:
                invoiceMenu();
                break;
            case 3:
                maintenanceMenu();
                break;
            case 4:
                reportMenu();
                break;
            case 5:
                System.out.println("Good bye");
                closeCon();
                System.exit(0);
                break;
            default:
                System.out.println("You shouldn't be here!");
                break;
        }
    }

    /**
     * Menu to handle some system maintenance
     */
    private void maintenanceMenu() {
        Menu maintMenu = new Menu("Maintenance Menu", new String[] {"1. Create new staff","2. Edit room details","3. Back"});
        int opt = maintMenu.choose();
        switch (opt) {
            case 1:
                createStaff();
                break;
            case 2:
                editRoom();
                break;
            case 3:
                mainMenu();
                break;
            default:
                System.out.println("Shouldn't be possible to see this");
                break;
        }
    }

    /**
     * Method to edit the room details, returns the edited room via SQL Statement to console.
     */
    private void editRoom() {
        ResultSet result = getQueryResult("SELECT * FROM room");
        System.out.println("Room No, type, price");
        ArrayList<Integer> roomNo = new ArrayList<>();
        try {
            while (result != null && result.next()) {
                roomNo.add(result.getInt(1));
                System.out.println(result.getInt(1) + "\t" +
                        result.getString(2) + "\t" +
                        result.getFloat(3));
            }
            System.out.print("Enter room to edit: ");
            int roomToEdit =0;
            while (!roomNo.contains(roomToEdit)) {
                roomToEdit = getNumber();
                if (!roomNo.contains(roomToEdit)) {
                    System.out.print("Not a valid room number, try again: ");
                }
            }
            Menu roomType = new Menu("Choose Valid Room Type:", new String[]{"1. Twin", "2. Double", "3. Single", "4. Don't change"});
            int opt = roomType.choose();
            String type = (opt == 1 ? "Twin" : (opt == 2 ? "Double" : (opt == 3 ? "Single" : "")));
            System.out.println("Enter new price (blank or 0 for no change): ");
            scanner.nextLine(); // avoid skipping next input
            String newPrice = scanner.nextLine();
            float price = 0;
            try {
                price = Float.parseFloat(newPrice);
            } catch (NumberFormatException ignored) {
            }
            String stmt = "UPDATE room SET ";
            if (price == 0 && type.equals("")) {
                System.out.println("No change required");
                waitMenu();
                maintenanceMenu();
            } else if (price != 0 && type.equals("")) {
                stmt += " price = " + price;
            } else if (price == 0 && !type.equals("")) {
                stmt += " room_type = '" + type + "'";
            } else {
                stmt += " room_type = '" + type + "', price = " + price;
            }
            stmt += " WHERE room_number = " + roomToEdit;
            int update = getUpdateResult(stmt);
            if (update == 0) {
                System.out.println("Nothing was changed!");
            } else if (update > 0) {
                System.out.println("Room updated: ");
                result = getQueryResult("SELECT * FROM room WHERE room_number = " + roomToEdit);
                System.out.println("Room No, type, price");
                while (result != null && result.next()) {
                    System.out.println(result.getInt(1) + "\t" +
                            result.getString(2) + "\t" +
                            result.getFloat(3));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error with SQL, returning to main menu");
            mainMenu();
        }
        waitMenu();
        maintenanceMenu();
    }

    /**
     * Method to generate SQL INSERT statement to create an employee, returns the inserted employee via SQL to console.
     */
    private void createStaff() {
        System.out.print("Enter new employee Number: ");
        int empNo = getNumber();
        scanner.nextLine(); // try to stop it skipping first name
        System.out.print("Enter new employee first name: ");
        String fname = scanner.nextLine();
        System.out.print("Enter new employee last name: ");
        String lname = scanner.nextLine();
        Menu roleMenu = new Menu("Select Role: ",new String[] {"1. Admin", "2. Cleaner"});
        int opt = roleMenu.choose();
        String role = (opt == 1 ? "ADMIN" : "CLEANER");
        int update = getUpdateResult("INSERT INTO staff (employee_no, first_name, last_name, role) VALUES (" + empNo + ",'" + fname + "','" + lname + "','" + role +"')");
        if (update > 0) {
            System.out.print("Row Created: ");
            ResultSet result = getQueryResult("SELECT * FROM staff WHERE employee_no = " + empNo);
            try {
                while (result != null && result.next()) {
                    System.out.print(result.getInt(1) + ", " + result.getString(2) + ", " + result.getString(3) + ", " + result.getString(4));
                }
            } catch (SQLException e) {
                System.out.println("Error retrieving data, row may not have been created or connection lost");
            }
        }
        waitMenu();
        maintenanceMenu();
    }

    /**
     * Method to handle getting numeric input only
     * @return int number entered via console
     */
    private int getNumber() {
        boolean isNumber = false;
        int number =0;
        while(!isNumber) {
            try {
                number = scanner.nextInt();
                isNumber = true;
            } catch (InputMismatchException e) {
                System.out.print("Must be a number: ");
                scanner.nextLine();
            }
        }
        return number;
    }

    /**
     * Generate an menu to for financial options
     */
    private void invoiceMenu() {
        Menu invoiceMenu = new Menu("Invoice Options", new String[] {"1. Generate new Invoice", "2. Generate new Payment", "3. Quit"});
        int opt;
        do {
            invoiceMenu.display();
            opt = invoiceMenu.getInput();
        } while (invoiceMenu.getChoice() < 1 || invoiceMenu.getChoice() > invoiceMenu.getNumOptions() );

        switch (opt) {
            case 1:
                generateInvoice();
                break;
            case 2:
                generatePayment();
                break;
            case 3:
                mainMenu();
                break;
            default:
                System.out.println("Should not be possible to get here");
                break;
        }
    }

    /**
     * Method to create SQL statement to insert a payment.
     */
    private void generatePayment() {
        ArrayList<Float> value = new ArrayList<>();
        ArrayList<Integer> inv = new ArrayList<>();
        ResultSet result = getQueryResult("SELECT guest.first_name, guest.last_name, booking.booking_id, invoice.invoice_number, invoice.invoice_value - SUM(payment_value) AS outstanding\n" +
                "\tFROM guest, booking, invoice, payment\n" +
                "\tWHERE guest.guest_id = booking.guest_id AND booking.booking_id = invoice.booking_ref AND invoice.invoice_number = payment.invoice_no\n" +
                "\tGROUP BY guest.first_name, guest.last_name, booking.booking_id, invoice.invoice_number, invoice.invoice_value\n" +
                "\tHAVING invoice.invoice_value > SUM(payment_value);");
        System.out.println("inv, booking, outstanding, name");
        try {
            while (result != null && result.next()) {
                inv.add(result.getInt(3));
                value.add(result.getFloat(5));
                System.out.println(result.getInt(3) + "\t" +
                        result.getInt(4) + "\t" +
                        result.getFloat(5) + "\t" +
                        result.getString(1) + " " +
                        result.getString(2));
            }

            System.out.print("Enter invoice from list to generate a payment against: ");
            int invoice = 0;
            do {
                invoice = scanner.nextInt();
                if (!inv.contains(invoice)) System.out.println("Not a valid invoice number, try again: ");
            } while (!inv.contains(invoice));
            System.out.print("Enter value being paid: ");
            float stillToPay = 0;
            for (int i = 0; i < inv.size(); i++) {
                if (inv.get(i) == invoice) {
                    stillToPay = value.get(i);
                }
            }
            float paid;
            do {
                paid = scanner.nextInt();
                if (paid <= 0) {
                    System.out.println("Must be a positive value");
                } else if (paid > stillToPay) {
                    System.out.println("Can't take payment more than is left outstanding");
                }
            } while (paid <= 0 || paid > stillToPay);
            Menu cc = new Menu("Cash or Card payment?", new String[] {"1. Cash", "2. Card"});
            int choice = cc.choose();
            String method = (choice == 1 ? "CASH" : "CARD");
            int empNo = getEmpNo();
            LocalDate today = LocalDate.now();
            int insert = getUpdateResult("INSERT INTO payment (payment_value,payment_method,payment_date,invoice_no,generated_by) " +
                    "VALUES (" + paid + ",'" + method + "','" + today + "', " + invoice + "," + empNo + ")");
            if (insert > 0) {
                System.out.println("Payment created");
                result = getQueryResult("SELECT * FROM payment WHERE payment_number = " + insert);
                System.out.println("Payment_no, value, method, date, generated_by, invoice");
                while(result != null && result.next()) {
                    System.out.println(result.getInt(1) + ", " + result.getFloat(2) + ", " + result.getString(3) + ", " +
                            result.getString(4) + ", " + result.getInt(5) + ", " + result.getInt(6));
                }
            }
            else {
                System.out.println("Error creating payment");
            }
        } catch (SQLException e) {
            System.out.println("Error " + e);
        }
        mainMenu();
    }

    /**
     * Method to perform checks when creating an invoice
     */
    private void generateInvoice()  {
        ResultSet result = getQueryResult("SELECT * \n" +
                "  FROM booking, guest\n" +
                "  WHERE booking.guest_id = guest.guest_id AND NOT EXISTS\n" +
                "   (SELECT * \n" +
                "    FROM invoice\n" +
                "\tWHERE invoice.booking_ref = booking.booking_id);");
        ArrayList<Integer> bookings = new ArrayList<>();
        try {
            while (result != null && result.next()) {
                bookings.add(result.getInt(1));
                System.out.println(result.getInt(1) + "\t" +
                        result.getString(2) + "\t" +
                        result.getString(3) + "\t" +
                        result.getInt(4) + "\t" +
                        result.getInt(6) + "\t" +
                        result.getString(8) + "\t" +
                        result.getString(9));
            }

            if (bookings.isEmpty()) {
                System.out.println("No bookings require an invoice!");
                mainMenu();
            }
            System.out.print("Enter booking to invoice: ");
            int bookingChoice = 0;
            while (!bookings.contains(bookingChoice)) {
                bookingChoice = scanner.nextInt();
            }
            int empNo = getEmpNo();

            LocalDate today = LocalDate.now();
            int insert = getUpdateResult("INSERT INTO invoice (invoice_date,booking_ref,generated_by) VALUES ('" + today + "'," +bookingChoice + "," + empNo + ")");
            if (insert > 0) {
                System.out.println("Invoice created");
                result = getQueryResult("SELECT * FROM invoice WHERE invoice_number = " + insert);
                System.out.println("Invoice No, value, date, generated_by, booking ref");
                while(result != null && result.next()) {
                    System.out.println(result.getInt(1) + ", " + result.getFloat(2) + ", " + result.getString(3) + ", "
                            + result.getInt(4) + ", " + result.getInt(5) );
                }
            }
            else {
                System.out.println("Error creating invoice");
            }
        } catch (SQLException e) {
            System.out.println("Error " + e);
            System.out.println("Returning to main menu");
            mainMenu();
        }
        mainMenu();
    }

    /**
     * Method to get an employee number, gives option to return to main menu
     * @return int that is the employee number
     */
    private int getEmpNo() {
        ResultSet result;
        System.out.print("Enter employee number: ");
        boolean cantInvoice = true;
        int empNo = 0;
        while (cantInvoice) {
            String role = "";
            empNo = getNumber();
            result = getQueryResult("SELECT role FROM staff WHERE employee_no = " + empNo);
            try {
                if (result != null && result.next()) {
                    role = result.getString(1);
                }
            } catch (SQLException e) {
                System.out.println("Error " + e);
                System.out.println("Returning to main menu");
                mainMenu();
            }
            if (role.equals("ADMIN")) {
                cantInvoice = false;
            }
            else {
                System.out.println("User can't create invoices or payments, returning to main menu");
                Menu m = new Menu("Escape", new String[] {"1. Retry", "2. Return to main Menu"});
                int opt = m.choose();
                if (opt == 2) {
                    mainMenu();
                }
            }
        }
        return empNo;
    }

    /**
     * Method to perform checks when creating a booking
     */
    private void createBookingMenu() {
        int guestId = 0;
        System.out.print("Enter guests first name: ");
        String fname = scanner.nextLine();
        System.out.print("Enter guests last name: ");
        String lname = scanner.nextLine();
        ResultSet result;
        try {
            result = getQueryResult("SELECT * FROM guest WHERE first_name = '" + fname + "' AND last_name ='" + lname + "'");
            if (result != null && result.next()) {
                guestId = result.getInt(1);
            }
            else {
                int insert = getUpdateResult("INSERT INTO guest (first_name,last_name) VALUES ('" + fname +"','" + lname +"')");
                result = getQueryResult("SELECT * FROM guest WHERE guest_id = " + insert);
                if (result != null && result.next()) {
                    guestId = result.getInt(1);
                }
            }
            LocalDate arr = LocalDate.now();
            LocalDate dep = LocalDate.now();
            String arrival = "";
            String departure = "";
            while (arr.compareTo(dep) >= 0) {
                arrival = getValidDate("Enter date of Arrival");
                departure = getValidDate("Enter date of Departure");
                arr = LocalDate.parse(arrival);
                dep = LocalDate.parse(departure);
                if (arr.compareTo(dep) >= 0) System.out.println("departure must be after arrival");
            }
            long diff = dateDifference(arr, dep);
            result = getQueryResult("SELECT * FROM room WHERE NOT EXISTS " +
                    "(SELECT * FROM booking WHERE room_number = room_no" +
                    " AND (('" + arrival + "' >= arrival AND '" + arrival + "' <= departure)" +
                    " OR ('" + departure + "' <= departure AND '" + departure + "' >= arrival)" +
                    " OR (arrival >= '" + arrival + "' AND arrival <= '" + departure + "')" +
                    " OR (departure >= '" + arrival + "' AND departure <= '" + departure + "')))");
            int count = 0;
            ArrayList<Integer> availRooms = new ArrayList<>();
            while(result != null && result.next()) {
                count++;
                availRooms.add(result.getInt(1));
                System.out.println("Room no " + result.getInt(1) + " type: " + result.getString(2) + " price per night: " + result.getFloat(3) +
                        " | Total Price = " + result.getFloat(3) * diff);
            }
            if (count == 0) {
                System.out.println("No rooms available on chosen dates, sorry.");
                mainMenu();
            }
            System.out.print("Enter desired room: ");
            int roomChoice = 0;
            while (!availRooms.contains(roomChoice)) {
                roomChoice = getNumber();
                if (!availRooms.contains(roomChoice)) {
                    System.out.print("Not an available room, please try again: ");
                }
            }
            System.out.print("Enter party size: ");
            int party_size = getNumber();
            int updated = getUpdateResult("INSERT INTO BOOKING (guest_id, room_no, arrival, departure, party_size) VALUES ("+ guestId + "," + roomChoice + ",'" + arrival + "','" + departure + "'," + party_size +")");
            result = getQueryResult("SELECT * FROM booking, guest, room where booking_id = " + updated + " AND booking.guest_id = guest.guest_id AND room_no = room_number");
            while (result != null && result.next()) {
                System.out.println("Booking ID: " + result.getInt(1));
                System.out.println("Customer: " + result.getString(8) + " " + result.getString(9));
                System.out.println("Arrival: " + result.getString(2) + " | Departure: " + result.getString(3));
                System.out.println("Room No: " + result.getInt(6) + " | Room Type: " + result.getString(11) + " | ppn: " + result.getString(12));
            }
            mainMenu();
        } catch (SQLException e) {
            System.out.println("Error: " + e);
            System.out.println("Returning to main menu");
            mainMenu();
        }
    }

    /**
     * Method to determine if String date is in correct format
     * @param dateToAsk String asking for the date
     * @return String with valid date format
     */
    private String getValidDate(String dateToAsk) {
        String date;
        while(true) {
            System.out.println(dateToAsk);
            date = scanner.nextLine();
            try {
                LocalDate.parse(date);
                return date;
            } catch (DateTimeParseException e) {
                System.out.println("Not valid date, try YYYY-MM-DD");
            }
        }
    }

    private long dateDifference(LocalDate date1, LocalDate date2) {
        return ChronoUnit.DAYS.between(date1, date2);
    }

    /**
     * Method to generate and handle a list of reports/queries
     */
    private void reportMenu() {
        Menu reportMenu = new Menu("Queries", new String[] {"1. List all Guests", "2. List all Bookings with more than 1 party",
                "3. Card or Cash payments on a date", "4. Total number of bookings", "5. Display bookings without invoice", "6. Show outstanding invoices",
                "7. back"});
        int opt = reportMenu.choose();
        switch(opt) {
            case 1:
                displayGuests();
                break;
            case 2:
                moreThanOne();
                break;
            case 3:
                paymentsOnDate();
                break;
            case 4:
                displayCountBookings();
                break;
            case 5:
                displayBookingsWithoutInvoice();
                break;
            case 6:
                outstandingInvoices();
            case 7:
                mainMenu();
                break;
            default:
                System.out.println("Not an option here!");
        }
    }

    /**
     * Method to create SQL statement and display bookings with party size > 1
     */
    private void moreThanOne()  {
        ResultSet result = getQueryResult("SELECT *\n" +
                " FROM booking\n" +
                " GROUP BY booking_id\n" +
                " HAVING(party_size) > 1\n" +
                " ORDER BY booking_id;");
        System.out.println("Booking, arrival, departure, party_size, guest_id, room_no");
        try {
            while (result != null && result.next()) {
                System.out.println(result.getInt(1) + "\t" +
                        result.getString(2) + "\t" +
                        result.getString(3) + "\t" +
                        result.getInt(4) + "\t" +
                        result.getInt(5) + "\t" +
                        result.getInt(6));
            }
        } catch (SQLException e) {
            System.out.println("Error " + e);
            System.out.println("Returning to main menu");
            mainMenu();
        }
        waitMenu();
        reportMenu();
    }

    /**
     * Method to generate SQL statement to display value of payments on a date
     */
    private void paymentsOnDate() {
        String stmt = "SELECT SUM(payment_value) FROM payment";
        Menu m = new Menu("Options", new String[] {"1. Card only", "2. Cash only", "3. Card and Cash"});
        int opt = m.choose();
        switch(opt) {
            case 1:
                stmt += " WHERE payment_method = 'CARD'";
                break;
            case 2:
                stmt += " WHERE payment_method = 'CASH'";
                break;
        }
        String date = getValidDate("Enter Date: ");
        if(stmt.contains("WHERE")) {
            stmt += " AND ";
        }
        else {
            stmt += " WHERE ";
        }
        stmt += "payment_date = '" + date + "'";
        ResultSet result = getQueryResult(stmt);
        try {
            while (result != null && result.next()) {
                System.out.println("Revenue = £" + result.getFloat(1));
            }
        } catch (SQLException e) {
            System.out.println("Error " + e);
            System.out.println("Returning to main menu");
            mainMenu();
        }
        waitMenu();
        reportMenu();
    }

    /**
     * Method to generate SQL statement and display outstanding invoices
     */
    private void outstandingInvoices() {
        ResultSet result = getQueryResult("SELECT guest.first_name, guest.last_name, booking.booking_id, invoice.invoice_number, invoice.invoice_value - SUM(payment_value) AS outstanding\n" +
                "\tFROM guest, booking, invoice, payment\n" +
                "\tWHERE guest.guest_id = booking.guest_id AND booking.booking_id = invoice.booking_ref AND invoice.invoice_number = payment.invoice_no\n" +
                "\tGROUP BY guest.first_name, guest.last_name, booking.booking_id, invoice.invoice_number, invoice.invoice_value\n" +
                "\tHAVING invoice.invoice_value > SUM(payment_value);");
        System.out.println("inv, booking, outstanding, name");
        try {
            while (result != null && result.next()) {
                System.out.println(result.getInt(3) + "\t" +
                        result.getInt(4) + "\t" +
                        result.getFloat(5) + "\t" +
                        result.getString(1) + " " +
                        result.getString(2));
            }
        } catch (SQLException e) {
            System.out.println("Error " + e);
        }
        waitMenu();
        reportMenu();
    }

    /**
     * Method to set SQL string and display bookings not yet invoiced.
     */
    private void displayBookingsWithoutInvoice() {
        ResultSet result = getQueryResult("SELECT * FROM booking WHERE NOT EXISTS (SELECT * FROM invoice WHERE booking_ref = booking.booking_id)");
        System.out.println("Booking, arrival, departure, party, guest_id, room_no");
        try{
            while (result != null && result.next()) {
                System.out.println(result.getInt(1) + "\t" + result.getString(2) + "\t" + result.getString(3) +
                        "\t" + result.getInt(4) + "\t" + result.getInt(5) + "\t" + result.getInt(6) );
            }
        } catch (SQLException e) {
            System.out.println("Error " + e);
            System.out.println("Returning to main menu");
            mainMenu();
        }
        waitMenu();
        reportMenu();
    }

    /**
     * Method to form SQL query and display total number of bookings
     */
    private void displayCountBookings() {
        ResultSet result = getQueryResult("SELECT COUNT(booking_id) FROM booking");
        try {
            if (result != null && result.next()) {
                System.out.println("Total Bookings: " + result.getInt(1));
            } else {
                System.out.println("No Bookings");
            }
        } catch (SQLException e) {
            System.out.println("Error " + e);
            System.out.println("Returning to main menu");
            mainMenu();
        }
        waitMenu();
        reportMenu();
    }

    /**
     * Method to collate SQL statement and to display all guests
     */
    private void displayGuests() {
        String stmt = "SELECT * FROM guest ";
        Menu menu = new Menu("Display Guests Options", new String[] {"1. By First Name","2. By Last Name", "3. Back"});
        int opt = menu.choose();
        switch(opt) {
            case 1:
                stmt += "ORDER BY first_name";
                break;
            case 2:
                stmt += "ORDER BY last_name";
                break;
            case 3:
                reportMenu();
                break;
            default:
                System.out.println("You can't get here");
        }

        ResultSet result = getQueryResult(stmt);
        try {
            while (result != null && result.next()) {
                System.out.println(result.getInt(1) + "\t " + result.getString(2) + "\t " + result.getString(3));
            }
        } catch (SQLException e) {
            System.out.println("Error " + e);
            System.out.println("Returning to main menu");
            mainMenu();
        }
        waitMenu();
        reportMenu();
    }

    /**
     * Just a way to stop the next menu appearing straight away.
     */
    private void waitMenu() {
        Menu wait = new Menu("",new String[] {"1. Continue"});
        wait.display();
        wait.getInput();
    }

    /**
     * Returns ResultSet of a query from connected database
     * @param query String SQL query
     * @return ResultSet from query or null if connection error
     */
    private ResultSet getQueryResult(String query) {
        try {
            PreparedStatement ps = con.prepareStatement(query);
            System.out.println(query);
            return ps.executeQuery();
        } catch (SQLException e) {
            System.out.println("Error connecting to database: " + e);
            return null;
        }
    }

    /**
     * Returns int results of update to connected database
     * @param query String SQL query (INSERT, ALTAR, DELETE, etc)
     * @return int representing status, 0 = no updates, > 0 = num rows affected, < 0 = mass delete on segmented table space or the generated key.
     */
    private int getUpdateResult(String query) {
        try {
            PreparedStatement ps;
            ps = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            System.out.println(query);
            int update = ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return update;
        } catch (SQLException e) {
            System.out.println("Error connecting to database: " + e);
            return 0;
        }
    }

    private void displayWelcome() {
        System.out.println("Welcome to the Hotel Booking Software Application (trade mark pending).");
        System.out.println("Please select an option from the Menu by entering its corresponding number.");
        System.out.println();
    }

    public static void main(String[] args) {
        if (args.length == 2) {
            new Hotel(args[0], args[1]);
        }
        else {
            System.out.println("Usage: java hotel <username> <password>");
            System.exit(0);
        }

    }
}
