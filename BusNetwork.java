import java.io.FileNotFoundException;
import java.time.*;
import java.util.*;

public class BusNetwork {
	// Config values
	private static double direct_route_cost = 1.0;
	private static double direct_transfer_cost = 2.0;
	private static double transfer_time_cost = 0.01;
	
	private static boolean debug_print_enabled = true;
	
	private static void debug_print(String to_print) {
		if (debug_print_enabled)
			System.out.println(to_print);
	}
	
	// PriorityQueue classes (for Dijkstra)
	
	private class QueueItem {
		Stop stop;
		double cost;
		
		public QueueItem(Stop node, double cost) {
			this.stop = node;
			this.cost = cost;
		}
	}
	
	private class DijkstraComparator implements Comparator<QueueItem> {
		@Override
		public int compare(QueueItem o1, QueueItem o2) {
			return Double.compare(o1.cost,o2.cost);
		}
	}
	
	// Utility classes
	
	public static class Connection {
		Stop from;
		Stop to;
		
		double cost;
		int type;
	}
	
	public static class Stop {
		private static String prefix_to_suffix(String raw, String prefix) {
			if (raw.length() < prefix.length()) return raw;
			
			String ret = raw;
			if (ret.substring(0,prefix.length()).equalsIgnoreCase(prefix))
				ret = ret.substring(prefix.length()+1) + " " + prefix;
				
			return ret;
		}
		
		private static String correct_name(String raw_name) {
			raw_name = raw_name.toUpperCase();
			
			raw_name = prefix_to_suffix(raw_name,"FLAGSTOP");
			raw_name = prefix_to_suffix(raw_name,"WB");
			raw_name = prefix_to_suffix(raw_name,"NB");
			raw_name = prefix_to_suffix(raw_name,"SB");
			raw_name = prefix_to_suffix(raw_name,"EB");
			
			return raw_name;
		}
		
		List<Connection> connections = new ArrayList<Connection>();
		int stop_id;
		int node_id;
		int stop_code;
		String TST_key;
		
		String name;
		String desc;
		
		double lat;
		double lon;
		
		String zone;
		
		public void connect(Stop to, double length, int type) {
			Connection conn = new Connection();
			
			conn.from = this;
			conn.to = to;
			conn.cost = length;
			conn.type = type;
			
			connections.add(conn);
		}
		
		public Connection getConnection(Stop target) {
			for (Connection c:connections) {
				if (c.to == target)
					return c;
			}
			
			return null;
		}
		
		public double getCost(Stop target) {
			if (target == this) return 0;
			Connection c = getConnection(target);
			if (c != null)
				return c.cost;
			else
				return Double.MAX_VALUE;
		}
		
		public String toString() {
			return this.name;
		}
		
		public String dataToString() {
			String code_str = (this.stop_code != -1) ? Integer.toString(this.stop_code) : "N/A";
			return this.name + ":\n" + this.desc + "\n" + "Stop ID: " + this.stop_id + "	Stop Code:" + code_str + "\n\nLocation: " + this.lat + "°, " + this.lon + "°\nZone: " + this.zone;
		}
		
		public Stop(int node_id, int stop_id, int stop_code, String name, String desc, double lat, double lon, String zone) {
			this.node_id = node_id;
			this.stop_id = stop_id;
			this.stop_code = stop_code;
			this.TST_key = correct_name(name);
			
			this.name = name;
			this.desc = desc;
			
			this.lat = lat;
			this.lon = lon;
			
			this.zone = zone;
		}
	}
	
	// Paths
	
	public static class Path {
		List<Stop> stops = new ArrayList<Stop>();
		double cost = 0;
		
		public String toString() {
			String ret = "Total cost: " + Double.toString(cost) + "\n";
			
			Stop last_stop = null;
			for (Stop stop: stops) {
				if (last_stop != null) {
					Connection c = last_stop.getConnection(stop);
					if (c.type == 0)
						ret = ret + " -> ";
					else
						ret = ret + " ==> ";
				}
					
				ret = ret + stop.toString();
				last_stop = stop;
			}
			
			return ret.substring(0);
		}
	}
	
