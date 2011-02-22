package examples.balance;

import common.ConcurrentSystem;
import common.registers.CASRegister;
import common.registers.Register;
import common.tasks.Task;

import core.ConcurrentTester;

public class BalanceTester {
	public static class Add implements Task {
		private final int inc;
		
		public Add(int inc) {
			this.inc = inc;
		}
		
		@Override
		public void execute(ConcurrentSystem system) {
			Register balance = system.getRegister(0);
			int cur = balance.read();
			balance.write(cur + inc);
		}
		
		@Override
		public String toString() {
			return getClass().getSimpleName() + "[" + inc + "]";
		}
	}
	
	public static class Remove implements Task {
		private final int dec;
		
		public Remove(int dec) {
			this.dec = dec;
		}
		
		@Override
		public void execute(ConcurrentSystem system) {
			Register balance = system.getRegister(0);
			int cur = balance.read();
			balance.write(cur - dec);
		}
		
		@Override
		public String toString() {
			return getClass().getSimpleName() + "[" + dec + "]";
		}
	}
	
	public static class AddSafe implements Task {
		private final int inc;
		
		public AddSafe(int inc) {
			this.inc = inc;
		}
		
		@Override
		public void execute(ConcurrentSystem system) {
			CASRegister balance = system.getCASRegister(0);
			while (true) {
				int cur = balance.read();
				if (balance.compareAndSet(cur, cur + inc))
					return;
			}
		}
		
		@Override
		public String toString() {
			return getClass().getSimpleName() + "[" + inc + "]";
		}
	}
	
	public static void main(String[] args) {
		System.out.println(" *** Unsafe add task test *** ");
		ConcurrentTester.testTasks(new Add(5), new Add(7), new Remove(11));
		
		System.out.println(" *** Safe add task test *** ");
		ConcurrentTester.testTasks(new AddSafe(5), new AddSafe(7), new AddSafe(-11));
	}
}
