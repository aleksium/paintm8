package p.m.paintm8;

import java.time.Duration;

public class Environment {
    
    public static final int SERVER_PORT = 3174;
    public static final int MAX_NUMBER_OF_CLIENTS = 20;
    public static final int CANVAS_WIDTH = 640;
    public static final int CANVAS_HEIGHT = 380;
    public static final Duration MAX_TIME_WITHOUT_SIGN_OF_LIFE = Duration.ofMillis(21000);
}
