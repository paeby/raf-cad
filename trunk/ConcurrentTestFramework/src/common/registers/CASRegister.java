package common.registers;

public interface CASRegister extends Register {
	boolean compareAndSet(int expect, int update);
}
