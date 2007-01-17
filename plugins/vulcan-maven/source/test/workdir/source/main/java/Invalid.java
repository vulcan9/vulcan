// used to test that erorrs printed to stderr during build are captured.
public class Invalid {
	Syntax error;

	private void a() {
		noSuchHelperMethod();
	}
}
