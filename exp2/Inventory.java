import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;

public class Inventory {
    public static void main(String[] args) throws IOException {
        List<Item> items = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new FileReader("Inventory.txt"));

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) continue;

            String[] parts = line.split("\t");
            Item item = new Item(
                parts[0],
                Integer.parseInt(parts[1]),
                parts[2],
                parts[3]
            );
            items.add(item);
        }

        for (Item item : items) {
            System.out.println(item);
        }

        reader.close();
    }
}

class Item {
    String itemNumber;
    int quantity;
    String supplier;
    String description;

    Item(String itemNumber, int quantity, String supplier, String description) {
        this.itemNumber = itemNumber;
        this.quantity = quantity;
        this.supplier = supplier;
        this.description = description;
    }

    @Override
    public String toString() {
        return itemNumber + "\t" + quantity + "\t" + supplier + "\t" + description;
    }
}
