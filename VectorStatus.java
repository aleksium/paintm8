public class VectorStatus
{
    public int[] c;
    private static final int VECTOR_SIZE = 5;

    public VectorStatus(final int x1, final int y1, final int x2, final int y2, final int hue) {
        common();
        setCoordinates(x1,y1,x2,y2, hue);
    }
    public VectorStatus() {
        common();
    }

    private void common() {
        c = new int[VECTOR_SIZE];
    }

    public void setCoordinates(final int x1, final int y1, final int x2, final int y2, final int hue) {
        c[0] = x1;
        c[1] = y1;
        c[2] = x2;
        c[3] = y2;
        c[4] = hue;
    }
};