package point;

public class Point3D extends Point2D {
    private int z;

    public Point3D(int x, int y, int z) {
        super(x, y);
        this.z = z;
    }

    public Point3D(Point2D p, int z) {
        super(p.x, p.y);
        this.z = z;
    }

    public void offset(int a, int b, int c) {
        super.offset(a, b);
        this.z += c;
    }

    public double distance(Point3D p) {
        return Math.sqrt(Math.pow(this.x - p.x, 2) + Math.pow(this.y - p.y, 2) + Math.pow(this.z - p.z, 2));
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }

    public static void main(String[] args) {
        Point2D p2d1 = new Point2D(1, 2);
        Point2D p2d2 = new Point2D(4, 6);
        System.out.println("Distance between " + p2d1 + " and " + p2d2 + " is: " + p2d1.distance(p2d2));

        Point3D p3d1 = new Point3D(1, 2, 3);
        Point3D p3d2 = new Point3D(4, 6, 8);
        System.out.println("Distance between " + p3d1 + " and " + p3d2 + " is: " + p3d1.distance(p3d2));
    }
}
