import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;

/**
 * A java Robot that monitors whether it has been interrupted by user input. An
 * interruption is detected if the last position that the robot moved the mouse to
 * doesn't match its current location (i.e. a user manually moved it). If
 * the robot has been interrupted, then the Robot methods that are overridden in this
 * class will not execute and will return null (if applicable).
 * @author roger
 *
 */
public class InterruptibleRobot extends Robot {
    
    private int lastXMove;
    private int lastYMove;
    public boolean interrupted;

    public InterruptibleRobot() throws AWTException {
        super();
        lastXMove = -1;
        lastYMove = -1;
        interrupted = false;
    }
    
    @Override
    public void mouseMove(int x, int y) {
        if (checkInterrupt()) { return; }
        super.mouseMove(x, y);
        lastXMove = x;
        lastYMove = y;
    }
    
    @Override
    public void mousePress(int buttons) {
        if (checkInterrupt()) { return; }
        super.mousePress(buttons);
    }
    
    @Override
    public void mouseRelease(int buttons) {
        if (checkInterrupt()) { return; }
        super.mouseRelease(buttons);
    }
    
    @Override
    public void mouseWheel(int wheelAmt) {
        if (checkInterrupt()) { return; }
        super.mouseWheel(wheelAmt);
    }
    
    @Override
    public void keyPress(int keycode) {
        if (checkInterrupt()) { return; }
        super.keyPress(keycode);
    }
    
    @Override
    public void keyRelease(int keycode) {
        if (checkInterrupt()) { return; }
        super.keyRelease(keycode);
    }
    
    /**
     * Checks if the user (or any other program) has interrupted the robot by checking
     * if the mouse cursor is still in the same position that it was last mouseMove'ed to.
     * If interrupted, the instance variable `interrupted` is set to true.
     * @return the value of `interrupted`
     */
    private boolean checkInterrupt() {
        if (interrupted || lastXMove == -1 || lastYMove == -1) { return interrupted; }
        Point curr = MouseInfo.getPointerInfo().getLocation();
        interrupted = Math.abs(curr.x - lastXMove) > 3
        		|| Math.abs(curr.y - lastYMove) > 3;
        return interrupted;
    }
    
}