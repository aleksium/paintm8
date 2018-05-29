import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;

public class MsgCodec {
  private ByteBuffer bb_;
  private MessageType type_;
  private State state_;
  private final int HEADER_SIZE = 8;
  private final int LINE_SIZE = 20;

  // Thanks to Bo Andersen for showing how to define proper enums:
  // https://codingexplained.com/coding/java/enum-to-integer-and-integer-to-enum
  private enum State {
	  DECODE(1),
	  ENCODE(2),
	  UNDEFINED(3);

	  private int value;
	  private static Map<Integer, State> map = new HashMap<>();

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
	  STATUS_SERVER(1),
	  STATUS_CLIENT(2),
	  LINE(3),
	  CLEANUP(4),
	  TEXT(5),
	  INVALID(6);
	  private int value;
	  private static Map<Integer, MessageType> map = new HashMap<>();

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
    type_ = MessageType.INVALID;
    state_ = State.UNDEFINED;
  }

  public MessageType mountDecodeBuffer(ByteBuffer bb) {
    this.bb_ = bb;
    this.bb_.flip();
    identify();
    state_ = State.DECODE;
    return  type_;
  }
  
  public MessageType getType() {
    return type_;
  }

  public void mountEncodeBuffer(ByteBuffer bb) {
    this.bb_ = bb;
    state_ = State.ENCODE;
  }

  private void identify() {
    type_ = MessageType.INVALID;
    final int actualSize = bb_.limit();

    if (actualSize >= HEADER_SIZE) {	
      final MessageType reportedType = MessageType.valueOf(bb_.getInt());
      final int reportedSize = bb_.getInt();

      if (reportedSize != actualSize) {
        return;
      }
      switch(reportedType) {
      case STATUS_CLIENT:
    	  if ((bb_.limit() > HEADER_SIZE) && (bb_.limit() <= MAX_NAME_SIZE)) {
    		  type_ = MessageType.STATUS_CLIENT;
    	  }
    	  break;
      case LINE:
    	  if (reportedSize % 4 == 0) {
    		  type_ = MessageType.LINE;
    	  }
    	  break;
      case STATUS_SERVER:
      case CLEANUP:
      case TEXT:
      case INVALID:
    	  type_ = reportedType;
    	  break;
      }
    }
  }

  public boolean encodeCleanUpMsg() {
    bb_.clear();
    final int msgSize = HEADER_SIZE;
    final MessageType type = MessageType.CLEANUP;
    bb_.putInt(type.getValue());
    bb_.putInt(msgSize);
    bb_.flip();
    state_ = State.UNDEFINED;
    return true;
  }

  public boolean encodeServerStatusMsg(String text)
  {
    if (text != null)
    {
      int textSize = text.getBytes().length;
      int msgSize = HEADER_SIZE + textSize;
      bb_.clear();
      bb_.putInt(MessageType.STATUS_SERVER.getValue());
      bb_.putInt(msgSize);
      
      bb_.put(text.getBytes(), 0, textSize);
      bb_.flip();
      state_ = State.UNDEFINED;
      return true;
    }
    return false;
  }
  
  public String decodeServerStatusMsg()
  {
    String text = null;
    if (type_ == MessageType.STATUS_SERVER)
    {
      if (bb_.position() < bb_.limit())
      {
        byte[] bytearr = new byte[bb_.remaining()];
        bb_.get(bytearr);
        text = new String(bytearr); 
      }
    }
    return text;
  }
  
  public boolean encodeClientStatusMsg(String text, boolean status) {
    if (text != null) {
      int textSize = text.getBytes().length;
      int msgSize = HEADER_SIZE + textSize;
      bb_.clear();
      if (status) {
        bb_.putInt(MessageType.STATUS_CLIENT.getValue());
      } else {
        bb_.putInt(MessageType.TEXT.getValue());
      }
      bb_.putInt(msgSize);
      if (bb_.capacity() > textSize) {
        bb_.put(text.getBytes(), 0, textSize);
        bb_.flip();
        state_ = State.UNDEFINED;
        return true;
      }
    }
    return false;
  }
  
  public String decodeClientStatusMsg() {
    String text = null;
    if ((type_ == MessageType.STATUS_CLIENT) || (type_ == MessageType.TEXT)) {
      if (bb_.position() < bb_.limit()) {
        byte[] bytearr = new byte[bb_.remaining()];
        bb_.get(bytearr);
        text = new String(bytearr); 
      }
    }
    return text;
  }

  public int encodeLines(final Vector<VectorStatus> lines, int offset, int numLines) {
    bb_.clear();
    int numLinesEncoded = 0;
    if (numLines > MAX_LINES) {
      numLines = MAX_LINES;
    }

    int msgSize = HEADER_SIZE + (numLines * LINE_SIZE);
    if (msgSize > bb_.capacity()) {
      //System.out.println("MsgCodec: Capacity problems: msgSize: " + msgSize + " bb.capacity: " + bb.capacity());
      state_ = State.UNDEFINED;
      return numLinesEncoded;
    }

    bb_.putInt(MessageType.LINE.getValue());
    bb_.putInt(msgSize);

    if (state_ == State.ENCODE) {
      for (int i = offset; i < numLines + offset; ++i) {
        VectorStatus vs = lines.get(i);
        for (int j = 0; j < 5; ++j) {
          bb_.putInt(vs.c[j]);
        }
        ++numLinesEncoded;
      }
    }
    state_ = State.UNDEFINED;

    bb_.flip();
    return numLinesEncoded;
  }

  public int decodeLines(Vector<VectorStatus> lines) {
    int numLines = 0;
    if (lines != null) {
      if (type_ == MessageType.LINE) {
        final int endPos = bb_.limit() - LINE_SIZE;
        while (bb_.position() <= endPos) {
          lines.add(new VectorStatus(bb_.getInt(), bb_.getInt(), bb_.getInt(), bb_.getInt(), bb_.getInt()));
          ++numLines;
        }
      }
    }
    state_ = State.UNDEFINED;
    return numLines;
  }
}
