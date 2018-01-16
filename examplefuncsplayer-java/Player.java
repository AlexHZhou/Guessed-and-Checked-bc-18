// import the API.
// See xxx for the javadocs.
import java.util.Random;

import bc.*;

public class Player {
	static GameController gc = new GameController();
	static int round = 0;
	static long teamKarbonite = 100;
	static long totalTime = 0;
    // Connect to the manager, starting the game
	static VecUnit allUnits = null;
	static Team myTeam;
	static Team enemyTeam;
	
	static Planet primaryPlanet;
	static Planet secondaryPlanet;
	static MapLocation earthStartPoint;
	static MapLocation marsStartPoint;
	static MapLocation earthMidPoint;
	static MapLocation guessEnemyEarthStart;
	static MapLocation earthBuildPoint;
	static MapLocation earthDefendPoint;
	static MapLocation standingRallyPoint;
	
	static PlanetMap earthMap = gc.startingMap(Planet.Earth);
	static PlanetMap marsMap= gc.startingMap(Planet.Mars);
	
	static boolean flipRallyPointRatioX = false;
	static boolean flipRallyPointRatioY = false;
	static double rushPowerRequirement = 15d;
	
	
	static int[] tryRotate = new int[]{0,-1,1,-2,2};
	static int[] tryRotate2 = new int[]{0,1,-1,2,-2};
	static Direction[] directions = new Direction[]{Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South,Direction.Southwest, Direction.West, Direction.Northwest};
	//NOTE: Because DirectionToInt is hard coded, you need to change the method if you change directions at all.
	static Random rnd = new Random();
	
	static int buildWorkerCost = 25;
	final static int replicateWorkerCost = 15;
	final static int buildKnightCost = 20;
	final static int buildRangerCost = 20;
	final static int buildMageCost = 20;
	final static int buildHealerCost = 20;
	final static int buildFactoryCost = 100;
	final static int buildRocketCost = 75;
	static int numFactories = 0; 
	static int factoryLimit = 0;
	static int numRockets = 0;
	static int numWorkers = 0;
	static int numKnights = 0;
	static int numRangers = 0;
	static int numMages = 0;
	static int numHealers = 0;
	static int numCombatUnits = 0;

