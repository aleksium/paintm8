package p.m.paintm8;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class MsgCodec {

    private static final int HEADER_SIZE = 8;
    private static final int BYTES_PER_INTEGER = 4;
    private static final int LINE_SIZE = Line.NUM_OF_INTEGERS * BYTES_PER_INTEGER;

    private ByteBuffer bb = ByteBuffer.allocate(MsgCodec.MAX_MESSAGE_SIZE);
    private MessageType type;
    private State state;

    private enum State {
        DECODE(1), ENCODE(2), UNDEFINED(3);

        private int value;
        private static final Map<Integer, State> map = new HashMap<>();

        private State(int value) {
            this.value = value;
        }

        static {
            for (State state : State.values()) {
                map.put(state.value, state);
            }
        }

        public static State valueOf(int state) {
            return (State) map.get(state);
        }

        public int getValue() {
            return value;
        }
    }

    public enum MessageType {
        STATUS_SERVER(1), STATUS_CLIENT(2), LINE(3), INVALID(4);

        private int value;
        private static final Map<Integer, MessageType> map = new HashMap<>();

        private MessageType(int value) {
            this.value = value;
        }

        static {
            for (MessageType type : MessageType.values()) {
                map.put(type.value, type);
            }
        }

        public static MessageType valueOf(int type) {
            return (MessageType) map.get(type);
        }

        public int getValue() {
            return value;
        }
    }

    public static final int MAX_MESSAGE_SIZE = 500;
    public static final int MAX_LINES = 20;
    public static final int MAX_NAME_SIZE = 20;

    public MsgCodec() {
        type = MessageType.INVALID;
        state = State.UNDEFINED;
    }

    public MessageType mountDecodeBuffer(DatagramPacket receivePacket) {
        bb.clear();
        bb.put(receivePacket.getData(), 0, receivePacket.getLength());
        bb.flip();
        identify();
        state = State.DECODE;
        return type;
    }

    public MessageType getType() {
        return type;
    }

    public void mountEncodeBuffer(ByteBuffer bb) {
        this.bb = bb;
        state = State.ENCODE;
    }

    private void identify() {
        type = MessageType.INVALID;
        final int actualSize = bb.limit();

        if (actualSize >= HEADER_SIZE) {
            final MessageType reportedType = MessageType.valueOf(bb.getInt());
            final int reportedSize = bb.getInt();

            if (reportedSize != actualSize) {
                return;
            }
            switch (reportedType) {
                case STATUS_CLIENT -> {
                    if ((bb.limit() > HEADER_SIZE) && (bb.limit() <= MAX_NAME_SIZE)) {
                        type = MessageType.STATUS_CLIENT;
                    }
                }
                case LINE -> {
                    if (reportedSize % BYTES_PER_INTEGER == 0) {
                        type = MessageType.LINE;
                    }
                }
                case STATUS_SERVER, INVALID ->
                    type = reportedType;
            }
        }
    }

    public boolean encodeServerStatusMsg(String text) {
        if (text != null) {
            int textSize = text.getBytes().length;
            int msgSize = HEADER_SIZE + textSize;
            bb.clear();
            bb.putInt(MessageType.STATUS_SERVER.getValue());
            bb.putInt(msgSize);

            bb.put(text.getBytes(), 0, textSize);
            bb.flip();
            state = State.UNDEFINED;
            return true;
        }
        return false;
    }

    public String decodeServerStatusMsg() {
        String text = null;
        if (type == MessageType.STATUS_SERVER) {
            if (bb.position() < bb.limit()) {
                byte[] bytearr = new byte[bb.remaining()];
                bb.get(bytearr);
                text = new String(bytearr);
            }
        }
        return text;
    }

    public boolean encodeClientStatusMsg(String text) {
        if (text != null) {
            int textSize = text.getBytes().length;
            int msgSize = HEADER_SIZE + textSize;
            bb.clear();
            bb.putInt(MessageType.STATUS_CLIENT.getValue());
            bb.putInt(msgSize);
            if (bb.capacity() > textSize) {
                bb.put(text.getBytes(), 0, textSize);
                bb.flip();
                state = State.UNDEFINED;
                return true;
            }
        }
        return false;
    }

    public String decodeClientStatusMsg() {
        String text = null;
        if (type == MessageType.STATUS_CLIENT) {
            if (bb.position() < bb.limit()) {
                byte[] bytearr = new byte[bb.remaining()];
                bb.get(bytearr);
                text = new String(bytearr);
            }
        }
        return text;
    }

    public int encodeLines(List<Line> lines, int offset, int numLines) {
        bb.clear();
        int numLinesEncoded = 0;
        if (numLines > MAX_LINES) {
            numLines = MAX_LINES;
        }

        int msgSize = HEADER_SIZE + (numLines * LINE_SIZE);
        if (msgSize > bb.capacity()) {
            state = State.UNDEFINED;
            return numLinesEncoded;
        }

        bb.putInt(MessageType.LINE.getValue());
        bb.putInt(msgSize);

        if (state == State.ENCODE) {
            for (int i = offset; i < numLines + offset; ++i) {
                Line vs = lines.get(i);
                for (int j = 0; j < Line.NUM_OF_INTEGERS; ++j) {
                    bb.putInt(vs.c[j]);
                }
                ++numLinesEncoded;
            }
        }
        state = State.UNDEFINED;

        bb.flip();
        return numLinesEncoded;
    }

    public List<Line> decodeLines() {
        List<Line> lines = new ArrayList<>();

        if (type == MessageType.LINE) {
            int endPos = bb.limit() - LINE_SIZE;
            while (bb.position() <= endPos) {
                lines.add(new Line(bb.getInt(), bb.getInt(), bb.getInt(), bb.getInt(), bb.getInt()));
            }
        }
        state = State.UNDEFINED;
        return lines;
    }
}
