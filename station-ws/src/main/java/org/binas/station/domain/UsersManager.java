package org.binas.station.domain;

import org.binas.station.domain.exception.UserNotFoundException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * Class that manages the users in a station
 *
 */
public class UsersManager {

	// Singleton -------------------------------------------------------------

	private UsersManager() {
	}

	/**
	 * SingletonHolder is loaded on the first execution of
	 * Singleton.getInstance() or the first access to SingletonHolder.INSTANCE,
	 * not before.
	 */
	private static class SingletonHolder {
		private static final UsersManager INSTANCE = new UsersManager();
	}

	public static synchronized UsersManager getInstance() {
		return SingletonHolder.INSTANCE;
	}
	
	// ------------------------------------------------------------------------

	
	/**
	 * Map of existing users <email, User>. Uses concurrent hash table
	 * implementation supporting full concurrency of retrievals and high
	 * expected concurrency for updates.
	 */
	private Map<String, User> registeredUsers = new ConcurrentHashMap<>();

	public void addUser(User user){
	    if(user != null && user.getEmail() != null && user.getBalance() >= 0)
	        registeredUsers.put(user.getEmail(), user);
    }
	
	public User getUser(String email) throws UserNotFoundException{
		User user = registeredUsers.get(email);
		if(user == null) {
			throw new UserNotFoundException();
		}
		return user;
	}

	public synchronized void reset() {
		registeredUsers.clear();
	}

	
}