	//static String[] trainOrder = new String[]{"Worker/Mage", "Ranger", "Ranger", "Knight"};
    static String[] trainOrder = new String[]{"Worker", "Knight"};
	//TODO: Need more robust train order. Like more workers,change over time (mage up in value)
	static int buildIndex = 0;
	static boolean garrisoningTroops = false;
	static int inGarrison = 0;
	//need attack priorities, attack map (weights), safe locations, etc.
	static Unit[] priorityTargets = new Unit[4];
	
	
    public static void main(String[] args) {
        rnd.setSeed(42); //some fancy stuff to get same random numbers every time
    	initTeamInfo();
    	initVariables();
    	calculateStartPoints();
    	createWayPoints();
    	
    	researchCureForCancer();
    	
    
        while (true) {
            long startTime = System.nanoTime();
            
            allUnits = gc.myUnits();
            teamKarbonite = gc.karbonite();
            battleStateManagerEarth();
            round++;

            int workers = 0;
        	int factories = 0; 
        	int knights = 0;
        	int rangers = 0;
        	int mages = 0;
        	int healers = 0;
        	int rockets = 0;
        	
            for (int i = 0; i < allUnits.size(); i++){
            	String u = allUnits.get(i).unitType().toString();
            	//System.out.println("Switch is running a " + u);
            	switch (u){
            	case "Worker":
            		workers++;
            	
            		runWorker(allUnits.get(i));
            		break;
            	case "Factory":
            		factories++;
            		runFactory(allUnits.get(i));
            		break; //because factory uses numWorkers, it HAS to be after it.
            	case "Ranger":
            		rangers++;
            		runRanger(allUnits.get(i));
            		break;
            	case "Knight":
            		knights++;
            		runKnight(allUnits.get(i));
            		break;
            	case "Mage":
            		mages++;
        		RunMage(allUnits.get(i));
            		break;
            	case "Healer":
            		healers++;
            		RunHealer(allUnits.get(i));
            		break;
            	case "Rocket":
            		rockets++;
            		runRocket(allUnits.get(i));
            		break;
            	}	
            	
            } 
            long elapsedTimeMilli = (System.nanoTime() - startTime) / 1000000;
            totalTime+=elapsedTimeMilli;
           // System.out.println("Round "+ gc.round() + ". " + totalTime + "ms. Karbonite: " + teamKarbonite);
          
            
            // Submit the actions we've done, and wait for our next turn.
            updateBotCounters(workers, factories, knights, rangers, mages, healers, rockets);
            
            gc.nextTurn();
        }
    }
    
   
    private static void initTeamInfo(){
		myTeam = gc.team();
		allUnits = gc.myUnits();
		
		if (myTeam.equals(Team.Red)) enemyTeam = Team.Blue;
		else enemyTeam = Team.Red;
	}
    private static void initVariables(){
    	if (earthMap.getWidth() > 35) factoryLimit = 3;
    	else if (earthMap.getWidth() > 30) factoryLimit = 2;
    	else factoryLimit = 1;
    }
    private static void calculateStartPoints(){
    	int EavgX = 0;
		int EavgY = 0;
		int MavgX = 0;
		int MavgY = 0;
		int unitsOnEarth = 0;
		int unitsOnMars = 0;
		for (int i = 0; i < allUnits.size(); i++){
			if (allUnits.get(i).location().isOnPlanet(Planet.Earth)){
				EavgX += allUnits.get(i).location().mapLocation().getX();
				EavgY += allUnits.get(i).location().mapLocation().getY();
				unitsOnEarth++;
			}
			else if (allUnits.get(i).location().isOnPlanet(Planet.Mars)){
				MavgX += allUnits.get(i).location().mapLocation().getX();
				MavgY += allUnits.get(i).location().mapLocation().getY();
				unitsOnMars++;
			}
		}
		if (unitsOnEarth > 0){
			EavgX /= unitsOnEarth;
			EavgY /= unitsOnEarth;
		}
		if (unitsOnMars > 0){
			MavgX /= unitsOnMars;
			MavgY /= unitsOnMars;
		}
		
		earthStartPoint = new MapLocation(Planet.Earth, EavgX, EavgY);
		marsStartPoint = new MapLocation(Planet.Mars, MavgX, MavgY);
		
		guessEnemyEarthStart = guessEnemyLocation(earthStartPoint);
		
		int halfX = (int) (earthMap.getWidth() / 2); 
		int halfY = (int) (earthMap.getHeight() / 2);
		earthMidPoint = new MapLocation(Planet.Earth, halfX, halfY);
    }
    public static MapLocation guessEnemyLocation(MapLocation loc){
    	boolean rotationalSymmetry = true;;
    	long newX = earthMap.getWidth() - loc.getX();
    	long newY = earthMap.getHeight() - loc.getY();
    	MapLocation rotational = new MapLocation(Planet.Earth, (int)newX, (int)newY);
    	MapLocation invertedX = new MapLocation (Planet.Earth, loc.getX(), (int) newY);
    	MapLocation invertedY = new MapLocation (Planet.Earth, (int)newX, loc.getY());
    	//TODO: Figure out how calculate symmetry.
    	
    	MapLocation guessEnemy = null;
    	if (loc.getX() > 0.35 * earthMap.getWidth() && loc.getX() < 0.65 * earthMap.getWidth()){
    		guessEnemy = invertedX;
    	}
    	else if (loc.getY() > 0.35 * earthMap.getHeight() && loc.getY() < 0.65 * earthMap.getHeight()){
    		guessEnemy = invertedY;
    	}
    	else guessEnemy = rotational;
    	
    	return guessEnemy;
    	
    }
    public static void createWayPoints(){
    	earthBuildPoint = earthStartPoint; //build location
    	//TODO: make start in corner, not just start point.
    	earthDefendPoint = BetweenMapLocations(earthStartPoint, earthMidPoint, 1, 2); //rally location
    	//earthMidPoint as is
    	//guessEnemyEarthStart; as is
    	
		System.out.println("Attack Rally: " + MapLocationToStringConcise(guessEnemyEarthStart));	
		System.out.println("Mid Point: " + MapLocationToStringConcise(earthMidPoint));
		System.out.println("Defend Rally: " + MapLocationToStringConcise(earthDefendPoint));
		System.out.println("build rally point: "+ MapLocationToStringConcise(earthBuildPoint));
    }
    private static void researchCureForCancer(){
    	gc.queueResearch(UnitType.Worker); //Worker 1
    	if (round > 600) gc.queueResearch(UnitType.Rocket);
    }
	
