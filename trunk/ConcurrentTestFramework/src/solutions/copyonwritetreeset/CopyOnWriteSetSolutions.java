package solutions.copyonwritetreeset;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import sun.misc.Unsafe;
import useful.UnsafeHelper;
import examples.copyonwriteset.CopyOnWriteSet;
import examples.copyonwriteset.CopyOnWriteSetTester;
import examples.unsafemutex.UnsafeMutex;

public class CopyOnWriteSetSolutions {
	
	public static final class PositivelyWorkingCoWSet implements CopyOnWriteSet {
//		private final java.util.concurrent.CopyOnWriteArraySet<Integer> set = new java.util.concurrent.CopyOnWriteArraySet<Integer>();
		private final java.util.concurrent.ConcurrentSkipListSet<Integer> set = new java.util.concurrent.ConcurrentSkipListSet<Integer>();
		
		@Override
		public boolean add(int value) {
			return set.add(value);
		}
		
		@Override
		public boolean remove(int value) {
			return set.remove(value);
		}
		
		@Override
		public boolean contains(int value) {
			return set.contains(value);
		}
	}
	
	public static final class CopyOnWriteSortedArraySet implements CopyOnWriteSet {
		private final FairMutex mutex = new FairMutex();
		private final AtomicReference<int[]> valuesRef = new AtomicReference<int[]>(new int[0]);
		
		@Override
		public boolean add(int value) {
			int[] oldValues = valuesRef.get();
			int index = Arrays.binarySearch(oldValues, value);
			if (index >= 0)
				return false;
			else {
				try {
					mutex.lock();
					oldValues = valuesRef.get();
					index = Arrays.binarySearch(oldValues, value);
					if (index >= 0)
						return false;
					
					index = -index - 1;
					int[] newArray = Arrays.copyOf(oldValues, oldValues.length + 1);
					for (int i = oldValues.length; i > index; i--) {
						newArray[i] = newArray[i - 1];
					}
					newArray[index] = value;
					valuesRef.set(newArray);
					
					return true;
				} finally {
					mutex.unlock();
				}
			}
		}
		
		@Override
		public boolean remove(int value) {
			int[] oldValues = valuesRef.get();
			int index = Arrays.binarySearch(oldValues, value);
			if (index < 0)
				return false;
			else {
				try {
					mutex.lock();
					oldValues = valuesRef.get();
					index = Arrays.binarySearch(oldValues, value);
					if (index < 0)
						return false;
					
					int[] newArray = new int[oldValues.length - 1];
					int i = 0;
					for (; i < index; i++)
						newArray[i] = oldValues[i];
					for (; i < newArray.length; i++)
						newArray[i] = oldValues[i + 1];
					
					valuesRef.set(newArray);
					
					return true;
				} finally {
					mutex.unlock();
				}
			}
		}
		
		@Override
		public boolean contains(int value) {
			return Arrays.binarySearch(valuesRef.get(), value) >= 0;
		}
	}
	
	public static final class CopyOnWriteTreeSet implements CopyOnWriteSet {
		private final FairMutex mutex = new FairMutex();
		private final AtomicReference<LockFreeAVLTreeSetNode> rootRef = new AtomicReference<CopyOnWriteSetSolutions.LockFreeAVLTreeSetNode>();
		
		public CopyOnWriteTreeSet() {
			LockFreeAVLTreeSetNode root = new LockFreeAVLTreeSetNode(Integer.MAX_VALUE, rootRef);
			rootRef.set(root);
		}
		
		@Override
		public boolean add(int value) {
			// if (rootRef.get().contains(value))
			// return false;
			try {
				mutex.lock();
				// if (rootRef.get().contains(value))
				// return false;
				// System.out.println("beforeAdd " + value + ": " +
				// rootRef.get().draw());
				boolean added = rootRef.get().add(value);
				// if (!rootRef.get().contains(value))
				// throw new RuntimeException();
				// System.out.println("afterAdd " + value + ": " +
				// rootRef.get().draw());
				return added;
			} finally {
				mutex.unlock();
			}
		}
		
