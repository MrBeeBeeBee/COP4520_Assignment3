import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.io.PrintWriter;

public class Assignment3a
{
	public static final List<Integer> presentBag = Collections.synchronizedList(new ArrayList<>());
	private static PrintWriter printWriter;

	public static int presentsAdded = 0;
	public static int notesSent = 0;

	static class Present
	{
		int presentID;
		Present next;

		public Present(int presentID)
		{
			this.presentID = presentID;
			this.next = null;
		}
		
	}

	static class PresentChain
	{
		private Present head;
		private final Lock lock;

		public PresentChain()
		{
			this.head = null;
			this.lock = new ReentrantLock();
		}

		public void addPresent(int presentID)
		{
			lock.lock();

			try
			{
				Present newPresent = new Present(presentID);
				Present cur = head;
				Present prev = null;

				while (cur != null && cur.presentID < presentID)
				{
					prev = cur;
					cur = cur.next;
				}
				if (prev == null)
				{
					newPresent.next = head;
					head = newPresent;
				}
				else
				{
					prev.next = newPresent;
					newPresent.next = cur;
				}
			}
			finally
			{
				lock.unlock();
			}
		}

		public Present writeNote()
		{
			lock.lock();

			Present newHead = null;

			try
			{
				if (head != null)
				{
					newHead = head;
					head = head.next;
				}
			}
			finally
			{
				lock.unlock();
			}

			return newHead;
		}

		public boolean findPresent(int presentID)
		{
			lock.lock();

			try
			{
				Present current = head;

				while (current != null)
				{
					if (current.presentID == presentID)
					{
						// Present found
						return true;
					}

					current = current.next;
				}

				// Present not found
				return false;
			}
			finally
			{
				lock.unlock();
			}
		}
	}

	// The actable threads
	static class MinotaurServant implements Runnable {
		private final PresentChain presentChain;
		private final Random random;
	
		public MinotaurServant(PresentChain presentChain)
		{
			this.presentChain = presentChain;
			this.random = new Random();
		}
	
		// The main action of the threads. Assigns them to complete one of three actions.
		// 1) The thread adds a present to the presentChain from the presents in the bag
		// 2) The thread removes the first present from the presentChain (& sends a note)
		// 3) The thread attempts to find a specific present in the presentChain
		@Override
		public void run() {
			while(!(presentBag.isEmpty() && presentChain.head == null)) {
				// Generate a random action
				int servantOrder = random.nextInt(3);
	
				switch (servantOrder) {
					case 0:
						addPresent();
						break;
					case 1:
						writeNote();
						break;
					case 2:
						findPresent();
						break;
					default:
						break;
				}
			}
		}
	
		// Adds a present to the list, using its ID to
		// place it in the correct location
		private void addPresent()
		{
			int presentID;

			synchronized (presentBag)
			{
				if (presentBag.isEmpty())
				{
					return;
				}
				presentID = presentBag.remove(0);
				presentChain.addPresent(presentID);
			}

			printWriter.println(Thread.currentThread().getName() 
			+ " added Present with ID: " + presentID);

			presentsAdded+= 1;
		}
	
		// Removes the first present in the list and "sends" a thank you note
		private void writeNote()
		{
			Present newHead = presentChain.writeNote();

			if (newHead != null)
			{
				printWriter.println(Thread.currentThread().getName() + " sent a letter to the gifter of present ID: " + newHead.presentID);
				notesSent += 1;
			}
		}
	
		// Attempts to find the present with a specific ID is in the presentChain
		private void findPresent()
		{
			int presentID = random.nextInt(500000);
			
			boolean found = presentChain.findPresent(presentID);
			if (found)
			{
				printWriter.println(Thread.currentThread().getName() + " found Present with ID: " + presentID + " in the chain");
			}
			else
			{
				printWriter.println(Thread.currentThread().getName() + " did not find Present with ID: " + presentID + " in the chain");
			}
		}
	}

	public static void main(String[] args)
	{	

		// Start tracking the time for the end report
		long startTime = System.currentTimeMillis();

		// Initialize Printwriter
		try
		{
			printWriter = new PrintWriter("thankYouNoteOutput.txt");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}

		// Pre-generate all presents and shuffle them
		System.out.println("Filling the bag");
		for (int i = 1; i <= 500000; i++)
		{
			presentBag.add(i);
		}
		Collections.shuffle(presentBag);

		// Create list and servants
		PresentChain presentChain = new PresentChain();

		System.out.println("Creating servants 1 through 4");
		MinotaurServant servant1 = new MinotaurServant(presentChain);
		MinotaurServant servant2 = new MinotaurServant(presentChain);
		MinotaurServant servant3 = new MinotaurServant(presentChain);
		MinotaurServant servant4 = new MinotaurServant(presentChain);

		// Create threads
		Thread[] threads = new Thread[]{new Thread(servant1), new Thread(servant2), 
										new Thread(servant3), new Thread(servant4)};

		// Start threads
		for (Thread thread : threads)
		{
			thread.start();
		}

		// Wait for the threads to finish
		System.out.println("Begin sorting, servants!");
		for (Thread thread : threads)
		{
			try
			{
				thread.join();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}

		// Calculate the duration of the program
		long endTime = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		long simplifiedTime = totalTime/1000;

		System.out.println("Total runtime is " + totalTime + 
		" milliseconds, or " + simplifiedTime + " seconds.");

		System.out.println("Presents: " + presentsAdded);
		System.out.println("Letters: " + notesSent);
	}
}