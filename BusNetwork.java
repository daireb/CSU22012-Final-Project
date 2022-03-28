import java.io.File;
import java.io.FileNotFoundException;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Scanner;

public class BusNetwork {
	// Config values
	private static double direct_route_cost = 1.0;
	private static double direct_transfer_cost = 2.0;
	private static double transfer_time_cost = 0.01;
	
	// PriorityQueue classes (for A*)
	
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
	}
	
	public static class Stop {
		Connection[] connections = new Connection[128];
		int connection_amount = 0;
		int stop_id;
		int node_id;
		
		String name;
		
		public Connection[] getConnections() {
			Connection[] ret = new Connection[connection_amount];
			for (int i = 0; i < connection_amount; i++) {
				ret[i] = connections[i];
			}
			
			return ret;
		}
		
		public void connect(Stop to, double length) {
			Connection conn = new Connection();
			
			conn.from = this;
			conn.to = to;
			conn.cost = length;
			
			connections[connection_amount] = conn;
			connection_amount++;
		}
		
		public double getCost(Stop target) {
			if (target == this) return 0;
			for (int i = 0; i < this.connection_amount; i++) {
				Connection c = this.connections[i];
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
				if (stops[i] == null) {System.out.println("Failed to find path element " + i); break;}
				ret = ret + stops[i].toString() + " -> ";
			}
			
			return ret;
		}
	}
	
	public Path getPath(Stop from, Stop to) {
		// Setting up priority queue and distance cache
		
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
		
		while (true) {
			QueueItem entry = queue.poll();
			if (entry == null) break;
			
			Stop current_stop = entry.stop;
			double current_cost = entry.cost;
			
			if (current_stop == to) break; // Found target stop, exit early
			
			Connection[] connections = current_stop.getConnections();
			for (int i = 0; i < connections.length; i++) {
				Connection path = connections[i];
				Stop to_check = path.to;
				
				double new_cost = current_cost + path.cost;
				if (entry_cache[to_check.node_id].cost < new_cost) continue;
				
				QueueItem new_entry = new QueueItem(to_check, new_cost);
				
				queue.remove(entry_cache[to_check.node_id]);
				queue.add(new_entry);
				
				entry_cache[to_check.node_id] = new_entry;
				came_from[to_check.node_id] = current_stop.node_id;
			}
		}
		
		// Creating list of stops
		
		int path_length = 1;
		Stop current_stop = to;
		
		Stop[] stops = new Stop[1024];
		stops[0] = current_stop;
		
		while (true) {
			int from_id = came_from[current_stop.node_id];
			if (from_id == -1) break;
			
			current_stop = this.getNode(from_id);
			stops[path_length] = current_stop;
			
			path_length++;
		}
		
		Stop[] new_stops = new Stop[path_length]; // Reversing list order and copying into list of minimal size
		for (int i = 0; i < path_length; i++) {
			new_stops[i] = stops[path_length-i-1];
		}
		
		// Returning Path object
		
		Path path = new Path();
		path.stops = new_stops;
		path.cost = entry_cache[to.node_id].cost;
		
		return path;
	}
	
	// Main BusNetwork class
	
	Stop[] stop_list;
	int stop_amount;
	
	public Stop getStopById(int id) { // Gets stop by the actual bus id it has
		for (int i = 0; i < stop_amount; i++)
			if (stop_list[i].stop_id == id)
				return stop_list[i];
		
		return null;
	}
	
	public Stop getNode(int id) { // Gets stop by the internal id we have assigned it (much faster)
		return stop_list[id];
	}
	
	public BusNetwork(Stop[] stops) {
		this.stop_amount = stops.length;
		this.stop_list = stops;
	}
	
	public static BusNetwork networkFromFiles(String stops_file, String transfers_file, String times_file) {
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
		
		int stop_amount = stops_reader.fileLength()-1;
		Stop[] stops = new Stop[stop_amount];
		
		for (int i = 0; i < stop_amount; i++) {
			String str = stops_reader.nextLine();
			if (str == null) break;
			
			String data[] = str.split(",");
			
			int stop_id = Integer.parseInt(data[0]);
			
			String name = data[2];
			
			stops[i] = new Stop(i, stop_id, name);
		}
		
		// Connecting direct routes
		
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
				last_stop.connect(current_stop, BusNetwork.direct_route_cost);
			}
			
			last_stop = current_stop;
		}
		
		// Connecting transfers
		
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
			
			from_stop.connect(to_stop, cost);
		}
		
		// Returning network
		
		return network;
	}
}
