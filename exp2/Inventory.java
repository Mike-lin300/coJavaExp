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

        // for (Item item : items) {
        //     System.out.println(item);
        // }

        reader.close();

        List<String[]> newItems = new ArrayList<>();    // A
        List<String[]> arrivals = new ArrayList<>();    // R
        List<String[]> orders = new ArrayList<>();      // O
        List<String[]> deletes = new ArrayList<>();     // D

        BufferedReader reader2 = new BufferedReader(new FileReader("Transactions.txt"));
        while ((line = reader2.readLine()) != null) {
            if (line.trim().isEmpty()) continue;
            String[] parts = line.split("\t");
            // 分拣
            switch (parts[0]) {
                case "A":
                    newItems.add(parts);
                    break;
                case "R":
                    arrivals.add(parts);
                    break;
                case "O":
                    orders.add(parts);
                    break;
                case "D":
                    deletes.add(parts);
                    break;
            }
        }
        reader2.close();

        // System.out.println("===== 库存读取情况 =====");
        // System.out.println("库存货物总数: " + items.size());
        // System.out.println("===== 事务分类情况 =====");
        // System.out.println("A (新增货物) 条数: " + newItems.size());
        // System.out.println("R (到货) 条数: " + arrivals.size());
        // System.out.println("O (发货) 条数: " + orders.size());
        // System.out.println("D (删除) 条数: " + deletes.size());

        // 处理 A 事件
        for (String[] parts : newItems) {
            String itemNumber = parts[1];
            String supplier = parts[2];
            String description = parts[3];
            items.add(new Item(itemNumber, 0, supplier, description));
        }

        // 处理 R 事件
        for (String[] parts : arrivals) {
            String itemNumber = parts[1];
            int _quantity = Integer.parseInt(parts[2]);
            for (Item item : items) {
                if (item.itemNumber.equals(itemNumber)) {
                    item.quantity += _quantity;
                    break;
                }
            }
        }

        System.out.println("A/R 之后");
        for (Item item : items) {
            System.out.println(item);
        }
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
