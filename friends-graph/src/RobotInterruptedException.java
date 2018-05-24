
/**
 * Thrown when an InterruptibleRobot is interrupted by user mouse movement.
 * @author roger
 *
 */
public class RobotInterruptedException extends Exception {

    private static final long serialVersionUID = 6871597708883005480L;

    public RobotInterruptedException() {
        super();
    }

    public RobotInterruptedException(String arg0) {
        super(arg0);
    }

    public RobotInterruptedException(Throwable arg0) {
        super(arg0);
    }

    public RobotInterruptedException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    public RobotInterruptedException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
        super(arg0, arg1, arg2, arg3);
    }

}