		@Override
		public boolean remove(int value) {
			// if (!rootRef.get().contains(value))
			// return false;
			try {
				mutex.lock();
				// if (!rootRef.get().contains(value))
				// return false;
				
				// System.out.println("beforeDel " + value + ": " +
				// rootRef.get().draw());
				boolean deleted = rootRef.get().delete(value);
				// System.out.println("afterDel " + value + ": " +
				// rootRef.get().draw());
				// if (rootRef.get().contains(value))
				// throw new RuntimeException();
				return deleted;
			} finally {
				mutex.unlock();
			}
		}
		
		@Override
		public boolean contains(int value) {
			return rootRef.get().contains(value);
		}
		
		@Override
		public String toString() {
			return rootRef.get().toString();
		}
	}
	
	public static final class LockFreeAVLTreeSetNode {
		private final int value;
		private final AtomicReference<AtomicReference<LockFreeAVLTreeSetNode>> refOfMyParent;
		private final AtomicReference<LockFreeAVLTreeSetNode> parent;
		private final AtomicReference<LockFreeAVLTreeSetNode> leftChild,
				rightChild;
		
		public LockFreeAVLTreeSetNode(int value, AtomicReference<LockFreeAVLTreeSetNode> parentRef) {
			this(value, parentRef, null, null, null);
		}
		
		public LockFreeAVLTreeSetNode(int value, AtomicReference<LockFreeAVLTreeSetNode> refOfMyParent, LockFreeAVLTreeSetNode parent, LockFreeAVLTreeSetNode leftChild, LockFreeAVLTreeSetNode rightChild) {
			this.value = value;
			this.refOfMyParent = new AtomicReference<AtomicReference<LockFreeAVLTreeSetNode>>(refOfMyParent);
			this.parent = new AtomicReference<LockFreeAVLTreeSetNode>(parent);
			this.leftChild = new AtomicReference<LockFreeAVLTreeSetNode>(leftChild);
			this.rightChild = new AtomicReference<LockFreeAVLTreeSetNode>(rightChild);
		}
		
		public boolean contains(int value) {
			if (value == this.value)
				return true;
			else if (value < this.value) {
				LockFreeAVLTreeSetNode leftChild = this.leftChild.get();
				if (leftChild == null)
					return false;
				return leftChild.contains(value);
			} else {
				LockFreeAVLTreeSetNode rightChild = this.rightChild.get();
				if (rightChild == null)
					return false;
				return rightChild.contains(value);
			}
		}
		
		public int getHeight() {
			int height = 0;
			if (leftChild.get() != null) {
				height = leftChild.get().getHeight();
			}
			if (rightChild.get() != null) {
				int rightHeight = rightChild.get().getHeight();
				if (rightHeight > height)
					height = rightHeight;
			}
			return height + 1;
		}
		
		public int getBalanceFactor() {
			int leftHeight = leftChild.get() == null ? 0 : leftChild.get().getHeight();
			int rightHeight = rightChild.get() == null ? 0 : rightChild.get().getHeight();
			return leftHeight - rightHeight;
		}
		
		private final AtomicInteger rotating = new AtomicInteger(0);
		
		public void rotateLeft() {
			if (rotating.incrementAndGet() > 1)
				throw new RuntimeException();
			LockFreeAVLTreeSetNode rightChild = this.rightChild.get();
			LockFreeAVLTreeSetNode childA = this.leftChild.get();
			LockFreeAVLTreeSetNode childB = rightChild.leftChild.get();
			LockFreeAVLTreeSetNode childC = rightChild.rightChild.get();
			
			LockFreeAVLTreeSetNode newRightChild = new LockFreeAVLTreeSetNode(rightChild.value, this.refOfMyParent.get(), this.parent.get(), null, childC);
			LockFreeAVLTreeSetNode newThis = new LockFreeAVLTreeSetNode(this.value, newRightChild.leftChild, newRightChild, childA, childB);
			newRightChild.leftChild.set(newThis);
			
			this.refOfMyParent.get().set(newRightChild);
			
			if (childA != null) {
				childA.parent.set(newThis);
				childA.refOfMyParent.set(newThis.leftChild);
			}
			if (childB != null) {
				childB.parent.set(newThis);
				childB.refOfMyParent.set(newThis.rightChild);
			}
			if (childC != null) {
				childC.parent.set(newRightChild);
				childC.refOfMyParent.set(newRightChild.rightChild);
			}
			rotating.decrementAndGet();
		}
		
