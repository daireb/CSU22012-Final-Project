
public class MainProgramme {
	public static void main(String[] args) {
		BusNetwork network = BusNetwork.networkFromFiles("src/stops.txt", "src/transfers.txt", "src/stop_times.txt");
		
		BusNetwork.Stop start = network.getNode(0);
		BusNetwork.Stop finish = network.getNode(60);
		
		System.out.println("starting path");
		BusNetwork.Path path = network.getPath(start, finish);
		
		System.out.println(path.toString());
		System.out.println("done");
	}
}