	private static void updateBotCounters(int w, int f, int k, int r, int m, int h, int rockets){
		numWorkers = w;
		numFactories = f;
		//System.out.println("Number of factories, according to updater: " + numFactories);
		numKnights = k;
		numRangers = r;
		numMages = m;
		numHealers = h;
		numRockets = rockets;
		
		numCombatUnits = numKnights + numRangers + numMages;
		
		if (round % 200 == 0) factoryLimit++;
		//doesn't include healers in count at all.
	}
	

    //TODO: random movement not best idea.
	//TODO: extend research plan, for different strategies too
    private static void runWorker(Unit worker) {
		//TODO: Repair buildings?
		//TODO: Replicate ability
    	//System.out.println("Number of factories, accroding to workers: " + numFactories);
		//Logic order:
		//build factories
		//set blueprint for factories (rocket if round > 600)
		//mine
		//move
    	if (!worker.location().isInGarrison()){
    		
			MapLocation location = worker.location().mapLocation();
			try{
		    	
		    	VecUnit nearby = gc.senseNearbyUnits(location, 2);
		    	
		    	for (int i = 0; i < nearby.size(); i++){ //detect if need to build a blueprint
		    		if (nearby.get(i).unitType().equals(UnitType.Factory) || nearby.get(i).unitType().equals(UnitType.Rocket)){
		    			//not yet built
		    			if (nearby.get(i).structureIsBuilt() == 0){
			    			MapLocation nearbyObjML = nearby.get(i).location().mapLocation();
		    				if (gc.canBuild(worker.id(), nearby.get(i).id())){
		    	    			gc.build(worker.id(), nearby.get(i).id());
		    	                //System.out.println(worker.id() + " helped build stuff.");
		    	                return;
		    	                // move onto the next unit
		    		    	}
		    				else if (location.distanceSquaredTo(nearbyObjML) > 1){
		    	    			fuzzyGoto(worker, nearbyObjML);
		    	    			return;
			    			}
		    			}
		    		}
		    	}
		    	int ARBITRARYBUILDDIRECTION = 2; //
		    	
		    	
		    	if (teamKarbonite > 40){
		    		//&& worker.location().mapLocation().isWithinRange(7, earthBuildPoint)
		    		//dont start building if Karbonite is low
			    	if (location.getPlanet().equals(Planet.Earth) && round > 600){
			    		if (teamKarbonite > buildRocketCost //Blueprint a rocket
				    			&& gc.canBlueprint(worker.id(), UnitType.Rocket, directions[ARBITRARYBUILDDIRECTION])){
			    			for (int i = 0; i < 8; i ++){
					    		gc.blueprint(worker.id(), UnitType.Factory, directions[ARBITRARYBUILDDIRECTION + i]);
					    		//System.out.println(worker.id() + " put down a blueprint for a factory.");
					    		numFactories++;
					    		break;
				    		}
				    	}
			    	} else if (numFactories < factoryLimit &&
			    			teamKarbonite > buildFactoryCost){ //Blueprint a factor)
			    		
			    		for (int i = 0; i < 8; i ++){
				    		gc.blueprint(worker.id(), UnitType.Factory, directions[ARBITRARYBUILDDIRECTION + i]);
				    		System.out.println(worker.id() + " put down a blueprint for a factory." + "on round" + round);
				    		numFactories++;
				    		break;
			    		}
			    	}
		    	}
		    	
			} catch (Exception e){
				System.out.println("Worker blueprinting or building a factory failed.");
			}  
			
			if (worker.location().isOnPlanet(Planet.Earth)){ //doesn't work on Mars.
				for (int searchRadius = 1; searchRadius < 3; searchRadius++){
			    	Direction mineHere = null;
			    	VecMapLocation nearby = gc.allLocationsWithin(location, searchRadius);
			    	for (int j = 0; j < nearby.size(); j++){ //area around worker
			    		if (gc.karboniteAt(nearby.get(j)) > 0){
			    			if (worker.location().mapLocation().distanceSquaredTo(nearby.get(j)) <=1){
				    			mineHere = directions[j];
				    	    	if (gc.canHarvest(worker.id(), mineHere)){
				    	    		gc.harvest(worker.id(), mineHere);
				    	    		//System.out.println(worker.id() + " is a gold digger");
					    			//break;
				    	    		return;
					    			//continue to skip move, break will continue onto move
				    	    	}
			    			}
			    			else{ //distance to karbonite is more than 1
			    				if (gc.isMoveReady(worker.id())){ //move towards karbonite. 
			    		    		fuzzyGoto(worker, nearby.get(j));
			    		    		return;
			    		    	}
			    			}
			    		}
			    	}
				}
			}
			if (gc.isMoveReady(worker.id())){ //move towards karbonite. 
	    		fuzzyGoto(worker, earthBuildPoint); //not actually, but just to get out of the while loop
	    		return;
	    	}
			
			//TODO: need a way to search for karbonite on Mars.
    	}
	}
	
	
	private static void runFactory(Unit factory){
		
	
		if (factory.structureIsBuilt() != 0){ //APPARENTLY FOR SHORTS, 0 = false, !0 = true
			inGarrison = (int) factory.structureGarrison().size();
			
			if (factory.isFactoryProducing() == 0){
				String nextUnit = trainOrder[buildIndex % trainOrder.length];
				if ((int)teamKarbonite > buildKnightCost * 2 && inGarrison < 7){
					
					//System.out.println("Next unit: " + nextUnit);
					
						switch (nextUnit){
							case "Worker":
								if ((int) teamKarbonite > buildWorkerCost && numWorkers < 2){
								gc.produceRobot(factory.id(), UnitType.Worker);
								}
								else gc.produceRobot(factory.id(), UnitType.Knight);
								break;
							//break only inside if statement, so worker falls thru to Ranger when there are too many workers	
							case "Knight":
								gc.produceRobot(factory.id(), UnitType.Knight);
								break;
							case "Ranger":
								gc.produceRobot(factory.id(), UnitType.Ranger);
								break;	
							case "Mage":
								gc.produceRobot(factory.id(), UnitType.Mage);
								break;	
							case "Healer":
								gc.produceRobot(factory.id(), UnitType.Healer);
								break;	
							default: //Hey, I actually use default case!
								System.out.println("Factory decision making feel thru to default case.");
							}	
						buildIndex++;
					
					
				}else {
					System.out.println("Not enough Karbonite to produce unit");
				}
			} 
				
		} 
		if (!garrisoningTroops && inGarrison > 0){
			//TODO: Make unloading better than random. 
			VecMapLocation nearby = gc.allLocationsWithin(factory.location().mapLocation(), 1);
	    	for (int j = 0; j < nearby.size(); j++){ //area around worker

	    			Direction d = factory.location().mapLocation().directionTo(nearby.get(j));
	    			if (gc.canUnload(factory.id(), d)){
	    				 gc.unload(factory.id(), d);
	    				// System.out.println("Factory " + factory.id() + " spat out a unit.");
		    	 		 break;
	    			 }
	    	}
		}
	}

