package solutions.queue;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.registers.CASRegister;

import examples.queue.FifoQueue;
import examples.queue.FifoQueueTester;

public class FifoQueueSolutions {
	static final class DummyFifoQueue implements FifoQueue {
		@Override
		public void add(int value, ConcurrentSystem system, ProcessInfo callerInfo) {
			int last = system.getRegister(-1).read();
			last++;
			system.getRegister(-1).write(last);
			system.getRegister(last).write(value);
		}
		
		@Override
		public int remove(ConcurrentSystem system, ProcessInfo callerInfo) {
			int first = system.getRegister(-2).read();
			int last = system.getRegister(-1).read();
			if (first == last)
				return -1;
			int value = system.getRegister(first).read();
			system.getRegister(-2).write(first + 1);
			return value;
		}
	}
	
	static final class LockFifoQueue implements FifoQueue {
		@Override
		public void add(int value, ConcurrentSystem system, ProcessInfo callerInfo) {
			CASRegister lock = system.getCASRegister(-1);
			while (!lock.compareAndSet(0, 1))
				;
			
			int last = system.getRegister(-2).read();
			system.getRegister(last).write(value);
			system.getRegister(-2).write(last + 1);
			
			lock.write(0);
		}
		
		@Override
		public int remove(ConcurrentSystem system, ProcessInfo callerInfo) {
			CASRegister lock = system.getCASRegister(-1);
			while (!lock.compareAndSet(0, 1))
				;
			
			int first = system.getRegister(-3).read();
			int last = system.getRegister(-2).read();
			if (first == last) {
				lock.write(0);
				return -1;
			}
			int value = system.getRegister(first).read();
			system.getRegister(-3).write(first + 1);
			
			lock.write(0);
			return value;
		}
	}
	
	static final class TransactionFifoQueue implements FifoQueue {
		@Override
		public void add(int value, ConcurrentSystem system, ProcessInfo callerInfo) {
			try {
				system.transactionStarted();
				
				int last = system.getRegister(-1).read();
				system.getRegister(last).write(value);
				system.getRegister(-1).write(last + 1);
			} finally {
				system.transactionEnded();
			}
		}
		
		@Override
		public int remove(ConcurrentSystem system, ProcessInfo callerInfo) {
			try {
				system.transactionStarted();
				
				int first = system.getRegister(-2).read();
				int last = system.getRegister(-1).read();
				if (first == last) {
					return -1;
				}
				int value = system.getRegister(first).read();
				system.getRegister(-2).write(first + 1);
				
				return value;
			} finally {
				system.transactionEnded();
			}
		}
	}
	
	static final class LockFreeFifoQueue implements FifoQueue {
		@Override
		public void add(int value, ConcurrentSystem system, ProcessInfo callerInfo) {
			CASRegister emptyLocPtr = system.getCASRegister(-1);
			int emptyLoc;
			do {
				emptyLoc = emptyLocPtr.read();
			} while (!emptyLocPtr.compareAndSet(emptyLoc, emptyLoc + 2));
			emptyLoc += 2;
			
			system.getRegister(emptyLoc).write(value);
			
			fromTheTop: while (true) {
				CASRegister elemPtr = system.getCASRegister(1);
				int elem;
				do {
					while ((elem = elemPtr.read()) != 0) {
						if (elem < 0)
							continue fromTheTop;
						elemPtr = system.getCASRegister(elem + 1);
					}
				} while (!elemPtr.compareAndSet(0, emptyLoc));
				return;
			}
		}
		
		@Override
		public int remove(ConcurrentSystem system, ProcessInfo callerInfo) {
			CASRegister firstPtr = system.getCASRegister(1), secNextPtr;
			int first, second, value;
			fromTheTop: while (true) {
				first = firstPtr.read();
				if (first == 0)
					return -1;
				
				value = system.getRegister(first).read();
				secNextPtr = system.getCASRegister(first + 1);
				do {
					second = secNextPtr.read();
					if (second < 0) {
						continue fromTheTop;
					}
				} while (!secNextPtr.compareAndSet(second, -second - 1));
				
				if (firstPtr.compareAndSet(first, second)) {
					return value;
				} else {
					System.out.println("Aaaa");
				}
			}
		}
	}
	
	/**
	 * implementacija ConcurrentLinkedQueue
	 * http://www.cs.rochester.edu/u/scott/papers/1996_PODC_queues.pdf
	 * 
	 * <ul>
	 * <li>korisni adresni prostor počinje od 2 naviše</li>
	 * <li>izbacio sam count-ove jer nema recikliranja memorije</li>
	 * <li>ptr ka praznom prostoru je na -1</li>
	 * <li>Q->head je na -2</li>
	 * <li>Q->tail je na -3</li>
	 * <li>umesto null koristimo 0</li>
	 * </ul>
	 * 
	 * @author Bocete
	 */
	
	static final class WaitFreeFifoQueue implements FifoQueue {
		
		@Override
		public void add(int value, ConcurrentSystem system, ProcessInfo callerInfo) {
			CASRegister emptyPtr = system.getCASRegister(-1);
			int newLoc;
			do {
				newLoc = emptyPtr.read();
			} while (!emptyPtr.compareAndSet(newLoc, newLoc + 2));
			newLoc += 2;
			system.getRegister(newLoc).write(value);
			
			CASRegister tailReg = system.getCASRegister(-3);
			int tail, next;
			while (true) {
				tail = tailReg.read();
				next = system.getRegister(tail + 1).read();
				if (tail == tailReg.read()) {
					if (next == 0) {
						if (system.getCASRegister(tail + 1).compareAndSet(0, newLoc))
							break;
					} else {
						tailReg.compareAndSet(tail, next);
					}
				}
			}
			tailReg.compareAndSet(tail, newLoc);
		}
		
		@Override
		public int remove(ConcurrentSystem system, ProcessInfo callerInfo) {
			CASRegister headReg = system.getCASRegister(-2), tailReg = system.getCASRegister(-3);
			int head, tail, next, value;
			while (true) {
				head = headReg.read();
				tail = tailReg.read();
				next = system.getRegister(head + 1).read();
				if (head == headReg.read()) {
					if (head == tail) {
						if (next == 0)
							return -1;
						tailReg.compareAndSet(tail, next);
					} else {
						value = system.getRegister(next).read();
						if (headReg.compareAndSet(head, next))
							break;
					}
				}
			}
			return value;
		}
	}
	
	public static void main(String[] args) {
		FifoQueueTester.testFifoQueue(new LockFreeFifoQueue());
	}
}
