package softnet.exceptions;

public class ConstraintViolationSoftnetException extends SoftnetException {
	private static final long serialVersionUID = 3281549326477949701L;

	public ConstraintViolationSoftnetException() {
		super(SoftnetError.ConstraintViolation, "One of the restrictions of hosting parameters has been violated.");
	}

	public ConstraintViolationSoftnetException(String message) {
		super(SoftnetError.ConstraintViolation, message);
	}
}
