package common.registers;

public interface Register {
	int read();
	void write(int value);
}
