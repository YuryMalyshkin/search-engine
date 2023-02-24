package searchengine.services;


public class Pairs implements Comparable<Pairs> {
    Integer x;
    Integer y;

    public Pairs(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }

    public Pairs() {
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    @Override
    public int compareTo(Pairs o) {
        return y.compareTo(o.getY());
    }
}
