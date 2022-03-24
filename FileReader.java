import java.io.*;
import java.util.Scanner;

public class FileReader {
	File file;
	Scanner scanner;
	String filename;
	
	public FileReader(String address) throws FileNotFoundException {
		filename = address;
		
		file = new File(filename);
		scanner = new Scanner(file);
	}
	
	String nextLine() {
		try {
			return scanner.nextLine();
		} catch (java.util.NoSuchElementException e) {
			return null;
		}
	}
	
	int fileLength() {
		int lines = 0;
		
		Scanner new_scanner = null;
		
		try {
			new_scanner = new Scanner(file);
			
			while (true) {
				new_scanner.nextLine();
				lines++;
			}
		} catch (FileNotFoundException e) {
			System.out.println("Failed to find file: " + filename);
			e.printStackTrace();
			return 0;
		} catch (java.util.NoSuchElementException e) {}
		
		new_scanner.close();
		
		return lines;
	}
}
