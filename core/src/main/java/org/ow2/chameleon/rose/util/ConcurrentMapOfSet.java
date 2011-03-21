package org.ow2.chameleon.rose.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * TODO doc
 * @author barjo
 *
 * @param <T>
 * @param <E>
 */
public class ConcurrentMapOfSet<T, E> {

	private final Map<T, Collection<E>> innermap;

	private final ReentrantReadWriteLock rwlock;

	public ConcurrentMapOfSet() {
		innermap = new ConcurrentHashMap<T, Collection<E>>();
		rwlock = new ReentrantReadWriteLock();
	}

	public int size() {
		rwlock.readLock().lock();
		try {
			return innermap.size();
		} finally {
			rwlock.readLock().unlock();
		}
	}

	public boolean isEmpty() {
		rwlock.readLock().lock();
		try {
			return innermap.isEmpty();
		} finally {
			rwlock.readLock().unlock();
		}
	}

	public boolean containsKey(Object key) {
		rwlock.readLock().lock();
		try {
			return innermap.containsKey(key);
		} finally {
			rwlock.readLock().unlock();
		}
	}

	public Collection<E> remove(Object key) {
		rwlock.writeLock().lock();
		try {
			return innermap.remove(key);
		} finally {
			rwlock.writeLock().unlock();
		}
	}

	public void clear() {
		rwlock.writeLock().lock();
		try {
			innermap.clear();
		} finally {
			rwlock.writeLock().unlock();
		}
	}

	/**
	 * 
	 * @param key
	 * @param value
	 * @return true if the mapOfSet change as the result of the call.
	 */
	public boolean add(T key, E value) {
		boolean retvalue;

		rwlock.writeLock().lock();
		try {
			if (innermap.containsKey(key)) {
				retvalue = innermap.get(key).add(value);
			} else {
				Collection<E> coll = new HashSet<E>();
				coll.add(value);
				innermap.put(key, coll);
				retvalue = true;
			}
		} finally {
			rwlock.writeLock().unlock();
		}

		return retvalue;
	}

	/**
	 * @param key
	 * @param elem
	 * @return true if the removed element was the last of the collection.
	 */
	public boolean remove(T key, E elem) {
		boolean retvalue = false;

		rwlock.writeLock().lock();
		try {

			if (innermap.containsKey(key)) {
				Collection<E> coll = innermap.get(key);
				coll.remove(elem);

				if (coll.isEmpty()) {
					innermap.remove(key);
					retvalue = true;
				}
			} 
			
		} finally {
			rwlock.writeLock().unlock();
		}

		return retvalue;
	}

	public E getElem(T key) {
		rwlock.readLock().lock();
		try {
			if (innermap.containsKey(key)) {
				return innermap.get(key).iterator().next();
			} else {
				return null;
			}
		} finally {
			rwlock.readLock().unlock();
		}
	}
	
	
	/**
	 * @return a view of the keys.
	 */
	public Set<T> keySet(){
		rwlock.readLock().lock();
		try {
			return innermap.keySet();
		} finally{
			rwlock.readLock().unlock();
		}
	}

}