		public void rotateRight() {
			if (rotating.incrementAndGet() > 1)
				throw new RuntimeException();
			LockFreeAVLTreeSetNode leftChild = this.leftChild.get();
			LockFreeAVLTreeSetNode childA = leftChild.leftChild.get();
			LockFreeAVLTreeSetNode childB = leftChild.rightChild.get();
			LockFreeAVLTreeSetNode childC = this.rightChild.get();
			
			LockFreeAVLTreeSetNode newLeftChild = new LockFreeAVLTreeSetNode(leftChild.value, this.refOfMyParent.get(), this.parent.get(), childA, null);
			LockFreeAVLTreeSetNode newThis = new LockFreeAVLTreeSetNode(this.value, newLeftChild.rightChild, newLeftChild, childB, childC);
			newLeftChild.rightChild.set(newThis);
			
			this.refOfMyParent.get().set(newLeftChild);
			
			if (childA != null) {
				childA.parent.set(newLeftChild);
				childA.refOfMyParent.set(newLeftChild.leftChild);
			}
			if (childB != null) {
				childB.parent.set(newThis);
				childB.refOfMyParent.set(newThis.leftChild);
			}
			if (childC != null) {
				childC.parent.set(newThis);
				childC.refOfMyParent.set(newThis.rightChild);
			}
			rotating.decrementAndGet();
		}
		
		public boolean balance() {
			int balanceFactor = getBalanceFactor();
			// System.out.println("Balance factor for node " + value + ": " +
			// balanceFactor);
			if (balanceFactor == 2) {
				int leftChildBalance = leftChild.get().getBalanceFactor();
				if (leftChildBalance < 0)
					leftChild.get().rotateLeft();
				rotateRight();
				return true;
			} else if (balanceFactor == -2) {
				int rightChildBalance = rightChild.get().getBalanceFactor();
				if (rightChildBalance > 0)
					rightChild.get().rotateRight();
				rotateLeft();
				return true;
			}
			return false;
		}
		
		final AtomicInteger adding = new AtomicInteger(0);
		
		public boolean add(int value) {
			try {
				adding.incrementAndGet();
				if (value == this.value)
					return false;
				if (value < this.value) {
					if (leftChild.get() == null) {
						leftChild.set(new LockFreeAVLTreeSetNode(value, leftChild, this, null, null));
						return true;
					} else {
						if (!leftChild.get().add(value))
							return false;
						balance();
						return true;
					}
				} else {
					if (rightChild.get() == null) {
						rightChild.set(new LockFreeAVLTreeSetNode(value, rightChild, this, null, null));
						return true;
					} else {
						if (!rightChild.get().add(value))
							return false;
						balance();
						return true;
					}
				}
			} finally {
				adding.decrementAndGet();
			}
		}
		
		public void forceDelete() {
			if (rightChild.get() == null) {
				LockFreeAVLTreeSetNode leftChild = this.leftChild.get();
				
				this.refOfMyParent.get().set(leftChild);
				if (leftChild != null) {
					leftChild.parent.set(this.parent.get());
					leftChild.refOfMyParent.set(this.refOfMyParent.get());
				}
				
				LockFreeAVLTreeSetNode nodeToBeBalanced = this.parent.get();
				while (nodeToBeBalanced != null) {
					nodeToBeBalanced.balance();
					nodeToBeBalanced = nodeToBeBalanced.parent.get();
				}
			} else if (leftChild.get() == null) {
				LockFreeAVLTreeSetNode rightChild = this.rightChild.get();
				
				this.refOfMyParent.get().set(rightChild);
				rightChild.parent.set(this.parent.get());
				rightChild.refOfMyParent.set(this.refOfMyParent.get());
				
				LockFreeAVLTreeSetNode nodeToBeBalanced = this.parent.get();
				while (nodeToBeBalanced != null) {
					nodeToBeBalanced.balance();
					nodeToBeBalanced = nodeToBeBalanced.parent.get();
				}
			} else {
				LockFreeAVLTreeSetNode leastGreaterNode = rightChild.get();
				while (leastGreaterNode.leftChild.get() != null)
					leastGreaterNode = leastGreaterNode.leftChild.get();
				
				LockFreeAVLTreeSetNode myLeftChild = this.leftChild.get();
				LockFreeAVLTreeSetNode myRightChild = this.rightChild.get();
				LockFreeAVLTreeSetNode newLGN = new LockFreeAVLTreeSetNode(leastGreaterNode.value, this.refOfMyParent.get(), this.parent.get(), myLeftChild, myRightChild);
				this.refOfMyParent.get().set(newLGN);
				
				myLeftChild.refOfMyParent.set(newLGN.leftChild);
				myLeftChild.parent.set(newLGN);
				myRightChild.refOfMyParent.set(newLGN.rightChild);
				myRightChild.parent.set(newLGN);
				
				leastGreaterNode.forceDelete();
			}
		}
		
