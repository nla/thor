package thor.util;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A service for running tasks that ensures no two tasks with the same lockId can be run simultaneously by different threads.
 */

public class LockService
{
    private HashMap<String, LockState> locks;
    private ReentrantLock masterLock;

    public LockService()
    {
        locks = new HashMap<String, LockState>();
        masterLock = new ReentrantLock();
    }

    /**
     * Runs a task with the specified lock ID. 
     * It is guaranteed the provided task will not be executed by multiple threads simultaneously for the same lock ID provided.
     *
     * @param lockId    The ID of the lock.
     * @param task      The task to be executed.
     * @return          The object returned by the task.
     */

    public <T> T runLockedTask(String lockId, LockedTask<T> task) throws Exception
    {
        LockState lock;
        masterLock.lock();

        try
        {
            lock = locks.get(lockId);

            if(lock==null)
            {
                lock = new LockState();
                locks.put(lockId, lock);
            }

            lock.incrementCount();
        }
        finally
        {
            masterLock.unlock();
        }

        lock.getLock().lock();

        try
        {
            return task.execute();
        }
        finally
        {
            masterLock.lock();

            try
            {
                lock.decrementCount();

                if(lock.getCount()==0)
                {
                    locks.remove(lockId);
                }
            }
            finally
            {
                masterLock.unlock();
            }

            lock.getLock().unlock();
        }
    }

    private class LockState
    {
        private ReentrantLock lock;
        private int count;

        public LockState()
        {
            this.lock = new ReentrantLock(true);
            this.count = 0;
        }

        public int getCount()
        {
            return count;
        }

        public void incrementCount()
        {
            count++;
        }

        public void decrementCount()
        {
            count--;
        }

        public ReentrantLock getLock()
        {
            return lock;
        }
    }
}