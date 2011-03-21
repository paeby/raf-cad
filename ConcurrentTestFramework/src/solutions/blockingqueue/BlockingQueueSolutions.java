package solutions.blockingqueue;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import examples.blockingqueue.BlockingQueue;
import examples.blockingqueue.BlockingQueueTester;

public class BlockingQueueSolutions {
	final static class NotEvenAQueueBlockingQueue implements BlockingQueue {
		@Override
		public void put(int value) {}
		
		@Override
		public int remove() {
			return 0;
		}
		
	}
	
	final static class NonBlockingQueue implements BlockingQueue {
		@Override
		public void put(int value) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public int remove() {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}
	
	final static class PositivelyWorkingBlockingQueue implements BlockingQueue {
		final LinkedBlockingDeque<Integer> linkedBlockingQueue = new LinkedBlockingDeque<Integer>();
		
		@Override
		public void put(int value) {
			linkedBlockingQueue.add(value);
		}
		
		@Override
		public int remove() {
			try {
				return linkedBlockingQueue.take();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		};
	}
	
	final static class LockFreeQueue implements BlockingQueue {
		final AtomicReference<Node> firstNodePtr = new AtomicReference<Node>(null);
		
		@Override
		public void put(int value) {
			try {
				Node newNode = new Node();
				newNode.value.set(value);
				
				AtomicReference<Node> lastNodePtr;
				fromTheTop: while (true) {
					lastNodePtr = firstNodePtr;
					do {
						while (lastNodePtr.get() != null) {
							if (lastNodePtr.get().isMarked.get()) {
								Thread.yield();
								continue fromTheTop;
							}
							lastNodePtr = lastNodePtr.get().nextNode;
						}
					} while (!lastNodePtr.compareAndSet(null, newNode));
					return;
				}
			} finally {
				synchronized (this) {
					notify();
				}
			}
		}
		
		@Override
		public int remove() {
			Node firstNode = null, secNode;
			int value;
			fromTheTop: while (true) {
				while (firstNode == null) {
					firstNode = this.firstNodePtr.get();
					if (firstNode != null)
						break;
					else
						synchronized (this) {
							try {
								System.out.println("Thread " + Thread.currentThread().getId() + " cheka");
								wait();
								System.out.println("Thread " + Thread.currentThread().getId() + " se budi");
							} catch (InterruptedException e) {
								throw new RuntimeException(e);
							}
						}
				}
				
				value = firstNode.value.get();
				secNode = firstNode.nextNode.get();
				
				if (!firstNode.isMarked.compareAndSet(false, true)) {
					continue fromTheTop;
				}
				
				if (this.firstNodePtr.compareAndSet(firstNode, secNode)) {
					return value;
				} else {
					System.out.println("Aaaa");
				}
			}
		}
		
		private static class Node {
			final AtomicInteger value = new AtomicInteger(-1);
			final AtomicReference<Node> nextNode = new AtomicReference<Node>(null);
			final AtomicBoolean isMarked = new AtomicBoolean(false);
		}
	}
	
	public static void main(String[] args) {
		BlockingQueueTester.testBlockingQueue(new LockFreeQueue());
	}
}
