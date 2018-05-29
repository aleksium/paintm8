
package p.m.paintm8;

import java.util.LinkedList;
import java.util.List;

public class PaintAccumulator {

    private static final int MAX_ACCUMULATED_LINES = 4096;

    private final LinkedList<Line> currentLines = new LinkedList<>();

    public PaintAccumulator() {
    }

    public void addLines(List<Line> lines) {
        if (lines.size() > MAX_ACCUMULATED_LINES) {
            return;
        }
        var overlap = currentLines.size() + lines.size() - MAX_ACCUMULATED_LINES;
        for (int i = 0; i < overlap; i++) {
            currentLines.removeFirst();
        }
        currentLines.addAll(lines);
    }

    public LinkedList<Line> currentLines() {
        return currentLines;
    }

    public void clearLines() {
        currentLines.clear();
    }
}