	public Path getPath(Stop from, Stop to) { // Finds shortest path between two stops using Dijkstra
		// Setting up priority queue and distance cache
		
		int stop_amount = stop_list.size();
		
		QueueItem[] entry_cache = new QueueItem[stop_amount];
		
		int[] came_from = new int[stop_amount];
		
		for (int i = 0; i < stop_amount; i++) {
			entry_cache[i] = new QueueItem(this.getNode(i),Double.MAX_VALUE);
			came_from[i] = -1;
		}
		
		PriorityQueue<QueueItem> queue = new PriorityQueue<>(new DijkstraComparator());
		
		entry_cache[from.node_id] = new QueueItem(from, 0);
		queue.add(entry_cache[from.node_id]);
		
		// Computing path
		
		while (true) {
			QueueItem entry = queue.poll();
			if (entry == null) return null; // Queue was fully exhausted = no path found
			
			Stop current_stop = entry.stop;
			double current_cost = entry.cost;
			
			if (current_stop == to) break; // Found target stop, exit early
			
			List<Connection> connections = current_stop.connections;
			for (Connection path:connections) {
				Stop to_check = path.to;
				
				QueueItem old_entry = entry_cache[to_check.node_id];
				
				double new_cost = current_cost + path.cost;
				if (old_entry.cost <= new_cost) continue;
				
				QueueItem new_entry = new QueueItem(to_check, new_cost);
				
				queue.remove(old_entry);
				queue.add(new_entry);
				
				entry_cache[to_check.node_id] = new_entry;
				came_from[to_check.node_id] = current_stop.node_id;
			}
		}
		
		// Creating list of stops
		
		Stop current_stop = to;
		
		List<Stop> stops = new ArrayList<Stop>();
		stops.add(current_stop);
		
		while (true) {
			int from_id = came_from[current_stop.node_id];
			if (from_id == -1) break;
			
			current_stop = this.getNode(from_id);
			stops.add(0,current_stop);
		}
		
		// Returning Path object
		
		Path path = new Path();
		
		path.stops = stops;
		path.cost = entry_cache[to.node_id].cost;
		
		return path;
	}
	
	// Trip class
	
	private static class TripComparator implements Comparator<Trip> {
		@Override
		public int compare(BusNetwork.Trip o1, BusNetwork.Trip o2) {
			int time_delta = o1.last_time.compareTo(o2.last_time);
			
			if (time_delta == 0)
				return Integer.compare(o1.id, o2.id); // Sort by ID if time is equal
			else
				return time_delta; // Sort by time
		}
	}
	
	public static class Trip extends Path {
		List<LocalTime> times = new ArrayList<LocalTime>();
		
		int id;
		LocalTime last_time = null;
		
		public Stop getLastStop() {
			if (stops.size() > 0)
				return stops.get(stops.size()-1);
			else
				return null;
		}
		
		public void addStop(Stop stop, LocalTime time) {
			if (times.size() > 0) {
				if (last_time != null && time.compareTo(last_time) < 0) {
					System.out.println("Warning: Added stop to trip going back in time! (" + last_time + " > " + time + ")");
				}
			}
			
			stops.add(stop);
			times.add(time);
			
			if (last_time == null || time.compareTo(last_time) > 0)
				last_time = time;
		}
		
		public Trip(int id) {
			this.id = id;
		}
		
		public String toString() {
			String ret = "Trip " + id + ": ";
			
			for (int i = 0; i < stops.size(); i++) {
				Stop stop = stops.get(i);
				LocalTime time = times.get(i);
				
				ret = ret + "\n	" + time + " @ " + stop.toString();
			}
			
			return ret;
		}
	}
	
	// Main BusNetwork class
	
	List<Stop> stop_list;
	List<Trip> trip_list;
	private TST<Stop> stopSearch;
	
	public Stop getStopById(int id) { // Gets stop by the actual bus id it has
		for (Stop stop:stop_list)
			if (stop.stop_id == id)
				return stop;
		
		return null;
	}
	
	public Stop getNode(int id) { // Gets stop by the internal id we have assigned it (much faster)
		return stop_list.get(id);
	}
	
	public BusNetwork(List<Stop> stops) {
		this.stop_list = stops;
		this.stopSearch = new TST<Stop>();
		
		for (Stop stop:stops) {
			this.stopSearch.put(stop.TST_key, stop);
		}
	}
	
	public List<Stop> searchStops(String search_term) {
		List<Stop> ret = new ArrayList<Stop>();
		
		 for (Object key: stopSearch.keysWithPrefix(search_term)) {
			 Stop stop = (Stop) stopSearch.get((String) key);
			 ret.add(stop);
		 }
		
		return ret;
	}
	
