import bc.*;
public class PlanetManager {
	static GameController gc = new GameController();
	Planet initPlanet;
	Planet secondPlanet;
	PlanetMap earthMap = gc.startingMap(Planet.Earth);
	PlanetMap marsMap = gc.startingMap(Planet.Mars);
	
	public PlanetManager(){
		
	}
	
	boolean flipRallyX = false;
	boolean flipRallyY = false;
	public MapLocation calcEnemyStart(VecUnit allUnits){
		int avgX = 0;
		int avgY = 0;
		for (int i = 0; i < allUnits.size(); i++){
			avgX += allUnits.get(i).location().mapLocation().getX();
			avgY += allUnits.get(i).location().mapLocation().getY();
		}
		
		if (allUnits.size() == 0){
			avgX = 0;
			avgY = 0;
		} else{
			avgX /= allUnits.size();
			avgY /= allUnits.size();
		}
//		
//		if (avgX > (int) 1/2 * earthMap.getWidth()) flipRallyX = true;
//		if (avgY > (int) 1/2 * earthMap.getHeight()) flipRallyX = true;
		
		MapLocation enemyStart = new MapLocation(invert(new MapLocation(Planet.Earth, avgX, avgY)));
		return enemyStart;
		
		if (gc.planet().toString().equalsIgnoreCase("Earth")){ // Start on Earth
    		primaryPlanet = Planet.Earth;
    		secondaryPlanet = Planet.Mars;
    		homeStart = new MapLocation(primaryPlanet, avgX, avgY);
    		guessEnemyStart = invertMapLoc(homeStart);
    		
    		System.out.println("HomeStart: (" + homeStart.getX() + ", " + homeStart.getY() + ")");
    		System.out.println("guessEnemyStart: (" + guessEnemyStart.getX() + ", " + guessEnemyStart.getY() + ")");
    		
    		//inverting mapLoc to guess enemy start only works on Earth, bc Mars not symmetrical
    		
    		//TODO: so we on earth. What's the plan now?

//				wayPoints = new MapLocation[(int)(earthMap.getWidth() + earthMap.getHeight()) / 2 / 10];
//				//scales how many waypoints there are with size.
    	}
    	else { // Start on Mars
    		primaryPlanet = Planet.Mars;
    		secondaryPlanet = Planet.Earth;
    		homeStart = new MapLocation(primaryPlanet, avgX, avgY);
    		
    		guessEnemyStart = null; //idk if I wanna make this better.
    		//TODO: do we even want to go back to earth (farming simulator? or stay on Mars?)
    	}
		
		try{
			//TODO: only handleds flipping horizontally, not vertically
			if (!flipRallyPointRatioX){
				defendRallyPoint = new MapLocation(primaryPlanet, 
						(homeStart.getX()+ guessEnemyStart.getX()) * 1/3,
						(homeStart.getY() + guessEnemyStart.getY()) * 1/3); 
				//rather aggressive, consider putting back to 1/3
				
				buildRallyPoint = new MapLocation(primaryPlanet,
						defendRallyPoint.getX() / 2,
						defendRallyPoint.getY() / 2
						);
				midPoint = defendRallyPoint = new MapLocation(primaryPlanet, 
						(homeStart.getX()+guessEnemyStart.getX()) / 2,
						(homeStart.getY() + guessEnemyStart.getY()) / 2); 
				
				attackRallyPoint = new MapLocation(primaryPlanet,
						(homeStart.getX() + guessEnemyStart.getX()) * 3/2,
						(homeStart.getY() + guessEnemyStart.getY()) * 3/2);
				//aka 3/4ths to enemy
				
			}
			else{ //do need to flip x
				defendRallyPoint = new MapLocation(primaryPlanet, 
						(homeStart.getX()+ guessEnemyStart.getX()) * 3/4,
						(homeStart.getY() + guessEnemyStart.getY()) * 3/4); 
				
				buildRallyPoint = new MapLocation(primaryPlanet,
						defendRallyPoint.getX() * 7/6,
						defendRallyPoint.getY() * 7/6
						); //the math works for 7/6ths I swear.
				
				midPoint = defendRallyPoint = new MapLocation(primaryPlanet, 
						(homeStart.getX() + guessEnemyStart.getX()) / 2,
						(homeStart.getY() + guessEnemyStart.getY()) / 2); 
				
				attackRallyPoint = new MapLocation(primaryPlanet,
						(homeStart.getX() + guessEnemyStart.getX()) * 1/4,
						(homeStart.getY() + guessEnemyStart.getY()) * 1/4);
			}
		
			if (earthMap.getWidth() > 35) factoryLimit = 4;
			else if (earthMap.getWidth() < 30) factoryLimit = 3;
			else factoryLimit = 2;
			
		System.out.println("attack Rally: (" + attackRallyPoint.getX() + ", " + attackRallyPoint.getY() + ")");		
		System.out.println("mid point: (" + midPoint.getX() + ", " + midPoint.getY() + ")");
		System.out.println("defend Rally: (" + defendRallyPoint.getX() + ", " + defendRallyPoint.getY() + ")");
		System.out.println("build rally point: (" + buildRallyPoint.getX() + ", " + buildRallyPoint.getY() + ")");
		
		//slightly less that 2/3s there.
		//TODO: this scales badly to large maps. 
		//Need map strategy.
		
		} catch (Exception e){
			System.out.println("Somehow, planet detection failed.");
		}
	}
	public MapLocation invert(MapLocation loc){ //assumes Earth
		long newx = earthMap.width-loc.x
		long newy = earthMap.height-loc.y
		return bc.MapLocation(bc.Planet.Earth, newx,newy)
	}
}