		AtomicInteger deleting = new AtomicInteger();
		
		public boolean delete(int value) {
			try {
				deleting.incrementAndGet();
				if (this.value == value) {
					forceDelete();
					return true;
				} else if (value < this.value) {
					if (leftChild.get() == null)
						return false;
					else
						return leftChild.get().delete(value);
				} else {
					if (rightChild.get() == null)
						return false;
					else
						return rightChild.get().delete(value);
				}
			} finally {
				deleting.decrementAndGet();
			}
		}
		
		@Override
		public String toString() {
			// return (leftChild.get() != null ? leftChild.get().toString() +
			// ", " : "") + value + (rightChild.get() != null ? ", " +
			// rightChild.get().toString() : "");
			return "Rotating: " + rotating.get() + " Adding: " + adding.get() + " Deleting: " + deleting.get() + " " + draw();
		}
		
		public String draw() {
			return '(' + (leftChild.get() == null ? "-" : leftChild.get().draw()) + ',' + value + ',' + (rightChild.get() == null ? "-" : rightChild.get().draw()) + ")";
		}
	}
	
	public static final class RegularAVLTreeSetNode {
		private final int value;
		private final AtomicReference<AtomicReference<RegularAVLTreeSetNode>> refOfMyParent;
		private final AtomicReference<RegularAVLTreeSetNode> parent;
		private final AtomicReference<RegularAVLTreeSetNode> leftChild,
				rightChild;
		
		public RegularAVLTreeSetNode(int value, AtomicReference<RegularAVLTreeSetNode> parentRef) {
			this(value, parentRef, null, null, null);
		}
		
		public RegularAVLTreeSetNode(int value, AtomicReference<RegularAVLTreeSetNode> refOfMyParent, RegularAVLTreeSetNode parent, RegularAVLTreeSetNode leftChild, RegularAVLTreeSetNode rightChild) {
			this.value = value;
			this.refOfMyParent = new AtomicReference<AtomicReference<CopyOnWriteSetSolutions.RegularAVLTreeSetNode>>(refOfMyParent);
			this.parent = new AtomicReference<CopyOnWriteSetSolutions.RegularAVLTreeSetNode>(parent);
			this.leftChild = new AtomicReference<CopyOnWriteSetSolutions.RegularAVLTreeSetNode>(leftChild);
			this.rightChild = new AtomicReference<CopyOnWriteSetSolutions.RegularAVLTreeSetNode>(rightChild);
		}
		
		public boolean contains(int value) {
			if (value == this.value)
				return true;
			else if (value < this.value) {
				if (leftChild.get() == null)
					return false;
				return leftChild.get().contains(value);
			} else {
				if (rightChild.get() == null)
					return false;
				return rightChild.get().contains(value);
			}
		}
		
		public int getHeight() {
			int height = 0;
			if (leftChild.get() != null) {
				height = leftChild.get().getHeight();
			}
			if (rightChild.get() != null) {
				int rightHeight = rightChild.get().getHeight();
				if (rightHeight > height)
					height = rightHeight;
			}
			return height + 1;
		}
		
