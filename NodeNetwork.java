import java.io.File;
import java.io.FileNotFoundException;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Scanner;

public class NodeNetwork {
	private class QueueItem {
		Node node;
		double dist;
		
		public QueueItem(Node node, double dist) {
			this.node = node;
			this.dist = dist;
		}
	}
	
	private class CustomComparator implements Comparator<QueueItem> {
		@Override
		public int compare(QueueItem o1, QueueItem o2) {
			return Double.compare(o1.dist,o2.dist);
		}
	}
	
	public static class Connection {
		Node from;
		Node to;
		double length;
	}
	
	public static class Node {
		Connection[] connections = new Connection[128];
		int connection_amount = 0;
		int id;
		
		public void connect(Node to, double length) {
			Connection conn = new Connection();
			
			conn.from = this;
			conn.to = to;
			conn.length = length;
			
			connections[connection_amount] = conn;
			connection_amount++;
		}
		
		public double getLength(Node target) {
			if (target == this) return 0;
			for (int i = 0; i < this.connection_amount; i++) {
				Connection c = this.connections[i];
				if (c.to == target)
					return c.length;
			}
			
			return Double.MAX_VALUE;
		}
		
		public Node(int id) {
			this.id = id;
		}
	}
	
	Node[] node_list;
	int node_amount;
	
	public Node getNode(int id) {
		return this.node_list[id];
	}
	
	public NodeNetwork(int size) {
		this.node_amount = size;
		this.node_list = new Node[size];
		
		for (int i = 0; i < size; i++) {
			this.node_list[i] = new Node(i);
		}
	}
	
	public static NodeNetwork networkFromFiles(String stops_file, String transfers_file, String times_file) {
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
		
		stops_reader.nextLine();
		
		while (true) {
			String str = stops_reader.nextLine();
			if (str == null) break;
		}
		
		//NodeNetwork nodes = new NodeNetwork(node_amount);
		
		return null;
	}
}