	//not yet implemented
   
	private static void runRocket(Unit rocket) {
		// TODO: write rocket
		System.out.println("Rocket don't exist yet");
	}

	private static void RunHealer(Unit healer) {
		// TODO: write healer
		if (!healer.location().isInGarrison()){
		
		
		
		}
	}

	private static void RunMage(Unit mage) {
		// TODO: write mage
		if (!mage.location().isInGarrison()){
			
			
			
		}
	}

	//TODO: Knight code sucks. Horrendously.
	//Also, could be wayyy more efficient.
	static double knightRetreatHealth = -1; //idk if want to keep this. Seems to work badly.
	private static void runKnight(Unit knight) {
		//structure:
		//check attack
		//determine game state's effect on knight code
		//determine move
		//check attack again.
		//The first atttakc fheck handles both attack-move and move-attack 
		
		if (!knight.location().isInGarrison()){

		boolean isAttackReady = gc.isAttackReady(knight.id());
		String moveType = "fuzzyGoto";
		String attackType = "adjacent";
		MapLocation location = knight.location().mapLocation();
		
		
		//first check if can attack
		if (isAttackReady){
			VecUnit nearby = gc.senseNearbyUnits(location, 1);
	    	for (int i = 0; i < nearby.size(); i++){
				if (gc.canAttack(knight.id(),nearby.get(i).id())){
					gc.attack(knight.id(), nearby.get(i).id());
					isAttackReady = gc.isAttackReady(knight.id());
					break;
				}
	    	}
		}
		
		//handles different cases for battlestates
		if (!battleStateEarth.equals(BSEarth.ATTACK)){
			moveType = "wanderTo";
			//else, moveType = "fuzzyGoto"
		}
		//else moveType = "FuzzyGoto", set by default.
		if (battleStateEarth.equals(BSEarth.ATTACK) || battleStateEarth.equals(BSEarth.SEARCHANDDESTROY)){
			attackType = "seek";
		}
		
		
		//movement
		if (moveType.equals("wanderTo")) {
			wanderTo(knight, standingRallyPoint, 4);
		}else if (attackType.equalsIgnoreCase("seek")){
			VecUnit enemies = gc.senseNearbyUnitsByTeam(location, 3, enemyTeam);
			Unit target = null;
			double closest = 9001;
			int targetIndex = -1;
			for (int i = 0; i < enemies.size(); i++){
				double distToSq = (double)location.distanceSquaredTo(enemies.get(i).location().mapLocation());
				if (distToSq < closest){
					closest = distToSq;
					targetIndex = i;
				}
			}
			if (gc.canMove(knight.id(), knight.location().mapLocation().directionTo(enemies.get(targetIndex).location().mapLocation()))){
				fuzzyGoto(knight, target.location().mapLocation());
			}
		}
		else{ 
			fuzzyGoto(knight, standingRallyPoint);	
		}
		
		//check attack again.
		if (isAttackReady){
			VecUnit nearby = gc.senseNearbyUnits(location, 1);
	    	for (int i = 0; i < nearby.size(); i++){
				if (gc.canAttack(knight.id(),nearby.get(i).id())){
					gc.attack(knight.id(), nearby.get(i).id());
					break;
				}
	    	}
		}
		
	} //dw bout this double bracket
	}
	

