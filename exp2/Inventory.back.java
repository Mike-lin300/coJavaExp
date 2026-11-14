// import java.io.BufferedReader;
// import java.io.BufferedWriter;
// import java.io.IOException;
// import java.nio.charset.StandardCharsets;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.nio.file.Paths;
// import java.util.ArrayList;
// import java.util.Collections;
// import java.util.Comparator;
// import java.util.List;

// public class Inventory {
//     public static void main(String[] args) {
//         Path dataDirectory = Paths.get(args.length == 0 ? "." : args[0]);

//         try {
//             InventoryProcessor processor = new InventoryProcessor(dataDirectory);
//             processor.run();
//         } catch (IOException e) {
//             System.err.println("File processing failed: " + e.getMessage());
//             System.exit(1);
//         } catch (IllegalArgumentException e) {
//             System.err.println("Invalid input: " + e.getMessage());
//             System.exit(1);
//         }
//     }
// }

// class InventoryProcessor {
//     private static final String INVENTORY_FILE = "Inventory.txt";
//     private static final String TRANSACTIONS_FILE = "Transactions.txt";
//     private static final String SHIPPING_FILE = "Shipping.txt";
//     private static final String ERRORS_FILE = "Errors.txt";
//     private static final String NEW_INVENTORY_FILE = "NewInventory.txt";

//     private final Path dataDirectory;
//     private final List<Item> items = new ArrayList<Item>();

//     InventoryProcessor(Path dataDirectory) {
//         this.dataDirectory = dataDirectory.normalize();
//     }

//     void run() throws IOException {
//         if (!Files.isDirectory(dataDirectory)) {
//             throw new IOException("Data directory does not exist: " + dataDirectory);
//         }

//         readInventory();
//         TransactionBatch batch = readTransactions();

//         addNewItems(batch.newItems);
//         receiveArrivals(batch.arrivals);

//         List<Shipment> shipments = new ArrayList<Shipment>();
//         List<ErrorRecord> errors = new ArrayList<ErrorRecord>();
//         processOrders(batch.orders, shipments, errors);
//         processDeletes(batch.deletes, errors);

//         writeShipping(shipments);
//         writeErrors(errors);
//         writeInventory();

//         System.out.println("Inventory processing completed.");
//         System.out.println("Inventory items: " + items.size());
//         System.out.println("Shipping records: " + shipments.size());
//         System.out.println("Error records: " + errors.size());
//         System.out.println("Output directory: " + dataDirectory.toAbsolutePath());
//     }

//     private void readInventory() throws IOException {
//         Path file = dataDirectory.resolve(INVENTORY_FILE);
//         BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);

//         try {
//             String line;
//             int lineNumber = 0;

//             while ((line = reader.readLine()) != null) {
//                 lineNumber++;
//                 line = removeBom(line);

//                 if (line.trim().isEmpty()) {
//                     continue;
//                 }

//                 String[] fields = line.split("\t", 4);
//                 if (fields.length < 4) {
//                     throw inputError(INVENTORY_FILE, lineNumber,
//                             "expected 4 tab-separated fields");
//                 }

//                 String itemNumber = fields[0].trim();
//                 String supplier = fields[2].trim();
//                 String description = fields[3].trim();
//                 int quantity = parseNonNegativeInt(fields[1], INVENTORY_FILE, lineNumber);

//                 if (itemNumber.isEmpty() || supplier.isEmpty()) {
//                     throw inputError(INVENTORY_FILE, lineNumber,
//                             "item number and supplier must not be empty");
//                 }
//                 if (findItem(itemNumber) != null) {
//                     throw inputError(INVENTORY_FILE, lineNumber,
//                             "duplicate item number " + itemNumber);
//                 }

//                 items.add(new Item(itemNumber, quantity, supplier, description));
//             }
//         } finally {
//             reader.close();
//         }
//     }

//     private TransactionBatch readTransactions() throws IOException {
//         Path file = dataDirectory.resolve(TRANSACTIONS_FILE);
//         BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
//         TransactionBatch batch = new TransactionBatch();

//         try {
//             String line;
//             int lineNumber = 0;

//             while ((line = reader.readLine()) != null) {
//                 lineNumber++;
//                 line = removeBom(line);

//                 if (line.trim().isEmpty()) {
//                     continue;
//                 }

//                 String[] fields = line.split("\t", 4);
//                 String type = fields[0].trim();
//                 int sequence = lineNumber;

//                 if ("A".equals(type)) {
//                     requireFieldCount(fields, 4, lineNumber);
//                     String itemNumber = fields[1].trim();
//                     String supplier = fields[2].trim();
//                     String description = fields[3].trim();

