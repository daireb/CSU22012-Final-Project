import java.util.List;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Scanner;

public class MainProgramme {
	private static Scanner sc = new Scanner(System.in);
	
	private static String next() {
		if (!sc.hasNextLine()) {
			System.out.println("Fatal error in scanner");
			return null;
		}
		
		return sc.nextLine();
	}
	
	private static int nextInt(int max) {
		System.out.print("Enter a number: ");
		while (true) {
			String str = next();
			
			try {
				int ret = Integer.parseInt(str);
				if (ret > 0 && ret <= max)
					return ret;
				else
					System.out.println("Please enter a number between 1 and " + max);
			} catch (NumberFormatException e) {
				System.out.println("Please enter a valid number...");
			}
		}
	}
	
	private static BusNetwork.Stop searchStop(BusNetwork network) {
		while (true) {
			System.out.print("Search for a stop: ");
			String search_term = next().toUpperCase();
			
			List<BusNetwork.Stop> search_results = network.searchStops(search_term);
			
			if (search_results.size() <= 0) { // Failed to find any results
				System.out.println("No results");
				return null;
			}
			
			System.out.println("\nChoose from the following results:");
			
			int i = 0;
			for (BusNetwork.Stop stop: search_results) {
				i++;
				
				System.out.println(i + ". " + stop.name);
			}
			
			System.out.println();
			int index = nextInt(search_results.size())-1;
			
			return search_results.get(index);
		}
	}
	
	public static void doStops(BusNetwork network) {
		System.out.println();
		BusNetwork.Stop stop = searchStop(network);
		if (stop == null) return;
		
		while (true) {
			System.out.println();
			System.out.println("Selected stop: " + stop.name);
			System.out.println("Choose from the following options:\n1. See stop data\n2. Plot route\n3. Exit to main menu\n");
			
			int selection = nextInt(3);
			
			if (selection == 1) {
				System.out.println();
				System.out.println(stop.dataToString());
			} else if (selection == 2) {
				System.out.println();
				BusNetwork.Stop new_stop = searchStop(network);
				if (new_stop == null) continue;
				
				BusNetwork.Path path = network.getPath(stop, new_stop);
				
				System.out.println("\n" + path.toString());
			} else {
				break;
			}
		}
	}
	
	private static int findFirstChar(String str, char c) {
		for (int i = 0; i < str.length(); i++)
			if (str.charAt(i) == c)
				return i;
		return -1;
	}
	
	public static void doArrivalTimes(BusNetwork network) {
		while (true) {
			System.out.print("\nType \"exit\" to return to the main menu.\nEnter a time: ");
			String search_term = next();
			
			if (search_term.length() >= 4 && search_term.substring(0,4).toLowerCase().equals("exit"))
				return;
			
			LocalTime time;
			try {
				if (findFirstChar(search_term,':') < 2)
					search_term = "0" + search_term;
				
				time = LocalTime.parse(search_term);
			} catch (java.time.format.DateTimeParseException e) {
				System.out.println("Time was formatted incorrectly.");
				continue;
			}
			
			DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
			
			System.out.println("Searching for all trips arriving at " + time.format(formatter) + "...\n");
			
			List<BusNetwork.Trip> trips = network.getTripsAtTime(time);
			
			if (trips.size() > 0) {
				System.out.println("Trips ending at " + time + ": ");
				for (int i = 0; i < trips.size(); i++)
					System.out.println((i+1) + ". Trip id " + trips.get(i).id);
				
				System.out.println("\nEnter a number to see trip details.");
				int index = nextInt(trips.size())-1;
				
				BusNetwork.Trip trip = trips.get(index);
				System.out.println("\n" + trip.toString());
			}
			else
				System.out.println("No trips found ending at " + time);
		}
	}
	
	public static void main(String[] args) {
		String stops_file = args.length > 0 ? args[0] : "src/stops.txt";
		String transfers_file = args.length > 1 ? args[1] : "src/transfers.txt";
		String stop_times_file = args.length > 2 ? args[2] : "src/stop_times.txt";
		
		BusNetwork network = BusNetwork.networkFromFiles(stops_file, transfers_file, stop_times_file);
		if (network == null) {
			System.out.println("Error finding file, exiting programme...");
			next(); // Requires user to press enter
			return;
		}
		
		while (true) {
			System.out.println("\nChoose from the following options:\n1. Search for stop\n2. Find trips by arrival time\n3. Exit programme\n");
			int selection = nextInt(3);
			
			if (selection == 1)
				doStops(network);
			else if (selection == 2)
				doArrivalTimes(network);
			else
				break;
		}
		
		System.out.println("Exiting programme...");
		next();
	}
}