	private static void runRanger(Unit ranger) {
		if (!ranger.location().isInGarrison()){
			
			
			
		}
	}

	private static Direction rotate(Direction dir, int amount){
		int dirIndex = DirectionToInt(dir);
		return directions[(dirIndex+amount)%8];
	}
	private static int DirectionToInt(Direction d){
		String direction = d.toString().toUpperCase();
		switch (direction){
		case "NORTH": 
			return 0;
		case "NORTHEAST":
			return 1;
		case "EAST":
			return 2;
		case "SOUTHEAST":
			return 3;
		case "SOUTH":
			return 4;
		case "SOUTHWEST":
			return 5;
		case "WEST":
			return 6;
		case "NORTHWEST":
			return 7;
		default: return -1;
		//returning -1 going to make the array explode. So you better be careful.
		}
	}
 	
	
	public enum BSEarth{ //Stands for BattleStateEarth. Too lazy to type that all out though.
		PREP,
		ADVANCE,
		ATTACK,
		SEARCHANDDESTROY,
		POKE, //unused
		DEFEND,
		KERBALSPACEPROGRAM
	}
	public static BSEarth battleStateEarth = BSEarth.PREP;
	
	//TODO: prob should do this, because knight code sucks. 
	//Also need some kind of "free seek." With current knight code, 
	//after they rush enemy base, they just kinda sit there and do nothing.
	private static void targetPriority(){
		
	}
	
	//TODO: put earth as an argument. 
	//TODO: make somewhat adequete
	private static void battleStateManagerEarth(){ 
		try{
			
		int someValueThatNeedsToBeAdjustedByMapSize = 8;
		int someOtherValueThatNeedsToBeAdjustedByMapSize = 14;
		if (round > 600){
			battleStateEarth = BSEarth.KERBALSPACEPROGRAM;
			standingRallyPoint = earthStartPoint;
		}
		if (battleStateEarth == BSEarth.ATTACK && gc.senseNearbyUnitsByTeam(guessEnemyEarthStart, 4, myTeam).size() > 6){
			battleStateEarth = BSEarth.SEARCHANDDESTROY;
		}
		else if (numCombatUnits > someOtherValueThatNeedsToBeAdjustedByMapSize){
			battleStateEarth = BSEarth.ATTACK;
			standingRallyPoint = guessEnemyEarthStart;
			System.out.println("FOR THE HORDE!!");
		}
		else if (numCombatUnits > someValueThatNeedsToBeAdjustedByMapSize){
			battleStateEarth = BSEarth.ADVANCE;
			standingRallyPoint = earthMidPoint;
		}
		else if (numCombatUnits < someValueThatNeedsToBeAdjustedByMapSize){
			battleStateEarth = BSEarth.PREP;
			standingRallyPoint = earthDefendPoint;
		}

		} catch (Exception e){
			System.out.println("(mucho helpful AttackManager error message)");
		}
		
		//System.out.println("Current battleStateEarth: " + battleStateEarth.toString());
	}
	