//                     if (itemNumber.isEmpty() || supplier.isEmpty()) {
//                         throw inputError(TRANSACTIONS_FILE, lineNumber,
//                                 "item number and supplier must not be empty");
//                     }
//                     batch.newItems.add(new NewItem(itemNumber, supplier, description));
//                 } else if ("R".equals(type)) {
//                     requireFieldCount(fields, 3, lineNumber);
//                     batch.arrivals.add(new Arrival(
//                             fields[1].trim(),
//                             parsePositiveInt(fields[2], TRANSACTIONS_FILE, lineNumber)));
//                 } else if ("O".equals(type)) {
//                     requireFieldCount(fields, 4, lineNumber);
//                     batch.orders.add(new Order(
//                             fields[1].trim(),
//                             fields[2].trim(),
//                             parsePositiveInt(fields[3], TRANSACTIONS_FILE, lineNumber),
//                             sequence));
//                 } else if ("D".equals(type)) {
//                     requireFieldCount(fields, 2, lineNumber);
//                     batch.deletes.add(new DeleteRequest(fields[1].trim()));
//                 } else {
//                     throw inputError(TRANSACTIONS_FILE, lineNumber,
//                             "unknown transaction type " + type);
//                 }
//             }
//         } finally {
//             reader.close();
//         }

//         return batch;
//     }

//     private void addNewItems(List<NewItem> newItems) {
//         for (NewItem request : newItems) {
//             if (findItem(request.itemNumber) != null) {
//                 throw new IllegalArgumentException(
//                         "cannot add existing item " + request.itemNumber);
//             }
//             items.add(new Item(
//                     request.itemNumber, 0, request.supplier, request.description));
//         }
//     }

//     private void receiveArrivals(List<Arrival> arrivals) {
//         for (Arrival arrival : arrivals) {
//             Item item = findItem(arrival.itemNumber);
//             if (item == null) {
//                 throw new IllegalArgumentException(
//                         "arrival refers to unknown item " + arrival.itemNumber);
//             }
//             item.quantity += arrival.quantity;
//         }
//     }

//     private void processOrders(List<Order> orders,
//             List<Shipment> shipments,
//             List<ErrorRecord> errors) {
//         Collections.sort(orders, new Comparator<Order>() {
//             public int compare(Order first, Order second) {
//                 int byItem = first.itemNumber.compareTo(second.itemNumber);
//                 if (byItem != 0) {
//                     return byItem;
//                 }

//                 int byQuantity = Integer.compare(first.quantity, second.quantity);
//                 if (byQuantity != 0) {
//                     return byQuantity;
//                 }
//                 return Integer.compare(first.sequence, second.sequence);
//             }
//         });

//         for (Order order : orders) {
//             Item item = findItem(order.itemNumber);
//             if (item == null || item.quantity < order.quantity) {
//                 errors.add(new ErrorRecord(
//                         order.customerNumber, order.itemNumber, order.quantity));
//                 continue;
//             }

//             item.quantity -= order.quantity;
//             mergeShipment(shipments, order.customerNumber,
//                     order.itemNumber, order.quantity);
//         }
//     }

//     private void processDeletes(List<DeleteRequest> deletes,
//             List<ErrorRecord> errors) {
//         for (DeleteRequest request : deletes) {
//             Item item = findItem(request.itemNumber);
//             if (item == null) {
//                 continue;
//             }

//             if (item.quantity != 0) {
//                 errors.add(new ErrorRecord("0", item.itemNumber, item.quantity));
//             }
//             items.remove(item);
//         }
//     }

//     private void mergeShipment(List<Shipment> shipments,
//             String customerNumber,
//             String itemNumber,
//             int quantity) {
//         for (Shipment shipment : shipments) {
//             if (shipment.customerNumber.equals(customerNumber)
//                     && shipment.itemNumber.equals(itemNumber)) {
//                 shipment.quantity += quantity;
//                 return;
//             }
//         }

//         shipments.add(new Shipment(customerNumber, itemNumber, quantity));
//     }

//     private void writeShipping(List<Shipment> shipments) throws IOException {
//         Collections.sort(shipments, new Comparator<Shipment>() {
//             public int compare(Shipment first, Shipment second) {
//                 int byCustomer = first.customerNumber.compareTo(second.customerNumber);
//                 if (byCustomer != 0) {
//                     return byCustomer;
//                 }
//                 return first.itemNumber.compareTo(second.itemNumber);
//             }
//         });

//         Path file = dataDirectory.resolve(SHIPPING_FILE);
//         BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);

//         try {
//             for (Shipment shipment : shipments) {
//                 StringBuffer line = new StringBuffer();
//                 line.append(shipment.customerNumber).append('\t')
//                         .append(shipment.itemNumber).append('\t')
//                         .append(shipment.quantity);
//                 writer.write(line.toString());
//                 writer.newLine();
//             }
//         } finally {
//             writer.close();
//         }
//     }

//     private void writeErrors(List<ErrorRecord> errors) throws IOException {
//         Path file = dataDirectory.resolve(ERRORS_FILE);
//         BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);

