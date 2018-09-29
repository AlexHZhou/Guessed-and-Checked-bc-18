import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import bc.*;

public class ProcessedMap {
	int width;
	int height;
	int tileIdIndex = 0;
	ExtraMapInfo[][] mapInfo = null;
	ArrayList<ExtraMapInfo> sortedList = new ArrayList<ExtraMapInfo>();
	PlanetMap map = null;
	Planet planet = null;
	MapLocation startingReferenceLocation;
	MapLocation enemyStart;
	
	private HashMap<Integer, ExtraMapInfo> tileLookup = new HashMap<Integer, ExtraMapInfo>();
	
	//constructors
	public ProcessedMap(){}
	public ProcessedMap(PlanetMap m, Planet p, MapLocation startL, MapLocation enemyL){
		map = m;
		planet = p;
		width = (int) m.getWidth();
		height = (int) m.getHeight();
		mapInfo = new ExtraMapInfo[width + 1][height + 1]; //+1` just in case? idk.
		startingReferenceLocation = startL;
		enemyStart = enemyL;
		
		generateSortedList();
		//BFS();
	}
	
	
	public int karboniteAt(MapLocation loc){
		if (!isOnMap(loc)) return -1;
		return mapInfo[loc.getX()][loc.getY()].getKarbonite();
	}
	public void setKarbonite(MapLocation loc, int val){
		mapInfo[loc.getX()][loc.getY()].setKarbonite(val);;
	}
	public void subtractKarbonite(MapLocation loc, int val){
		mapInfo[loc.getX()][loc.getY()].subtractKarbonite(val);;
	}
	
	public void generateSortedList(){
		for (int x = 0; x < map.getWidth(); x++){
			for (int y = 0; y < map.getHeight(); y++){
				MapLocation temp = new MapLocation(planet, x, y);
				ExtraMapInfo tile = new ExtraMapInfo(temp); //idk how it could be null but...
				mapInfo[temp.getX()][temp.getY()] = tile;
				if (tile.karboniteDeposit > 0) sortedList.add(tile);
				
			}
		}
		Collections.sort(sortedList, Comparator.comparingDouble(ExtraMapInfo::getDistance).reversed());
	}

	//idk if this works lol.
	public void BFS(){
		
		LinkedList<ExtraMapInfo> currentIteration = new LinkedList<ExtraMapInfo>();
		HashSet<String> visited = new HashSet<String>();
		ExtraMapInfo start = new ExtraMapInfo(startingReferenceLocation);
		currentIteration.add(start);
		 System.out.print("currentIteration started with x:" + start.loc.getX() + " y:" + start.loc.getY());
		
		while (!currentIteration.isEmpty()){
			ExtraMapInfo current = currentIteration.remove();
			if (current == null) break;
			
			System.out.println(current.loc.getX() + ", " + current.loc.getY());
			if (current.loc == enemyStart) return;
			
			//prevents a tile from being written over again. kept for now.
			if (visited.contains(current.id)) continue;
			visited.add(current.id);
			
			for (int i = 0; i < current.adjacent.length; i++){
				currentIteration.add(current.adjacent[i]);

				for (int adjacnet = 0; adjacnet < 8;adjacnet++){
					MapLocation nextTo = getLocInDirection(current.loc, i);
					
//					try{
					if (nextTo != null){
						System.out.println("nextTo X: " + nextTo.getX() + " Y: " + nextTo.getY());
						
						//is this gonna infinite loop???

						mapInfo[nextTo.getX()][nextTo.getY()].setDirection(i);
					//}
//					} catch(Exception e){
//						System.out.println("broken api, ignore error message");
					}
					
				}
					
			}
//			for (Tile child : Tile.adjacent){
//				nextToVisit.add(child);
//			}
			//doesn't work because need to assign direction ,and adjacent is not updated currently
		}
		
		//printBFS();
	}
	
	public void printBFS(){
		System.out.println("Printing BFS direction map");
		for (int y = 0; y < map.getHeight(); y++){
			String buildstr ="";
			for (int x = 0; x < map.getWidth(); x++){
				buildstr+=String.format("%1d", mapInfo[x][(int) (map.getHeight()-1-y)].directionInt);
			}
			System.out.println(buildstr);
		}
	}
	
	public MapLocation getLocInDirection(MapLocation loc, int dir){
		int xOffset = 0;
		int yOffset = 0;
		switch (dir){
		case 0:
			yOffset = 1;
			break;
		case 1: 
			xOffset = 1;
			yOffset = 1;
			break;
		case 2:
			xOffset = 1;
			break;
		case 3:
			xOffset = 1;
			yOffset = -1;
			break;
		case 4:
			yOffset = -1;
			break;
		case 5:
			xOffset = -1;
			yOffset = -1;
			break;
		case 6:
			xOffset = -1;
			break;
		case 7:
			xOffset = -1;
			yOffset = 1;
			break;
		}
		int newX = loc.getX() + xOffset;
		int newY = loc.getY() + yOffset;
		MapLocation newLoc = new MapLocation(loc.getPlanet(), 
				newX, 
				newY);	
		
		if (isOnMap(newLoc)) return null;
        else return newLoc;
	}
	
	 
	public class ExtraMapInfo{
		String id;
		ExtraMapInfo[] adjacent = new ExtraMapInfo[8];
		private int directionInt= -1;
		
		MapLocation loc;
		private double distanceToStart;
		private int karboniteDeposit;
		
		public ExtraMapInfo(){}
		public ExtraMapInfo(MapLocation l){
			if (isOnMap(l)){
				loc = l;
				id = loc.getX() + "/" + loc.getY();
				karboniteDeposit = (int) map.initialKarboniteAt(l);
				distanceToStart = (double)l.distanceSquaredTo(startingReferenceLocation);
				
			}
			else System.out.println("Specified location is not on map; cannot make into tile.");
			
			
		}
		
		public String getId(){return id;}
		public double getDistance(){return distanceToStart;}
		public int getKarbonite(){return karboniteDeposit;}
		public void addKarboniteTo(int amount){karboniteDeposit += amount;}
		public void subtractKarbonite(int amount) {karboniteDeposit -= amount;}
		public void setKarbonite(int amount) {karboniteDeposit = amount;}
		public int getDirection(){return directionInt;}
		public void setDirection(int val){directionInt = val;}
		
	}
	public boolean isOnMap(MapLocation loc){
		
		if (loc == null 
				|| loc.getX() >= map.getWidth() 
				|| loc.getX() < 0 
				|| loc.getY() >= map.getHeight() 
				|| loc.getY() <= 0) return false;
		else return true;
	}
}