	private static void tryMoveRandomDirection(Unit unit){
		try{
			//TODO: add in a emphasis to certain directions (useful for knight wander and worker movement)
			int randDirection = rnd.nextInt(8) + 1; //+1 is to skip "center" option
			
				if (gc.canMove(unit.id(), directions[randDirection])){
					gc.moveRobot(unit.id(), directions[randDirection]);
					//System.out.println(unit.id() + " moved.");
					return;
			}
			
		}catch (Exception e){
			//System.out.println("TryMoveRandomDirection failed. Either unit dead, or code sux.");
		}
		
		
	}
	
	private static void Goto(Unit unit, MapLocation dest){
		try{
			if (gc.isMoveReady(unit.id())){
				Direction d = unit.location().mapLocation().directionTo(dest);
				
				if (gc.canMove(unit.id(), d)){
					gc.moveRobot(unit.id(), d);
				}
			}
		} catch (Exception e){
			System.out.println("Goto failed. Unit probably died.");
		}
	}
	

	//TODO: stinky trail
	private static void fuzzyGoto (Unit unit, MapLocation dest){
		try{
			if (gc.isMoveReady(unit.id())){
				Direction d = unit.location().mapLocation().directionTo(dest);
				
				Direction toward = unit.location().mapLocation().directionTo(dest);
				for (int i = 0; i < tryRotate.length; i++){
					int tilt = tryRotate[i];
					d = rotate(toward, tilt);
					
					if (gc.canMove(unit.id(), d)){
						gc.moveRobot(unit.id(), d); 
						break;
					}
				}
			}
		} catch (Exception e){
			//System.out.println("FuzzyGoto failed on " + unit.id() + ". They might have died.");
		}
	}
	
	private static void wanderTo(Unit unit, MapLocation dest, long wanderRadius){
		try{
			if (gc.isMoveReady(unit.id())){
				if (unit.location().mapLocation().isWithinRange(wanderRadius, dest)){
					tryMoveRandomDirection(unit);
				}
				else{	
					fuzzyGoto(unit, dest);
				}
			}
		} catch (Exception e){
			//System.out.println("wanderTo Failed. Unit probably died.");
		}
	}

	//TODO: Need anti mage move code.

    //some MapLocation utility functions.
    public static MapLocation BetweenMapLocations(MapLocation a, MapLocation b, int weightA, int weightB){
    	MapLocation midPoint = null;
    	Planet mP = null;
    	if (a.getPlanet().equals(b.getPlanet())){
    		mP = a.getPlanet();
    	} else return null;
    	
    	int totalWeight = weightA + weightB;
    	int mX = (a.getX() * weightA + b.getX() * weightB) / totalWeight;
    	int mY = (a.getY() * weightA + b.getY() * weightB) / totalWeight;
    	
    	midPoint = new MapLocation(mP, mX, mY);
    	return midPoint;
    	
    }
    public static String MapLocationToString (MapLocation loc){
    	String s = "Planet " + loc.getPlanet() + ": (" + loc.getX() + ", " + loc.getY() + ")";
    	return s;
    }
    public static String MapLocationToStringConcise (MapLocation loc){
    	String s = "(" + loc.getX() + ", " + loc.getY() + ")";
    	return s;
    }
    private static MapLocation invertMapLoc(MapLocation loc){
		//only works on Earth (Mars not symmetrical)
		if (loc.getPlanet().equals(Planet.Earth)){
			int newX = (int) (earthMap.getWidth() - loc.getX());
			int newY = (int) (earthMap.getHeight() - loc.getY());
			
			MapLocation inverted = new MapLocation(Planet.Earth, newX, newY);
			return inverted;
		}
		else return null;
	}
	
}