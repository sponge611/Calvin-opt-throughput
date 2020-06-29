package org.vanilladb.calvin.cache;

import java.util.LinkedHashMap;
import java.util.Map;

import org.vanilladb.calvin.sql.PrimaryKey;

public class readCache<PrimaryKey, InMemoryRecord> extends LinkedHashMap<PrimaryKey, InMemoryRecord>{
	private int capacity;
	private static final long serialVerionUID = 1L;
	
	public readCache(int capacity) {
		super(16,0.75f,true);
		
		this.capacity = capacity;
		
	}
	
	@Override
	public boolean removeEldestEntry(Map.Entry<PrimaryKey, InMemoryRecord> eldest) {
		return size()>capacity;
	}
}
