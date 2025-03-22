package p.m.paintm8;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PaintAccumulator {

    private static final int MAX_ACCUMULATED_LINES = 4096;

    private final Queue<Line> currentLines = new ConcurrentLinkedQueue<>();

    public PaintAccumulator() {
    }

    public void addLines(Collection<Line> lines) {
        if (lines.size() > MAX_ACCUMULATED_LINES) {
            return;
        }
        var overlap = currentLines.size() + lines.size() - MAX_ACCUMULATED_LINES;
        for (int i = 0; i < overlap; i++) {
            currentLines.poll();
        }
        currentLines.addAll(lines);
    }

    public Collection<Line> currentLines() {
        return currentLines;
    }

    public void clearLines() {
        currentLines.clear();
    }
}
