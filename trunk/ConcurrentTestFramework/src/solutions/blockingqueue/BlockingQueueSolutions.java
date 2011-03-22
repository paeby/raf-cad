package solutions.blockingqueue;

import java.util.concurrent.LinkedBlockingDeque;
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
		final Node deletingDummyNode = new Node();
		
		@Override
		public void put(int value) {
			try {
				Node newNode = new Node();
				newNode.value.set(value);
				
				AtomicReference<Node> lastNodePtr;
				fromTheTop: while (true) {
					lastNodePtr = firstNodePtr;
					do {
						while (true) {
							Node node = lastNodePtr.get();
							if (node == null)
								break;
							if (node == deletingDummyNode)
								continue fromTheTop;
							lastNodePtr = node.nextNode;
						}
					} while (!lastNodePtr.compareAndSet(null, newNode));
					return;
				}
			} finally {
				synchronized (this) {
					notifyAll();
				}
			}
		}
		
		@Override
		public int remove() {
			AtomicReference<Node> firstPtr = this.firstNodePtr, secNextPtr;
			Node first, second;
			int value;
			fromTheTop: while (true) {
				first = firstPtr.get();
				while (first == null) {
					synchronized (this) {
						try {
							wait(5);
						} catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
					}
					first = firstPtr.get();
				}
				
				value = first.value.get();
				secNextPtr = first.nextNode;
				
				do {
					second = secNextPtr.get();
					if (second == deletingDummyNode)
						continue fromTheTop;
				} while (!secNextPtr.compareAndSet(second, deletingDummyNode));
				
				if (this.firstNodePtr.compareAndSet(first, second)) {
					return value;
				} else
					throw new IllegalStateException("Šta je bre ovo");
			}
		}
		
		private static class Node {
			final AtomicInteger value = new AtomicInteger(-1);
			final AtomicReference<Node> nextNode = new AtomicReference<Node>(null);
		}
	}
	
	public static void main(String[] args) {
		BlockingQueueTester.testBlockingQueue(new LockFreeQueue());
	}
}
