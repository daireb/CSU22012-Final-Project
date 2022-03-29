import java.io.File;
import java.io.FileNotFoundException;
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
	
	private class CustomComparator implements Comparator<QueueItem> {
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
		List<Connection> connections = new ArrayList<Connection>();
		int stop_id;
		int node_id;
		
		String name;
		
		public void connect(Stop to, double length, int type) {
			Connection conn = new Connection();
			
			conn.from = this;
			conn.to = to;
			conn.cost = length;
			conn.type = type;
			
			connections.add(conn);
			
			//connections[connection_amount] = conn;
			//connection_amount++;
		}
		
		public double getCost(Stop target) {
			if (target == this) return 0;
			for (Connection c:connections) {
				if (c.to == target)
					return c.cost;
			}
			
			return Double.MAX_VALUE;
		}
		
		public String toString() {
			return this.name;
		}
		
		public Stop(int node_id, int stop_id, String name) {
			this.node_id = node_id;
			this.stop_id = stop_id;
			
			this.name = name;
		}
	}
	
	// Paths
	
	public static class Path {
		Stop[] stops = {};
		double cost = 0;
		
		public String toString() {
			String ret = "Total cost: " + Double.toString(cost) + "\n";
			
			for (int i = 0; i < stops.length; i++) {
				ret = ret + stops[i].toString() + " -> ";
			}
			
			return ret;
		}
	}
	
	public Path getPath(Stop from, Stop to) {
		// Setting up priority queue and distance cache
		
		int stop_amount = stop_list.size();
		
		QueueItem[] entry_cache = new QueueItem[stop_amount];
		
		int[] came_from = new int[stop_amount];
		
		for (int i = 0; i < stop_amount; i++) {
			entry_cache[i] = new QueueItem(this.getNode(i),Double.MAX_VALUE);
			came_from[i] = -1;
		}
		
		PriorityQueue<QueueItem> queue = new PriorityQueue<>(new CustomComparator());
		
		entry_cache[from.node_id] = new QueueItem(from, 0);
		queue.add(entry_cache[from.node_id]);
		
		// Computing path
		
		int scanned_count = 0;
		
		while (true) {
			QueueItem entry = queue.poll();
			if (entry == null) break;
			
			Stop current_stop = entry.stop;
			double current_cost = entry.cost;
			
			if (current_stop == to) break; // Found target stop, exit early
			
			scanned_count++;
			if (scanned_count % 1000 == 0)
				debug_print("Scanned " + scanned_count + " nodes");
			
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
		
		int path_length = 1;
		Stop current_stop = to;
		
		List<Stop> stops = new ArrayList<Stop>();
		stops.add(current_stop);
		
		while (true) {
			int from_id = came_from[current_stop.node_id];
			if (from_id == -1) break;
			
			current_stop = this.getNode(from_id);
			stops.add(0,current_stop);
			
			path_length++;
		}
		
		// Returning Path object
		
		Path path = new Path();
		path.stops = stops.toArray(path.stops);
		path.cost = entry_cache[to.node_id].cost;
		
		return path;
	}
	
	// Main BusNetwork class
	
	List<Stop> stop_list;
	
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
		
		//int stop_amount = stops_reader.fileLength()-1;
		List<Stop> stops = new ArrayList<Stop>();
		//Stop[] stops = new Stop[stop_amount];
		
		for (int i = 0; true; i++) {
			String str = stops_reader.nextLine();
			if (str == null) break;
			
			String data[] = str.split(",");
			
			int stop_id = Integer.parseInt(data[0]);
			
			String name = data[2];
			stops.add(new Stop(i, stop_id, name));
		}
		
		// Connecting direct routes
		debug_print("Connecting direct routes...");
		
		BusNetwork network = new BusNetwork(stops);
		
		int current_trip_id = -1; // Guaranteed to be different
		Stop last_stop = null;
		
		while (true) {
			String str = times_reader.nextLine();
			if (str == null) break;
			
			String data[] = str.split(",");
			
			int trip_id = Integer.parseInt(data[0]);
			int stop_id = Integer.parseInt(data[3]);
			
			Stop current_stop = network.getStopById(stop_id);
			
			if (current_trip_id != trip_id) { // New trip
				current_trip_id = trip_id;
			} else { // Continue current trip
				last_stop.connect(current_stop, BusNetwork.direct_route_cost,0);
			}
			
			last_stop = current_stop;
		}
		
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
