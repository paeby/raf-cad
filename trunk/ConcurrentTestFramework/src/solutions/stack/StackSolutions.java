package solutions.stack;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import common.ConcurrentSystem;
import common.ProcessInfo;

import examples.stack.Stack;
import examples.stack.StackTester;

/**
 * Test nije dobar
 * @author Bocete
 *
 */
@Deprecated
public class StackSolutions {
	public static class NonWorkingStack implements Stack {

		@Override
		public void push(int n, ConcurrentSystem system, ProcessInfo callerInfo) {
			int index = system.getCASRegister(-1).read();
			system.getCASRegister(index).write(n);
			system.getCASRegister(-1).write(index + 1);
		}

		@Override
		public int poll(ConcurrentSystem system, ProcessInfo callerInfo) {
			int index = system.getCASRegister(-1).read();
			if (index == 0)
				return -1;
			int value = system.getCASRegister(index - 1).read(); 
			system.getCASRegister(-1).write(index - 1);
			return value;
		}	
	}
	
	public static class CheatingStack implements Stack {
		
		private final BlockingDeque<Integer> deque = new LinkedBlockingDeque<Integer>();
		
		@Override
		public void push(int n, ConcurrentSystem system, ProcessInfo callerInfo) {
			deque.add(n);
		}
		
		@Override
		public int poll(ConcurrentSystem system, ProcessInfo callerInfo) {
			if (deque.isEmpty())
				return -1;
			else
				return deque.pollLast();
		}
	}
	
	public static void main(String[] args) {
		StackTester.testStack(new NonWorkingStack());
	}
}
