package cz.jkuchar.rcba.rules;

/**
 * @author Jaroslav Kuchar - https://github.com/jaroslav-kuchar
 */
public class Tuple implements Comparable<Tuple>{
    private String left;
    private String right;
    // represents frequency of items in transactions - increase efficiency and reduces memory requirements
    private int count;

    public Tuple(String left, String right) {
        this.left = left;
        this.right = right;
        this.count = 1;
    }

    public Tuple(String left, String right, int count) {
        this.left = left;
        this.right = right;
        this.count = count;
    }

    public String getLeft() {
        return left;
    }

    public String getRight() {
        return right;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Tuple getCopy() {
        return new Tuple(new String(left), new String(right));
    }

    @Override
    public java.lang.String toString() {
        return left + "(" + right + ")";
//        return left + "(" + right + ")-"+count+"-";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tuple tuple = (Tuple) o;

        if (left != null ? !left.equals(tuple.left) : tuple.left != null) return false;
        return right != null ? right.equals(tuple.right) : tuple.right == null;

    }

    @Override
    public int hashCode() {
        int result = left != null ? left.hashCode() : 0;
        result = 31 * result + (right != null ? right.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(Tuple o) {
        return this.toString().compareTo(o.toString());
    }
}
