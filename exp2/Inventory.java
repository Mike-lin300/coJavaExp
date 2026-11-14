import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;

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
            Item item = findItem(items, itemNumber);
            if (item != null) {
                item.quantity += _quantity;
            }
        }

        List<String[]> shipments = new ArrayList<>();
        List<String[]> errors = new ArrayList<>();

        orders.sort((o1,o2) -> {
            String item1 = o1[2];
            String item2 = o2[2];
            int res = item1.compareTo(item2);
            if (res != 0) return res;
            int q1 = Integer.parseInt(o1[3]);
            int q2 = Integer.parseInt(o2[3]);
            return Integer.compare(q1, q2);
        });

        // 处理 O 事件
        for (String[] parts : orders) {
            String customerNumber = parts[1];
            String itemNumber = parts[2];
            int amount = Integer.parseInt(parts[3]);
            Item target = findItem(items, itemNumber);
            // 库存不够或没有该货物，记录到错误列表中
            if (target == null || target.quantity < amount) {
                errors.add(new String[]{customerNumber, itemNumber, String.valueOf(amount)});
            } else {
                target.quantity -= amount;
                // 记录发货信息，检查是否需要合并
                boolean isMerged = false;
                for (String[] ship : shipments) {
                    if (ship[0].equals(customerNumber) && ship[1].equals(itemNumber)) {
                        int oldAmount = Integer.parseInt(ship[2]);
                        ship[2] = String.valueOf(oldAmount + amount);
                        isMerged = true;
                        break;
                    }
                }
                if (!isMerged) {
                    shipments.add(new String[]{customerNumber, itemNumber, String.valueOf(amount)});
                }
            }
        }
        
        // 处理 D 事件
        for (String[] parts : deletes) {
            String itemNumber = parts[1];
            Item target = findItem(items, itemNumber);

            if (target != null) {
                if (target.quantity != 0) {
                    errors.add(new String[]{"0", itemNumber, String.valueOf(target.quantity)});
                }
                items.remove(target);
            }
        }
        
        PrintWriter writer = new PrintWriter("Shipping.txt");
        for (String[] s : shipments) {
            writer.println(s[0] + "\t" + s[1] + "\t" + s[2]);
        }
        writer.close();

        PrintWriter writer2 = new PrintWriter("Errors.txt");
        for (String[] s : errors) {
            writer2.println(s[0] + "\t" + s[1] + "\t" + s[2]);
        }
        writer2.close();

        items.sort((i1, i2) -> i1.itemNumber.compareTo(i2.itemNumber));
        PrintWriter writer3 = new PrintWriter("NewInventory.txt");
        for (Item item : items) {
            writer3.println(item);
        }
        writer3.close();
    }
    
    private static Item findItem(List<Item> items, String itemNumber) {
        for (Item item : items) {
            if (item.itemNumber.equals(itemNumber)) {
                return item;
            }
        }
        return null;
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