	private static int BinarySearch(List<Trip> list, LocalTime target, int first, int last) {
		int mid = (first + last) / 2;
		
		while (first <= last) {
			if (list.get(mid).last_time.compareTo(target) < 0)
				first = mid+1;
			else if (list.get(mid).last_time.equals(target))
				return mid;
			else
				last = mid-1;
			
			mid = (first + last) / 2;
		}
		
		return -1; // Didn't find element
	}
	
	public List<Trip> getTripsAtTime(LocalTime time) {
		int index = BinarySearch(trip_list,time,0,trip_list.size()-1);
		if (index == -1) // None found, return empty list
			return new ArrayList<Trip>();
		
		int start_index = index;
		while (trip_list.get(start_index-1).last_time.equals(time))
			start_index--;
		
		int end_index = index;
		while (trip_list.get(end_index).last_time.equals(time))
			end_index++;
		
		List<Trip> list = new ArrayList<Trip>();
		
		for (int i = start_index; i < end_index; i++)
			list.add(trip_list.get(i));
		
		return list;
	}
	
	public static BusNetwork networkFromFiles(String stops_file, String transfers_file, String times_file) {
		debug_print("Getting network from files...");
		
		// Initialising readers
		
		FileReader stops_reader;
		FileReader transfers_reader;
		FileReader times_reader;
		
		try {
			stops_reader = new FileReader(stops_file);
			transfers_reader = new FileReader(transfers_file);
			times_reader = new FileReader(times_file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		
		// Skipping headers
		
		stops_reader.nextLine();
		times_reader.nextLine();
		transfers_reader.nextLine();
		
		// Reading stops
		debug_print("Creating stops list...");
		
		List<Stop> stops = new ArrayList<Stop>();
		
		for (int i = 0; true; i++) {
			String str = stops_reader.nextLine();
			if (str == null) break;
			
			String data[] = str.split(",");
			
			int stop_id = Integer.parseInt(data[0]);
			int stop_code;
			try {
				stop_code = Integer.parseInt(data[1]);
			} catch (NumberFormatException e) {
				stop_code = -1;
			}
			
			String name = data[2];
			String desc = data[3];
			
			double lat = Double.parseDouble(data[4]);
			double lon = Double.parseDouble(data[5]);
			
			String zone = data[6];
			
			stops.add(new Stop(i, stop_id, stop_code, name, desc, lat, lon, zone));
		}
		
		// Connecting direct routes
		debug_print("Connecting direct routes...");
		
		BusNetwork network = new BusNetwork(stops);
		
		Stop last_stop = null;
		
		network.trip_list = new ArrayList<Trip>();
		Trip current_trip = new Trip(-1); // Invalid id so is guaranteed to be different
		
		while (true) {
			String str = times_reader.nextLine();
			if (str == null) break;
			
			String data[] = str.split(",");
			
			int trip_id = Integer.parseInt(data[0]);
			int stop_id = Integer.parseInt(data[3]);
			
			LocalTime arrival_time;
			try {
				String raw_t = data[1];
				if (raw_t.charAt(0) == ' ')
					raw_t = "0" + raw_t.substring(1);
				
				arrival_time = LocalTime.parse(raw_t);
			} catch (java.time.format.DateTimeParseException e) {
				arrival_time = LocalTime.MAX;
			}
			
			Stop current_stop = network.getStopById(stop_id);
			
			if (current_trip.id != trip_id) { // New trip
				current_trip = new Trip(trip_id);
				network.trip_list.add(current_trip);
			} else { // Continue current trip
				last_stop.connect(current_stop, BusNetwork.direct_route_cost,0);
			}
			
			current_trip.addStop(current_stop, arrival_time);
			last_stop = current_stop;
		}
		
		network.trip_list.sort(new TripComparator());
		
		// Connecting transfers
		debug_print("Connecting transfers...");
		
		while (true) {
			String str = transfers_reader.nextLine();
			if (str == null) break;
			
			String data[] = str.split(",");
			
			int from_stop_id = Integer.parseInt(data[0]);
			int to_stop_id = Integer.parseInt(data[1]);
			
			Stop from_stop = network.getStopById(from_stop_id);
			Stop to_stop = network.getStopById(to_stop_id);
			
			int transfer_type = Integer.parseInt(data[2]);
			double cost = 0;
			
			if (transfer_type == 0)
				cost = BusNetwork.direct_transfer_cost;
			else if (transfer_type == 1) {
				double min_time = Double.parseDouble(data[3]);
				cost = min_time * BusNetwork.transfer_time_cost;
			}
			
			from_stop.connect(to_stop, cost, 1);
		}
		
		// Returning network
		debug_print("BusNetwork created successfully!");
		
		return network;
	}
}
