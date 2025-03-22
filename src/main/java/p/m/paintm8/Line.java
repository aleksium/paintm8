package p.m.paintm8;

public class Line {

    public static final int NUM_OF_INTEGERS = 5;

    public int[] c = new int[NUM_OF_INTEGERS];

    public Line(int x1, int y1, int x2, int y2, int i) {
        c[0] = x1;
        c[1] = y1;
        c[2] = x2;
        c[3] = y2;
        c[4] = i;
    }
};
