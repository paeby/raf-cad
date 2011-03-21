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
				Node lastNode = null;
				fromTheTop: while (true) {
					lastNodePtr = firstNodePtr;
					lastNode = null;
					do {
						while (lastNodePtr.get() != null) {
							lastNode = lastNodePtr.get();
							lastNodePtr = lastNode.nextNode;
						}
					} while (!lastNodePtr.compareAndSet(null, newNode));
					if (lastNode != null && lastNode.isMarked.get())
						continue fromTheTop;
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
								System.out.println("Thread " + Thread.currentThread().getId() + " nastavlja");
							} catch (InterruptedException e) {
								throw new RuntimeException(e);
							}
						}
				}
				
				if (!firstNode.isMarked.compareAndSet(false, true)) {
					System.out.println("MARKED!");
					Thread.yield();
					continue fromTheTop;
				}
				secNode = firstNode.nextNode.get();
				
				if (this.firstNodePtr.compareAndSet(firstNode, secNode)) {
					synchronized (this) {
						int value = firstNode.value.get();
						System.out.println("Pročitao sam " + value);
					}
					return firstNode.value.get();
				} else 
					throw new IllegalStateException("Šta je bre ovo");
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
