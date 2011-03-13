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
			system.getRegister(emptyLoc + 1).write(0);
			
			CASRegister elemPtr = system.getCASRegister(1);
			int elem;
			do {
				elem = elemPtr.read();
				while (elem > 0)
					elemPtr = system.getCASRegister(elem + 1);
			} while (!elemPtr.compareAndSet(0, emptyLoc));
		}
		
		@Override
		public int remove(ConcurrentSystem system, ProcessInfo callerInfo) {
			CASRegister firstPtr = system.getCASRegister(1);
			int first, second, value;
			do {
				first = firstPtr.read();
				if (first == 0)
					return -1;
				value = system.getRegister(first).read();
				second = system.getRegister(first + 1).read();
			} while (!firstPtr.compareAndSet(first, second));
			return value;
		}
	}
	
	/**
	 * implementacija ConcurrentLinkedQueue
	 * http://www.cs.rochester.edu/u/scott/papers/1996_PODC_queues.pdf
	 * 
	 * korisni adresni prostor počinje od 2 naviše izbacio sam count-ove jer
	 * nema recikliranja memorije ptr ka praznom prostoru je na -1 Q->head je na
	 * -2 Q->tail je na -3 umesto null koristimo 0
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
		FifoQueueTester.testFifoQueue(new TransactionFifoQueue());
	}
}