		private int getBalanceFactor() {
			int leftHeight = leftChild.get() == null ? 0 : leftChild.get().getHeight();
			int rightHeight = rightChild.get() == null ? 0 : rightChild.get().getHeight();
			return leftHeight - rightHeight;
		}
		
		private void rotateLeft() {
			RegularAVLTreeSetNode rightChild = this.rightChild.get();
			RegularAVLTreeSetNode rightLeftChild = rightChild.leftChild.get();
			
			rightChild.refOfMyParent.set(this.refOfMyParent.get());
			rightChild.parent.set(this.parent.get());
			this.refOfMyParent.get().set(rightChild);
			
			this.refOfMyParent.set(rightChild.leftChild);
			this.parent.set(rightChild);
			rightChild.leftChild.set(this);
			
			this.rightChild.set(rightLeftChild);
			if (rightLeftChild != null) {
				rightLeftChild.refOfMyParent.set(this.rightChild);
				rightLeftChild.parent.set(this);
			}
		}
		
		private void rotateRight() {
			RegularAVLTreeSetNode leftChild = this.leftChild.get();
			RegularAVLTreeSetNode leftRightChild = leftChild.rightChild.get();
			
			leftChild.refOfMyParent.set(this.refOfMyParent.get());
			leftChild.parent.set(this.parent.get());
			this.refOfMyParent.get().set(leftChild);
			
			this.refOfMyParent.set(leftChild.rightChild);
			this.parent.set(leftChild);
			leftChild.rightChild.set(this);
			
			this.leftChild.set(leftRightChild);
			if (leftRightChild != null) {
				leftRightChild.refOfMyParent.set(this.leftChild);
				leftRightChild.parent.set(this);
			}
		}
		
		private boolean balance() {
			int balanceFactor = getBalanceFactor();
			if (balanceFactor == 2) {
				int leftChildBalance = leftChild.get().getBalanceFactor();
				if (leftChildBalance < 0)
					leftChild.get().rotateLeft();
				rotateRight();
				return true;
			} else if (balanceFactor == -2) {
				int rightChildBalance = rightChild.get().getBalanceFactor();
				if (rightChildBalance > 0)
					rightChild.get().rotateRight();
				rotateLeft();
				return true;
			}
			return false;
		}
		
		public boolean add(int value) {
			if (value == this.value)
				return false;
			if (value < this.value) {
				if (leftChild.get() == null) {
					leftChild.set(new RegularAVLTreeSetNode(value, leftChild, this, null, null));
					return true;
				} else {
					if (!leftChild.get().add(value))
						return false;
					balance();
					return true;
				}
			} else {
				if (rightChild.get() == null) {
					rightChild.set(new RegularAVLTreeSetNode(value, rightChild, this, null, null));
					return true;
				} else {
					if (!rightChild.get().add(value))
						return false;
					balance();
					return true;
				}
			}
		}
		
		private void forceDelete() {
			if (rightChild.get() == null) {
				RegularAVLTreeSetNode leftChild = this.leftChild.get();
				
				this.refOfMyParent.get().set(leftChild);
				if (leftChild != null) {
					leftChild.parent.set(this.parent.get());
					leftChild.refOfMyParent.set(this.refOfMyParent.get());
				}
				
				RegularAVLTreeSetNode nodeToBeBalanced = this.parent.get();
				boolean balanced = false;
				while (nodeToBeBalanced != null && !balanced) {
					balanced = nodeToBeBalanced.balance();
					nodeToBeBalanced = nodeToBeBalanced.parent.get();
				}
			} else if (leftChild.get() == null) {
				RegularAVLTreeSetNode rightChild = this.rightChild.get();
				
				this.refOfMyParent.get().set(rightChild);
				rightChild.parent.set(this.parent.get());
				rightChild.refOfMyParent.set(this.refOfMyParent.get());
				
				RegularAVLTreeSetNode nodeToBeBalanced = this.parent.get();
				boolean balanced = false;
				while (nodeToBeBalanced != null && !balanced) {
					balanced = nodeToBeBalanced.balance();
					nodeToBeBalanced = nodeToBeBalanced.parent.get();
				}
			} else {
				RegularAVLTreeSetNode leastGreaterNode = rightChild.get();
				while (leastGreaterNode.leftChild.get() != null)
					leastGreaterNode = leastGreaterNode.leftChild.get();
				
				RegularAVLTreeSetNode nodeToBeBalanced = leastGreaterNode.parent.get();
				
				leastGreaterNode.forceDelete();
				
				this.refOfMyParent.get().set(leastGreaterNode);
				leastGreaterNode.refOfMyParent.set(this.refOfMyParent.get());
				leastGreaterNode.parent.set(this.parent.get());
				
				leastGreaterNode.leftChild.set(this.leftChild.get());
				if (this.leftChild.get() != null) {
					this.leftChild.get().refOfMyParent.set(leastGreaterNode.leftChild);
					this.leftChild.get().parent.set(leastGreaterNode);
				}
				
				leastGreaterNode.rightChild.set(this.rightChild.get());
				if (this.rightChild.get() != null) {
					this.rightChild.get().refOfMyParent.set(leastGreaterNode.rightChild);
					this.rightChild.get().parent.set(leastGreaterNode);
				}
				
				while (nodeToBeBalanced != null) {
					nodeToBeBalanced.balance();
					nodeToBeBalanced = nodeToBeBalanced.parent.get();
				}
			}
		}
		
