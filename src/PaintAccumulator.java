import java.util.LinkedList;

public class PaintAccumulator {
	private static final int MAX_ACCUMULATED_LINES = 4096;
	private LinkedList<VectorStatus> currentLines_;

	public PaintAccumulator() {
		currentLines_ = new LinkedList<VectorStatus>();
	}

	public void addLine(VectorStatus line) {
		if (MAX_ACCUMULATED_LINES <= currentLines_.size()) {
			currentLines_.pollLast();
		}
		currentLines_.addFirst(line);
	}

	public LinkedList<VectorStatus> currentLines() {
		return currentLines_;
	}

	public void clearLines() {
		currentLines_.clear();
	}
}