//         try {
//             for (ErrorRecord error : errors) {
//                 StringBuffer line = new StringBuffer();
//                 line.append(error.customerNumber).append('\t')
//                         .append(error.itemNumber).append('\t')
//                         .append(error.quantity);
//                 writer.write(line.toString());
//                 writer.newLine();
//             }
//         } finally {
//             writer.close();
//         }
//     }

//     private void writeInventory() throws IOException {
//         Collections.sort(items, new Comparator<Item>() {
//             public int compare(Item first, Item second) {
//                 return first.itemNumber.compareTo(second.itemNumber);
//             }
//         });

//         Path file = dataDirectory.resolve(NEW_INVENTORY_FILE);
//         BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);

//         try {
//             for (Item item : items) {
//                 StringBuffer line = new StringBuffer();
//                 line.append(item.itemNumber).append('\t')
//                         .append(item.quantity).append('\t')
//                         .append(item.supplier).append('\t')
//                         .append(item.description);
//                 writer.write(line.toString());
//                 writer.newLine();
//             }
//         } finally {
//             writer.close();
//         }
//     }

//     private Item findItem(String itemNumber) {
//         for (Item item : items) {
//             if (item.itemNumber.equals(itemNumber)) {
//                 return item;
//             }
//         }
//         return null;
//     }

//     private void requireFieldCount(String[] fields, int expected, int lineNumber) {
//         if (fields.length != expected) {
//             throw inputError(TRANSACTIONS_FILE, lineNumber,
//                     "expected " + expected + " tab-separated fields");
//         }
//     }

//     private int parseNonNegativeInt(String value, String fileName, int lineNumber) {
//         int number = parseInteger(value, fileName, lineNumber);
//         if (number < 0) {
//             throw inputError(fileName, lineNumber, "quantity must not be negative");
//         }
//         return number;
//     }

//     private int parsePositiveInt(String value, String fileName, int lineNumber) {
//         int number = parseInteger(value, fileName, lineNumber);
//         if (number <= 0) {
//             throw inputError(fileName, lineNumber, "quantity must be positive");
//         }
//         return number;
//     }

//     private int parseInteger(String value, String fileName, int lineNumber) {
//         try {
//             return Integer.parseInt(value.trim());
//         } catch (NumberFormatException e) {
//             throw inputError(fileName, lineNumber, "quantity is not an integer: " + value);
//         }
//     }

//     private IllegalArgumentException inputError(
//             String fileName, int lineNumber, String message) {
//         return new IllegalArgumentException(
//                 fileName + " line " + lineNumber + ": " + message);
//     }

//     private String removeBom(String line) {
//         if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
//             return line.substring(1);
//         }
//         return line;
//     }
// }

// class Item {
//     final String itemNumber;
//     int quantity;
//     final String supplier;
//     final String description;

//     Item(String itemNumber, int quantity, String supplier, String description) {
//         this.itemNumber = itemNumber;
//         this.quantity = quantity;
//         this.supplier = supplier;
//         this.description = description;
//     }
// }

// class NewItem {
//     final String itemNumber;
//     final String supplier;
//     final String description;

//     NewItem(String itemNumber, String supplier, String description) {
//         this.itemNumber = itemNumber;
//         this.supplier = supplier;
//         this.description = description;
//     }
// }

// class Arrival {
//     final String itemNumber;
//     final int quantity;

//     Arrival(String itemNumber, int quantity) {
//         this.itemNumber = itemNumber;
//         this.quantity = quantity;
//     }
// }

// class Order {
//     final String customerNumber;
//     final String itemNumber;
//     final int quantity;
//     final int sequence;

//     Order(String customerNumber, String itemNumber, int quantity, int sequence) {
//         this.customerNumber = customerNumber;
//         this.itemNumber = itemNumber;
//         this.quantity = quantity;
//         this.sequence = sequence;
//     }
// }

// class DeleteRequest {
//     final String itemNumber;

//     DeleteRequest(String itemNumber) {
//         this.itemNumber = itemNumber;
//     }
// }

// class Shipment {
//     final String customerNumber;
//     final String itemNumber;
//     int quantity;

//     Shipment(String customerNumber, String itemNumber, int quantity) {
//         this.customerNumber = customerNumber;
//         this.itemNumber = itemNumber;
//         this.quantity = quantity;
//     }
// }

// class ErrorRecord {
//     final String customerNumber;
//     final String itemNumber;
//     final int quantity;

//     ErrorRecord(String customerNumber, String itemNumber, int quantity) {
//         this.customerNumber = customerNumber;
//         this.itemNumber = itemNumber;
//         this.quantity = quantity;
//     }
// }

// class TransactionBatch {
//     final List<NewItem> newItems = new ArrayList<NewItem>();
//     final List<Arrival> arrivals = new ArrayList<Arrival>();
//     final List<Order> orders = new ArrayList<Order>();
//     final List<DeleteRequest> deletes = new ArrayList<DeleteRequest>();
// }