		public boolean delete(int value) {
			if (this.value == value) {
				forceDelete();
				return true;
			} else if (value < this.value) {
				if (leftChild.get() == null)
					return false;
				else
					return leftChild.get().delete(value);
			} else {
				if (rightChild.get() == null)
					return false;
				else
					return rightChild.get().delete(value);
			}
		}
		
		@Override
		public String toString() {
			return (leftChild.get() != null ? leftChild.get().toString() + ", " : "") + value + (rightChild.get() != null ? ", " + rightChild.get().toString() : "");
		}
		
		public String draw() {
			if (refOfMyParent.get().get() != this)
				System.out.println("ahahahah");
			if (parent.get() != null) {
				if (parent.get().rightChild.get() != this && parent.get().leftChild.get() != this)
					System.out.println("ahahahah");
			}
			return '(' + (leftChild.get() == null ? "-" : leftChild.get().draw()) + ',' + value + ',' + (rightChild.get() == null ? "-" : rightChild.get().draw()) + ")";
		}
	}
	
	public final static class FairMutex implements UnsafeMutex {
		private static final int ARRAY_SIZE = 1000;
		private final AtomicLong ticketDispenser = new AtomicLong(0);
		private final AtomicLong currentCustomer = new AtomicLong(0);
		private final AtomicReferenceArray<Thread> customersInLine = new AtomicReferenceArray<Thread>(ARRAY_SIZE);
		private final Unsafe unsafe = UnsafeHelper.getUnsafe();
		private final AtomicBoolean unlockingInProgress = new AtomicBoolean();
		
		@Override
		public void lock() {
			long myTicket = ticketDispenser.getAndIncrement();
			customersInLine.set((int) (myTicket % ARRAY_SIZE), Thread.currentThread());
			while (unlockingInProgress.get() != false)
				Thread.yield();
			while (currentCustomer.get() != myTicket) {
				unsafe.park(false, 0l);
			}
			customersInLine.set((int) (myTicket % ARRAY_SIZE), null);
		}
		
		@Override
		public void unlock() {
			long myTicket = currentCustomer.get();
			Thread nextToWakeUp = null;
			unlockingInProgress.set(true);
			if (ticketDispenser.get() > myTicket + 1) {
				while ((nextToWakeUp = customersInLine.get((int) ((myTicket + 1) % ARRAY_SIZE))) == null)
					Thread.yield();
				while (nextToWakeUp.getState() != Thread.State.WAITING) {
					unlockingInProgress.set(false);
					Thread.yield();
					unlockingInProgress.set(true);
				}
			}
			currentCustomer.incrementAndGet();
			unlockingInProgress.set(false);
			if (nextToWakeUp != null)
				unsafe.unpark(nextToWakeUp);
		}
	}
	
	public static void main(String[] args) {
		CopyOnWriteSetTester.testCopyOnWriteSet(new PositivelyWorkingCoWSet());
	}
}
