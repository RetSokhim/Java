package org.example;
import org.nocrala.tools.texttablefmt.BorderStyle;
import org.nocrala.tools.texttablefmt.CellStyle;
import org.nocrala.tools.texttablefmt.ShownBorders;
import org.nocrala.tools.texttablefmt.Table;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static final String BOLD = "\033[1m";
    public static final String RED = "\033[0;31m";
    public static final String GREEN = "\033[0;32m";
    public static final String YELLOW = "\033[0;33m";
    public static final String BLUE = "\033[0;34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String RESET = "\u001B[0m";
    static Scanner scanner = new Scanner(System.in);
    static Connection connection = null;
    static Statement statement = null;
    static ResultSet resultSet = null;
    static CellStyle cellStyle = new CellStyle(CellStyle.HorizontalAlign.center);
    private static final int DEFAULT_PAGE_SIZE = 5;
    static int currentPage = 1;
    static int pageSize = DEFAULT_PAGE_SIZE;
    static List<String> pendingInserts = new ArrayList<>();
    static List<String> pendingUpdates = new ArrayList<>();

    public static void main(String[] args) {
        try {
            System.out.println(GREEN+"Loading Please Wait..."+RESET);
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/postgres",
                    "postgres",
                    "092367169");
            statement = connection.createStatement();
            //create save_page_tb before if not yet has
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS product_tb (ID SERIAL PRIMARY KEY,Name VARCHAR(100),Unit_Price FLOAT,Stock_QTY INTEGER,Import_Date DATE DEFAULT current_date )");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS save_page_tb (key VARCHAR(50) PRIMARY KEY,value INTEGER)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS save_back_up_list_tb (ID SERIAL PRIMARY KEY,List_Of_Table VARCHAR(300))");
            loadConfigFromDatabase();
            // Create Table
            while (true) {
                //Display Table
                renderTable(currentPage);

                System.out.println(BLUE+BOLD+"F)"+RESET+BOLD+" First \t"+RESET+BLUE+BOLD+ "P)"+RESET+BOLD+" Previous \t "+RESET +BLUE+BOLD+"N)"+RESET+BOLD+" Next \t" +RESET+BLUE+BOLD+"L)"+RESET+BOLD+" Last \t"+RESET+ BLUE+BOLD+"G)"+RESET+BOLD+" Goto"+RESET);
                System.out.println();
                System.out.println(YELLOW+BOLD+"*)"+RESET+BLUE+BOLD+" Display"+RESET);
                System.out.println();
                System.out.println(BLUE+BOLD+"W)"+RESET+BOLD+" Write \t"+RESET+BLUE+BOLD+" R)"+RESET+BOLD+" Read \t\t"+RESET+BLUE+BOLD+" U)"+RESET+BOLD+" Update \t\t"+RESET+BLUE+BOLD+" D)"+RESET+BOLD+" Delete \t\t"+RESET+BLUE+BOLD+" S)"+RESET+BOLD+" Search \t\t"+RESET+BLUE+BOLD+" Se)"+RESET+BOLD+" Set Row"+RESET);
                System.out.println(BLUE+BOLD+"Sa)"+RESET+BOLD+" Save \t"+RESET+BLUE+BOLD+" Un)"+RESET+BOLD+" Unsaved \t"+RESET+BLUE+BOLD+" Ba)"+RESET+BOLD+" Back Up \t"+RESET+BLUE+BOLD+" Re)"+RESET+BOLD+" Restore \t"+RESET+BLUE+BOLD+" E)"+RESET+BOLD+" Exit"+RESET);
                System.out.println();
                String choice = validateInput(YELLOW+BOLD+"=>"+RESET+BOLD+" Please Choose : "+RESET,"^[Ff Pp Nn Ll Gg Ww Rr Uu Dd Ss Ee Aa Nn Bb Rr ]+$",RED+BOLD+"Please input valid choice...! Try Again"+RESET);

                switch (choice.toLowerCase()) {
                    case "w" -> insert();
                    case "r" -> read();
                    case "u" -> update();
                    case "d" -> delete();
                    case "f" -> currentPage = 1;
                    case "p" -> {
                        if (currentPage > 1) {
                            currentPage--;
                        } else {
                            System.out.println(YELLOW+BOLD+"Already on the first page."+RESET);
                        }
                    }
                    case "n" -> {
                        if(currentPage == getTotalPages()){
                            System.out.println(YELLOW+BOLD+"You are on last page"+RESET);
                        }else {
                            currentPage++;
                        }
                    }
                    case "l" -> currentPage = getTotalPages();
                    case "s" -> search();
                    case "g" -> {
                        System.out.println(YELLOW+BOLD+"=>"+RESET+BOLD+"Enter page number to go to: "+RESET);
                        int pageNumber = scanner.nextInt();
                        scanner.nextLine();
                        if (pageNumber >= 1 && pageNumber <= getTotalPages()) {
                            currentPage = pageNumber;
                        } else {
                            System.out.println(RED+BOLD+"Invalid page number."+RESET);
                        }
                    }
                    case "se" -> setRows();
                    case "sa" -> saveChanges();
                    case "un" -> unsavedChange();
                    case "ba" -> backupTable();
                    case "re" -> restoreTable();
                    case "e" -> {
                        System.out.println(GREEN+"=================================================="+RESET);
                        System.out.println(YELLOW+BOLD+"               (^-^) Good Bye! (^-^)"+RESET);
                        System.out.println(GREEN+"=================================================="+RESET);
                        System.exit(0);
                    }
                    default -> System.out.println(RED +BOLD+"Invalid Option ...! Please Try Again" + RESET);
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }
    //For Display Table
    private static void renderTable(int page) throws SQLException {
        //Find total record
        ResultSet rowResultSet = statement.executeQuery("SELECT COUNT(*) FROM product_tb");
        rowResultSet.next();
        int totalRows = rowResultSet.getInt(1);

        // Limit set for yk pun man row mk show and Offset use for romlong pun man row mk show
        int offset = (page - 1) * pageSize;
        resultSet = statement.executeQuery("SELECT * FROM product_tb ORDER BY ID ASC LIMIT " + pageSize + " OFFSET " + offset);

        Table table = new Table(5, BorderStyle.UNICODE_BOX_DOUBLE_BORDER_WIDE, ShownBorders.ALL);
        table.setColumnWidth(0, 10, 15);
        table.setColumnWidth(1, 10, 15);
        table.setColumnWidth(2, 10, 15);
        table.setColumnWidth(3, 10, 15);
        table.setColumnWidth(4, 10, 15);

        getData(table, resultSet);
        table.addCell(BLUE+BOLD+"Page : "+RESET+YELLOW+BOLD+currentPage+RESET+BLUE+BOLD+ " Of "+RESET+YELLOW+BOLD+ getTotalPages()+RESET, cellStyle, 2);
        table.addCell(BLUE+BOLD+"Total Record : " +RESET+YELLOW+BOLD+ totalRows+RESET, cellStyle,3);

        System.out.println(table.render());
    }
    //Get Total page From Table
    private static int getTotalPages() throws SQLException {
        resultSet = statement.executeQuery("SELECT COUNT(*) FROM product_tb");
        resultSet.next();
        int totalRows = resultSet.getInt(1);
        // Calculate somrab rok page sarob
        return (int) Math.ceil((double) totalRows / pageSize);
    }
    //Load row that has been set into database and load it when re-run the code
    private static void loadConfigFromDatabase() {
        try {
            ResultSet resultSet = statement.executeQuery("SELECT value FROM save_page_tb WHERE key = 'pageSize'");
            if (resultSet.next()) {
                //ber mean lek knong column value yk mk dak set page auto
                pageSize = resultSet.getInt("value");
            } else {
                // ber ot set page tam default del DEFAULT_PAGE_SIZE = 5
                pageSize = DEFAULT_PAGE_SIZE;
                // Initialize pageSize in the database
                PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO save_page_tb (key, value) VALUES (?, ?)");
                insertStatement.setString(1, "pageSize");
                insertStatement.setInt(2, pageSize);
                insertStatement.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println(RED +BOLD+ "Error loading configuration from database: " + RESET + e.getMessage());
            // Ber Error doy sa yk pi data base ot kert yk pi default doch knea
            pageSize = DEFAULT_PAGE_SIZE;
        }
    }
    //set row
    private static void setRows() {

        int rows = Integer.parseInt(validateInput(YELLOW+BOLD+"=> "+RESET+BOLD+"Enter number of rows per page: "+RESET, "\\d+", RED +BOLD+ "Invalid product quality...! Please Try Again" + RESET));

        if (rows > 0) {
            // Update the pageSize variable
            pageSize = rows;
            // Update the configuration in the database
            updateConfigInDatabase();
            System.out.println(GREEN+BOLD+"Row size updated successfully."+RESET);
        } else {
            System.out.println(RED+BOLD+"Invalid row count."+RESET);
        }
    }
    //Update Value in save_page_tb pel set row hz
    private static void updateConfigInDatabase() {
        try {
            PreparedStatement updateStatement = connection.prepareStatement("UPDATE save_page_tb SET value = ? WHERE key = 'pageSize'");
            updateStatement.setInt(1, pageSize);
            updateStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println(RED +BOLD+ "Error updating configuration in database: " + RESET + e.getMessage());
        }
    }
    private static void insert() throws SQLException {
        String proName = validateInput(YELLOW+BOLD+"=> "+RESET+BOLD+"Enter Product Name: "+RESET, "^[a-zA-Z\\s]+$", RED +BOLD+ "Invalid product name...! Please Try Again" + RESET);
        double proPrice = Double.parseDouble(validateInput(YELLOW+BOLD+"=> "+RESET+BOLD+"Enter Product Price: "+RESET, "\\d+(\\.\\d+)?", RED +BOLD+ "Invalid product price...! Please Try Again" + RESET));
        int proQTY = Integer.parseInt(validateInput(YELLOW+BOLD+"=> "+RESET+BOLD+"Enter Product QTY: "+RESET, "\\d+", RED +BOLD+ "Invalid product quality...! Please Try Again" + RESET));

        PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO product_tb (id,Name,Unit_Price, Stock_QTY,current_date) VALUES (default,?, ?, ?,default)");
        insertStatement.setString(1, proName);
        insertStatement.setDouble(2, proPrice);
        insertStatement.setInt(3, proQTY);
        String insertSQL = String.format("INSERT INTO product_tb (id,Name, Unit_Price, Stock_QTY,import_date) VALUES (default,'%s', %f, %d,default)", proName, proPrice, proQTY);
        pendingInserts.add(insertSQL);
        System.out.println(BOLD+"Insertion is pending. Use"+RESET+GREEN+BOLD+" 'Sa'"+RESET+BOLD+" to save."+RESET);
    }
    private static void read() throws SQLException {
        int readId = Integer.parseInt(validateInput(YELLOW+BOLD+"=> "+RESET+BOLD+"Enter ID to show product : "+RESET, "\\d+", RED + BOLD+"Invalid ID product ...! Please Try Again" + RESET));

        Table table = new Table(5, BorderStyle.UNICODE_BOX_DOUBLE_BORDER_WIDE, ShownBorders.ALL);

        PreparedStatement readStatement = connection.prepareStatement("SELECT * FROM product_tb  WHERE ID=?");
        readStatement.setInt(1, readId);
        ResultSet resultSet = readStatement.executeQuery();
        getData(table, resultSet);
        System.out.println(table.render());
        readStatement.execute();
        press();
    }
    private static void delete() throws SQLException {

        int deleteId = Integer.parseInt(validateInput(YELLOW+BOLD+"=> "+RESET+BOLD+"Enter ID to delete the product : "+RESET, "\\d+", RED +BOLD+ "Invalid ID product ...! Please Try Again" + RESET));

        Table table = new Table(5, BorderStyle.UNICODE_BOX_DOUBLE_BORDER_WIDE, ShownBorders.ALL);
        table.setColumnWidth(0,10,15);
        table.setColumnWidth(1,10,15);
        table.setColumnWidth(2,10,15);
        table.setColumnWidth(3,10,15);
        table.setColumnWidth(4,10,15);

        table.addCell(YELLOW+BOLD+"ID"+RESET, cellStyle);
        table.addCell(GREEN+BOLD+"Name"+RESET, cellStyle);
        table.addCell(CYAN+BOLD+"Unit Price"+RESET, cellStyle);
        table.addCell(MAGENTA+BOLD+"QTY"+RESET, cellStyle);
        table.addCell(BLUE+BOLD+"Import"+RESET);

        PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM product_tb WHERE ID=?");
        selectStatement.setInt(1, deleteId);
        ResultSet resultSet = selectStatement.executeQuery();

        if (resultSet.next()) {
            table.addCell(YELLOW+BOLD+resultSet.getString("ID")+RESET, cellStyle);
            table.addCell(GREEN+BOLD+resultSet.getString("Name")+RESET, cellStyle);
            table.addCell(CYAN+BOLD+resultSet.getString("Unit_Price")+RESET, cellStyle);
            table.addCell(MAGENTA+BOLD+resultSet.getString("Stock_QTY")+RESET, cellStyle);
            table.addCell(BLUE+BOLD+resultSet.getString("Import_Date")+RESET, cellStyle);
            System.out.println(table.render());

            String confirmation = validateInput(BLUE+BOLD + "Are you sure you want to delete this product? (Y/N): " + RESET,"^[Yy Nn]$",RED+"Please Input Only Y and N to confirm !"+RESET);
            if (confirmation.equalsIgnoreCase("Y")) {
                try {
                    connection.setAutoCommit(false); // Disable auto-commit
                    PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM product_tb WHERE ID=?");
                    deleteStatement.setInt(1, deleteId);
                    //show how many row affect by deletion
                    int rowsAffected = deleteStatement.executeUpdate();
                    if (rowsAffected > 0) {
                        System.out.println(GREEN + BOLD+"Product deleted successfully." + RESET);
                        connection.commit(); // Commit the transaction
                    } else {
                        System.out.println(RED +BOLD+ "Failed to delete product." + RESET);
                        connection.rollback(); // Rollback the transaction
                    }
                } catch (SQLException e) {
                    connection.rollback(); // Rollback in case of exception
                    System.out.println(GREEN +BOLD+ "Error deleting product: "+ RESET + e.getMessage());
                } finally {
                    connection.setAutoCommit(true); // Re-enable auto-commit
                }
            } else {
                System.out.println(GREEN + BOLD+"Deletion cancelled." + RESET);
            }
        } else {
            System.out.println(RED + BOLD+"Product with ID " + deleteId + " not found." + RESET);
        }
    }
    private static void update() throws SQLException {

        int productId = Integer.parseInt(validateInput(YELLOW+BOLD+"=> "+RESET+BOLD+"Input ID of the product to update: "+RESET, "\\d+", RED +BOLD+ "\tInvalid ID product...! Please Try Again" + RESET));
        String upName = validateInput(YELLOW+BOLD+"=> "+RESET+BOLD+"Enter Product Name: "+RESET, "^[a-zA-Z\\s]+$", RED + BOLD+"\tInvalid product name...! Please Try Again" + RESET);
        double upPrice = Double.parseDouble(validateInput(YELLOW+BOLD+"=> "+RESET+BOLD+"Enter Product Price: "+RESET, "\\d+(\\.\\d+)?", RED +BOLD+ "\tInvalid product price...! Please Try Again" + RESET));
        int upQTY = Integer.parseInt(validateInput(YELLOW+BOLD+"=> "+RESET+BOLD+"Enter Product QTY: "+RESET, "\\d+", RED + BOLD+"\tInvalid product quality...! Please Try Again" + RESET));

        PreparedStatement updateStatement = connection.prepareStatement("UPDATE product_tb SET Name=?, Unit_Price=?, Stock_QTY=? WHERE ID=?");
        updateStatement.setString(1, upName);
        updateStatement.setDouble(2, upPrice);
        updateStatement.setInt(3, upQTY);
        updateStatement.setInt(4, productId);
        String updateSQL = String.format("UPDATE product_tb SET Name = '%s', Unit_Price = %f, Stock_QTY = %d WHERE ID = %d", upName, upPrice, upQTY, productId);
        pendingUpdates.add(updateSQL);
        System.out.println(BOLD + "Update is pending. Use"+RESET+BLUE+BOLD+" 'Sa'"+RESET+BOLD+" to save." + RESET);
    }
    private static void search() throws SQLException {
        String searchQuery = validateInput(YELLOW+BOLD+"=> "+RESET+BOLD+"Enter Name or Letter to search for:  "+RESET, "^[a-zA-Z\\s]+$", RED +BOLD+ "\tInvalid product name...! Please Try Again" + RESET);

        Table table = new Table(5, BorderStyle.UNICODE_BOX_DOUBLE_BORDER_WIDE, ShownBorders.ALL);
        table.setColumnWidth(0,10,15);
        table.setColumnWidth(1,10,15);
        table.setColumnWidth(2,10,15);
        table.setColumnWidth(3,10,15);
        table.setColumnWidth(4,10,15);

        table.addCell(YELLOW+BOLD+"ID"+RESET, cellStyle);
        table.addCell(GREEN+BOLD+"Name"+RESET, cellStyle);
        table.addCell(CYAN+BOLD+"Unit Price"+RESET, cellStyle);
        table.addCell(MAGENTA+BOLD+"QTY"+RESET, cellStyle);
        table.addCell(BLUE+BOLD+"Import"+RESET);

        PreparedStatement searchStatement = connection.prepareStatement("SELECT * FROM product_tb WHERE Name ILIKE ? ORDER BY ID ASC");
        searchStatement.setString(1, "%" + searchQuery + "%");
        ResultSet resultSet = searchStatement.executeQuery();

        while (resultSet.next()) {
            table.addCell(YELLOW+BOLD+resultSet.getString("ID")+RESET, cellStyle);
            table.addCell(GREEN+BOLD+resultSet.getString("Name")+RESET, cellStyle);
            table.addCell(CYAN+BOLD+resultSet.getString("Unit_Price")+RESET, cellStyle);
            table.addCell(MAGENTA+BOLD+resultSet.getString("Stock_QTY")+RESET, cellStyle);
            table.addCell(BLUE+BOLD+resultSet.getString("Import_Date")+RESET, cellStyle);
        }
        System.out.println(table.render());
        press();
    }
    private static void saveChanges() {
        try {
            String choice = validateInput(YELLOW+BOLD+"=> "+RESET+BLUE+BOLD+"Do you want to save unsaved insertion or unsaved update?\n"+RESET+BLUE+BOLD+"\"ui\""+RESET+" to save unsaved insertion, "+BLUE+BOLD+"\"uu\""+RESET+" to save unsaved update, and "+BLUE+BOLD+"\"b\""+RESET+" to go back \n "+RESET,"^[UI ui Bb]+$",RED+BOLD+"Please Input Only UI and UU to save insertion or update ! Please Try again"+RESET);

            if (choice.equalsIgnoreCase("ui")) {
                if(pendingInserts.isEmpty()){
                    System.out.println(RED+BOLD+"No DATA"+RESET);
                }
                for (String insert : pendingInserts) {
                    statement.executeUpdate(insert);
//                    String insertSQL = String.format("INSERT INTO product_tb (id,Name, Unit_Price, Stock_QTY,import_date) VALUES (default,'%s', %f, %d,default)", proName, proPrice, proQTY);
                    Matcher matcher = Pattern.compile("\\(default,\\s*'([^']*)',\\s*([^,]+),\\s*(\\d+),default\\)").matcher(insert);
                    if (matcher.find()) {
                        String insertProductName = matcher.group(1);
                        System.out.println(GREEN+BOLD+"\""+insertProductName+"\"" +" Has Been Inserted Successfully "+RESET);
                    }
                }
                pendingInserts.clear(); // Clear pending inserts after execution
            } else if (choice.equalsIgnoreCase("uu")) {
                if(pendingUpdates.isEmpty()){
                    System.out.println(RED+"No DATA"+RESET);
                }
                for (String update : pendingUpdates) {
                    statement.executeUpdate(update);
//                    String updateSQL = String.format("UPDATE product_tb SET Name = '%s', Unit_Price = %f, Stock_QTY = %d WHERE ID = %d", upName, upPrice, upQTY, productId);
                    Matcher matcher = Pattern.compile("SET\\s+Name\\s*=\\s*'([^']*)',\\s*Unit_Price\\s*=\\s*([^,]+),\\s*Stock_QTY\\s*=\\s*(\\d+)").matcher(update);
                    if(matcher.find()){
                        String updateProductName = matcher.group(1);
                        System.out.println(GREEN+BOLD+"\""+updateProductName+"\"" +" Has Been Updated Successfully "+RESET);
                    }
                }
                pendingUpdates.clear(); // Clear pending updates after execution
            } else if (choice.equalsIgnoreCase("b")) {
                System.out.println(BOLD+"Going back..."+RESET);
            }else {
                System.out.println(RED+"Please Only Input UI,UU or B  "+RESET);
            }
        } catch (SQLException e) {
            System.out.println(RED +BOLD+ "Error saving changes: " + RESET + e.getMessage());
        }
    }
    private static void unsavedChange() {
        DecimalFormat decimalFormat = new DecimalFormat("$ #,##0.00");
        String option = validateInput(BOLD+"Please choose"+RESET+BLUE+BOLD+" 'uni'"+RESET+BOLD+" to view unsaved insertion products or"+RESET+BLUE+BOLD+" 'unu'"+RESET+BOLD+" to view unsaved updated products or "+RESET+BLUE+BOLD+"'b'"+RESET+" to go back : "+RESET,"^[Uu Nn Ii Bb]+$",RED+BLUE+"Please Input Only UNI or UNU or B ! Please try again"+RESET);

        Table table = new Table(4, BorderStyle.UNICODE_BOX_DOUBLE_BORDER_WIDE, ShownBorders.ALL);
        table.setColumnWidth(0, 14, 20);
        table.setColumnWidth(1, 14, 20);
        table.setColumnWidth(2, 14, 20);
        table.setColumnWidth(3, 14, 20);

        if (option.equalsIgnoreCase("uni")) {
            table.addCell(BLUE+BOLD+"Unsaved Insertion Product"+RESET, cellStyle, 5);
            table.addCell(GREEN+BOLD+"Name"+RESET, cellStyle);
            table.addCell(CYAN+BOLD+"Unit Price"+RESET, cellStyle);
            table.addCell(MAGENTA+BOLD+"QTY"+RESET, cellStyle);
            table.addCell(BLUE+BOLD+"Import Date"+RESET);
            if(pendingInserts.isEmpty()){
                table.addCell(RED+BOLD+"No DATA"+RESET, cellStyle,4);
            }
            for (String unsavedInsertion : pendingInserts) {
//                String insertSQL = String.format("INSERT INTO product_tb (id,Name, Unit_Price, Stock_QTY,import_date) VALUES (default,'%s', %f, %d,default)", proName, proPrice, proQTY);
                Matcher matcher = Pattern.compile("\\(default,\\s*'([^']*)',\\s*([^,]+),\\s*(\\d+),default\\)").matcher(unsavedInsertion);
                if (matcher.find()) {
                    String proName = matcher.group(1);
                    double proPrice = Double.parseDouble(matcher.group(2));
                    int proQTY = Integer.parseInt(matcher.group(3));
                    LocalDate importDate = LocalDate.now();

                    // Adding extracted data to the table
                    table.addCell(GREEN+BOLD+proName+RESET, cellStyle);
                    table.addCell(CYAN+BOLD+decimalFormat.format(proPrice)+RESET, cellStyle);
                    table.addCell(MAGENTA+BOLD+proQTY+RESET, cellStyle);
                    table.addCell(BLUE+BOLD+importDate+RESET, cellStyle);
                }
            }
        } else if (option.equalsIgnoreCase("unu")) {
            table.addCell(BLUE+BOLD+"Unsaved Updated Product"+RESET, cellStyle, 5);
            table.addCell(GREEN+BOLD+"Name"+RESET, cellStyle);
            table.addCell(CYAN+BOLD+"Unit Price"+RESET, cellStyle);
            table.addCell(MAGENTA+BOLD+"QTY"+RESET, cellStyle);
            table.addCell(BLUE+BOLD+"Import Date"+RESET);
            if(pendingUpdates.isEmpty()){
                table.addCell(RED+BOLD+"No DATA"+RESET, cellStyle,4);
            }
            for (String unsavedUpdated : pendingUpdates) {
                Matcher matcher = Pattern.compile("SET\\s+Name\\s*=\\s*'([^']*)',\\s*Unit_Price\\s*=\\s*([^,]+),\\s*Stock_QTY\\s*=\\s*(\\d+)").matcher(unsavedUpdated);
                if (matcher.find()) {
                    String upName = matcher.group(1);
                    double upPrice = Double.parseDouble(matcher.group(2));
                    int upQTY = Integer.parseInt(matcher.group(3));
                    LocalDate importDate = LocalDate.now();

                    // Adding extracted data to the table
                    table.addCell(GREEN+BOLD+upName+RESET, cellStyle);
                    table.addCell(CYAN+BOLD+decimalFormat.format(upPrice)+RESET, cellStyle);
                    table.addCell(MAGENTA+BOLD+upQTY+RESET, cellStyle);
                    table.addCell(BLUE+BOLD+importDate+RESET, cellStyle);
                }
            }
        }else if(option.equalsIgnoreCase("B")) {
            System.out.println(BOLD+"Going back..."+RESET);
            return; // Exit method if option is invalid
        }else {
            System.out.println(RED+"Please input Only UNI , UNU or B !!"+RESET);
            return;
        }
        System.out.println(table.render());
        press();
    }
    private static void getData(Table table, ResultSet resultSet) throws SQLException {
        table.setColumnWidth(0,10,15);
        table.setColumnWidth(1,10,15);
        table.setColumnWidth(2,10,15);
        table.setColumnWidth(3,10,15);
        table.setColumnWidth(4,10,15);

        table.addCell(BLUE+BOLD+"Product List"+RESET, cellStyle, 5);
        table.addCell(YELLOW+BOLD+"ID"+RESET, cellStyle);
        table.addCell(GREEN+BOLD+"Name"+RESET, cellStyle);
        table.addCell(CYAN+BOLD+"Unit Price"+RESET, cellStyle);
        table.addCell(MAGENTA+BOLD+"QTY"+RESET, cellStyle);
        table.addCell(BLUE+BOLD+"Import Date"+RESET);

        while (resultSet.next()) {
            table.addCell(YELLOW+BOLD+resultSet.getString("ID")+RESET, cellStyle);
            table.addCell(GREEN+BOLD+resultSet.getString("Name")+RESET, cellStyle);
            table.addCell(CYAN+BOLD+"$"+resultSet.getString("Unit_Price")+RESET, cellStyle);
            table.addCell(MAGENTA+BOLD+resultSet.getString("Stock_QTY")+RESET, cellStyle);
            table.addCell(BLUE+BOLD+resultSet.getString("Import_Date")+RESET, cellStyle);
        }
    }
    private static void backupTable() {
        try {
            // Generate a unique filename for the backup using timestamp
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String backupFileName = "backup_" + dateFormat.format(new Date()) + ".sql";
            String backupFilePath = "D:\\backup\\" + backupFileName;

            // Execute pg_dump command to create a backup of the database
            ProcessBuilder pb = new ProcessBuilder("C:\\Program Files\\PostgreSQL\\16\\data\\bin\\pg_dump", "-U", "postgres", "-d", "postgres", "-f", backupFilePath);
            //change password before backup
            pb.environment().put("PGPASSWORD", "092367169");
            pb.redirectErrorStream(true);

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            // Wait for the command to finish executing
            int exitCode = process.waitFor();
            //ber sin exitCode = 0 ban ney tha process successfully if exitCode > 0  ban ney tha mean error
            if (exitCode == 0) {
                System.out.println("Backup created successfully: " + backupFileName);

                // Insert the backup file path into the database
                PreparedStatement insertBackupStatement = connection.prepareStatement("INSERT INTO save_back_up_list_tb (List_Of_Table) VALUES (?)");
                insertBackupStatement.setString(1, backupFilePath);
                insertBackupStatement.executeUpdate();
                System.out.println(GREEN+BOLD+"Backup file path inserted into database."+RESET);
            } else {
                System.out.println(RED+BOLD+"Backup creation failed."+RESET);
            }

        } catch (IOException | InterruptedException | SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    private static void restoreTable() {
        try {
            //yk file path pi database
            ResultSet backupResultSet = statement.executeQuery("SELECT ID, List_Of_Table FROM save_back_up_list_tb");

            // Create a table to display the list of backup files
            Table table = new Table(2, BorderStyle.UNICODE_BOX_DOUBLE_BORDER_WIDE, ShownBorders.ALL);
            table.setColumnWidth(0, 10, 15);
            table.setColumnWidth(1, 50, 70);
            table.addCell(YELLOW+BOLD+"ID"+RESET, cellStyle);
            table.addCell("Backup File Path", cellStyle);

            // add file path del mean pi database hz add jol table dermbey display
            while (backupResultSet.next()) {
                int id = backupResultSet.getInt("ID");
                String filePath = backupResultSet.getString("List_Of_Table");
                table.addCell(String.valueOf(id), cellStyle);
                table.addCell(filePath, cellStyle);
            }

            // Display the table
            System.out.println(table.render());

            // For User Input to select which back up file to restore
            int choice = Integer.parseInt(validateInput(BOLD+"Input ID of back up file to restore : "+RESET,"^[1-9]+$",RED + BOLD+"Invalid product quality...! Please Try Again" + RESET));
            String confirmChoice = validateInput(BLUE+BOLD+"Are you sure you want to restore this back up file ? (Y/N) : "+RESET,"^[Yy Nn]+$",RED+BOLD+"Please Input Only Y or N ! Please try again..."+RESET);

            //oy user confirm tha jong backup ah ng men?
            if(confirmChoice.equalsIgnoreCase("y")){
                // pel confirm hz yk path file pi database tam ID
                ResultSet selectedBackupResultSet = statement.executeQuery("SELECT List_Of_Table FROM save_back_up_list_tb WHERE ID = " + choice);
                if (selectedBackupResultSet.next()) {
                    String backupFilePath = selectedBackupResultSet.getString("List_Of_Table");

                    // Execute psql command to restore the database from the selected backup file
                    ProcessBuilder pb = new ProcessBuilder("C:\\Program Files\\PostgreSQL\\16\\data\\bin\\psql", "-U", "postgres", "-d", "postgres", "-f", backupFilePath);
                    //change password before restore
                    pb.environment().put("PGPASSWORD", "092367169");

                    pb.redirectErrorStream(true);
                    Process process = pb.start();

                    //read line robos process
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("ERROR")) {
                            System.out.println(RED+BOLD+"Database restoration failed."+RESET);
                            break;
                        } else if (line.contains("successfully")) {
                            System.out.println("Database restored successfully from: "+ backupFilePath);
                            break;
                        }
                    }
                    //jam command execute oy job
                    int exitCode = process.waitFor();
                    //ber exitCode = 0 ban ney tha successfully
                    if (exitCode == 0) {
                        System.out.println("Database restored successfully from: " + backupFilePath);
                    } else {
                        System.out.println(RED+BOLD+"Database restoration failed."+RESET);
                    }
                }else {
                    System.out.println(RED+BOLD+"Invalid ID."+RESET);
                }
            } else if (confirmChoice.equalsIgnoreCase("n")) {
                System.out.println(BOLD+"Going Back..."+RESET);
            }
        } catch (SQLException | IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }
    public static String validateInput(String input, String pattern, String Error) {
        boolean check;
        Scanner scanner = new Scanner(System.in);
        String userInput;
        do {
            System.out.print(input);
            userInput = scanner.nextLine();
            check = Pattern.matches(pattern, userInput);
            if (!check)
                System.out.println(Error);
        } while (!check);
        return userInput;
    }
    private static void press(){
        Scanner s = new Scanner(System.in);
        System.out.println(BOLD+"press "+RESET+BLUE+BOLD+"\"Enter\""+RESET+BOLD+" to continue..."+RESET);
        s.nextLine();
    }
}