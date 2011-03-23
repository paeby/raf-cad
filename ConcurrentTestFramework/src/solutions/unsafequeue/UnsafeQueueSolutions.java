package solutions.unsafequeue;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import examples.unsafequeue.UnsafeQueue;
import examples.unsafequeue.UnsafeQueueTester;

public class UnsafeQueueSolutions {
	final static class NotEvenAQueueQueue implements UnsafeQueue {
		@Override
		public void put(int value) {
		}

		@Override
		public int remove() {
			return 0;
		}

	}

	final static class PositivelyWorkingQueue implements UnsafeQueue {
		final LinkedList<Integer> linkedList = new LinkedList<Integer>();

		@Override
		public synchronized void put(int value) {
			linkedList.addLast(value);
		}

		@Override
		public synchronized int remove() {
			if (linkedList.size() == 0)
				return -1;
			return linkedList.removeFirst();
		}
	}

	final static class LockFreeQueue implements UnsafeQueue {
		final AtomicReference<Node> firstNodePtr = new AtomicReference<Node>(null);
		final Node deletingDummyNode = new Node();

		@Override
		public void put(int value) {
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
		}

		@Override
		public int remove() {
			AtomicReference<Node> firstPtr = this.firstNodePtr, secNextPtr;
			Node first, second;
			int value;
			fromTheTop: while (true) {
				first = firstPtr.get();
				if (first == null)
					return -1;

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
		UnsafeQueueTester.testUnsafeQueue(new LockFreeQueue());
	}
}
