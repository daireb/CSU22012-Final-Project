import java.util.List;
import java.util.Scanner;

public class MainProgramme {
	private static int nextInt(Scanner sc, int max) {
		System.out.print("Enter a number: ");
		while (true) {
			try {
				int ret = sc.nextInt();
				if (ret >= 1 && ret <= max) {
					return ret;
				}
			} finally {}
			
			System.out.println("Please enter a valid number...");
		}
	}
	
	private static BusNetwork.Stop searchStop(Scanner sc, BusNetwork network) {
		while (true) {
			System.out.print("Search for a stop: ");
			String search_term = sc.next().toUpperCase();
			
			List<BusNetwork.Stop> search_results = network.searchStops(search_term);
			
			if (search_results.size() <= 0) { // Failed to find any results
				System.out.println("No results");
				return null;
			}
			
			System.out.println("Choose from the following results:");
			
			int i = 0;
			for (BusNetwork.Stop stop: search_results) {
				i++;
				
				System.out.println(i + ". " + stop.name);
			}
			
			int index = nextInt(sc,search_results.size())-1;
			
			return search_results.get(index);
		}
	}
	
	public static void main(String[] args) {
		BusNetwork network = BusNetwork.networkFromFiles("src/stops.txt", "src/transfers.txt", "src/stop_times.txt");
		Scanner sc = new Scanner(System.in);
		
		while (true) {
			BusNetwork.Stop stop = searchStop(sc,network);
			if (stop == null) continue;
			
			while (true) {
				System.out.println();
				System.out.println("Selected stop: " + stop.name);
				System.out.println("Choose from the following options:\n1. See stop data\n2. Plot route\n3. Select new stop");
				
				int selection = nextInt(sc,3);
				System.out.println();
				
				if (selection == 1) {
					System.out.println(stop.dataToString());
				} else if (selection == 2) {
					BusNetwork.Stop new_stop = searchStop(sc,network);
					BusNetwork.Path path = network.getPath(stop, new_stop);
					
					System.out.println("\n" + path.toString());
				} else {
					break;
				}
			}
		}
		
		//BusNetwork.Stop start = network.getNode(0);
		//BusNetwork.Stop finish = network.getNode(60);
		
		//System.out.println("starting path");
		//BusNetwork.Path path = network.getPath(start, finish);
		
		//System.out.println(path.toString());
		//System.out.println("done");
	}
}
