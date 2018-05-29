package p.m.paintm8;

public class Line {

    private static final int VECTOR_SIZE = 4;

    public int[] c = new int[VECTOR_SIZE];

    public Line(int x1, int y1, int x2, int y2) {
        c[0] = x1;
        c[1] = y1;
        c[2] = x2;
        c[3] = y2;
    }
};
