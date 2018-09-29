// import the API.
// See xxx for the javadocs.
import java.util.Random;

//import FloydWarshsall.MyMap.MyMapNode;
import bc.Direction;
import bc.GameController;
import bc.MapLocation;
import bc.Planet;
import bc.PlanetMap;
import bc.Team;
import bc.Unit;
import bc.UnitType;
import bc.VecMapLocation;
import bc.VecUnit;
import bc.VecUnitID;

//General TODO:
//////-Mage: don't shoot yourself. See knights committing sudoku. But that only works on adjacent people if there another dude two away, because you cannot shoot at empty spots
//-research
//-better path finding (split areas into large blocks?)
//Pathfinding+give rangers space to kite?
//workers getting stuc trying to get unreachable karbonite.


public class Player {
	static GameController gc = new GameController();
	static int round = 0;
	static long teamKarbonite = 100;
	static long totalTime = 0;
    // Connect to the manager, starting the game
	static VecUnit allUnits = null;
	static Team myTeam;
	static Team enemyTeam;
	
	public static MapLocation earthStartPoint;
	static MapLocation marsStartPoint;
	static MapLocation marsStartPointFlipped;
	static MapLocation earthMidPoint;
	static MapLocation guessEnemyEarthStart;
	static MapLocation earthBuildPoint;
	static MapLocation earthDefendPoint;
	static MapLocation standingRallyPoint;
	static MapLocation standingDefenseRallyPoint;
	
	static PlanetMap earthMap = gc.startingMap(Planet.Earth);
	static PlanetMap marsMap= gc.startingMap(Planet.Mars);
	
	static boolean startLowerX = true;
	static boolean startLowerY = true;
	static double rushPowerRequirement = 15d;
	
	
	final static int[] tryRotate = new int[]{0,-1,1,-2,2, 3, -3};
	final static int[] tryRotate2 = new int[]{0,1,-1,2,-2, -3, 3};
	final static Direction[] directions = new Direction[]{Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South,Direction.Southwest, Direction.West, Direction.Northwest};
	//NOTE: Because DirectionToInt is hard coded, you need to change the method if you change directions at all.
	static Random rnd = new Random();
	
	static int buildWorkerCost = 50;
	final static int replicateWorkerCost = 60;
	final static int buildKnightCost = 40;
	final static int buildRangerCost = 40;
	final static int buildMageCost = 40;
	final static int buildHealerCost = 40;
	final static int buildFactoryCost = 200;
	final static int buildRocketCost = 150;
	static int numFactories = 0; 
	static int numFactoriesMars = 0;
	static boolean structureNeedsBuilding = false;
	static int factoryLimit = 0;
	static int numRockets = 0;
	static boolean rocketAvailible = false;
	static int maxNumRockets = 6;
	static int numWorkers = 0;
	static int numKnights = 0;
	static int numRangers = 0;
	static int numMages = 0;
	static int numHealers = 0;
	static int numCombatUnits = 0;
	static final int maxNumUnits = 30;

	final static int workerAttRange = 0;
	final static int knightAttRange = 2;
	final static int rangerAttRange = 50;
	final static int mageAttRange = 30;
	final static int healerAttRange = 30;
	
	
	static int healerValue = 100;
	static int mageValue = 80;
	static int rangerValue = 60;
	static int knightValue = 50;
	static int workerValue = 40;
	static int factoryValue = 35;
	static int rocketValue = 49;

	static int workerMineRate = 3;
	static int maxWorkers = 4;
	static int karbWayPointIndex = -1;
	static int karbIndexChange = -1;
	
	//static String[] trainOrder = new String[]{"Worker/Mage", "Ranger", "Ranger", "Knight"};
    static final String[] trainOrder = new String[]{"Ranger", "Ranger","Ranger", "Ranger", "Healer"}; //HEALER IS BROKEN
	//TODO: Need more robust train order. Like more workers,change over time (mage up in value)
	static int buildIndex = 0;
	static boolean garrisoningTroops = false;
	static int inGarrison = 0;
	
	//juicy is priority targets.
	static Unit[] juicyTargets = new Unit[5];
	static int juicyIndex = 0;
	//already attacked units, by priority
	static Unit[] attackedLowPT = new Unit[5];
	static int LPTIndex = 0;
	static Unit[] attackedHighPT = new Unit[3];
	static int HPTIndex = 0;
	
	static final int[] criticalHealthValues = new int[]{20,  25, 10, 40};
	
	static ProcessedMap ProcessedMapEarth = null;
	static ProcessedMap ProcessedMapMars = null;
	
	final static long sqrt2 = (long) 1.42;
	
    public static void main(String[] args) {
        rnd.setSeed(42); //some fancy stuff to get same random numbers every time
    	initTeamInfo();
    	calculateStartPoints();
    	createWayPoints();
    	initMaps();
    	initVariables();
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
            	switch (u){
            	case "Rocket":
            		rockets++;
            		runRocket(allUnits.get(i));
            		break;
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
            	
            	}	
            	
            } 
            long elapsedTimeMilli = (System.nanoTime() - startTime) / 1000000;
            totalTime+=elapsedTimeMilli;
            //System.out.println("Round "+ gc.round() + ". " + totalTime + "ms. Karbonite: " + teamKarbonite);
         
            
            // Submit the actions we've done, and wait for our next turn.
            updateBotCounters(workers, factories, knights, rangers, mages, healers, rockets);
            
            if (round % 50 == 0){
            	//System.out.println("Cleaning up memory");
                System.runFinalization(); //for more memory problems
            	System.gc(); //for memory problems
            }
            gc.nextTurn();
        }
    }
    
    private static void initTeamInfo(){
		myTeam = gc.team();
		allUnits = gc.myUnits();
		
		if (myTeam.equals(Team.Red)) enemyTeam = Team.Blue;
		else enemyTeam = Team.Red;
		
		System.out.println("My team: " + myTeam.toString());
		System.out.println("Enemy Team: " + enemyTeam.toString());
	}
    private static void initVariables(){
    	maxWorkers = (int) ((earthMap.getWidth() + earthMap.getHeight()) / 15) - 1;
    	System.out.println("Max workers: " + maxWorkers);
    	//this varies by team, so needs below check;
		karbWayPointIndex = -1;
		if (earthStartPoint.distanceSquaredTo(ProcessedMapEarth.sortedList.get(0).loc) < earthStartPoint.distanceSquaredTo(ProcessedMapEarth.sortedList.get(ProcessedMapEarth.sortedList.size() - 1).loc)){
			karbWayPointIndex = 0;
			karbIndexChange = 1;
		}
		else karbWayPointIndex = ProcessedMapEarth.sortedList.size() - 1;
    }
    private static void calculateStartPoints(){
    	int EavgX = 0;
		int EavgY = 0;
		int unitsOnEarth = 0;
		for (int i = 0; i < allUnits.size(); i++){
			if (allUnits.get(i).location().isOnPlanet(Planet.Earth)){
				EavgX += allUnits.get(i).location().mapLocation().getX();
				EavgY += allUnits.get(i).location().mapLocation().getY();
				unitsOnEarth++;
			}
		}
		if (unitsOnEarth > 0){
			EavgX /= unitsOnEarth;
			EavgY /= unitsOnEarth;
		}
		
		
		earthStartPoint = new MapLocation(Planet.Earth, EavgX, EavgY);
		marsStartPoint = new MapLocation(Planet.Mars, (int)(marsMap.getWidth() / 4), (int)(marsMap.getHeight() / 4));
		marsStartPointFlipped = invertMapLoc(marsStartPoint);
		//starts us on the bottom right on mars. popular land location probably though,
		//TODO: make marsStart point better.
		guessEnemyEarthStart = guessEnemyLocation(earthStartPoint);
		
		int halfX = (int) (earthMap.getWidth() / 2); 
		int halfY = (int) (earthMap.getHeight() / 2);
		earthMidPoint = new MapLocation(Planet.Earth, halfX, halfY);
		if (earthStartPoint.getX() > halfX) startLowerX = false;
		if (earthStartPoint.getY() > halfY) startLowerY = false;
    }
    public static MapLocation guessEnemyLocation(MapLocation loc){
    	long newX = earthMap.getWidth() - loc.getX();
    	long newY = earthMap.getHeight() - loc.getY();
    	
    	//TODO: Figure out how calculate symmetry.
    	
    	MapLocation guessEnemy = null;
    	if (loc.getX() > 0.35 * earthMap.getWidth() && loc.getX() < 0.65 * earthMap.getWidth()){
    		MapLocation invertedX = new MapLocation (Planet.Earth, loc.getX(), (int) newY);
    		guessEnemy = invertedX;
    	}
    	else if (loc.getY() > 0.35 * earthMap.getHeight() && loc.getY() < 0.65 * earthMap.getHeight()){
    		MapLocation invertedY = new MapLocation (Planet.Earth, (int)newX, loc.getY());
    		guessEnemy = invertedY;
    	}
    	else {
    		MapLocation rotational = new MapLocation(Planet.Earth, (int)newX, (int)newY);
    		guessEnemy = rotational;
    	}
    	
    	return guessEnemy;
    	
    }
    public static void initMaps(){
    	//TODO: Gameplan based on availibe amounts of karbontie and locations of them?
    	//moveMapEarth = new ProcessedMap(earthMap);
    	//TODO: need mars version?
    	ProcessedMapEarth = new ProcessedMap(earthMap, Planet.Earth, earthStartPoint, guessEnemyEarthStart);
    	//ProcessedMapMars = new ProcessedMap(marsMap, Planet.Mars, marsStartPoint, marsStartPointFlipped);
    	
    	//TODO: missing max's thing about ordering the karboniteMap
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
    //TODO: extend research plan, for different strategies too    
    private static void researchCureForCancer(){
    	gc.queueResearch(UnitType.Worker); //25 turns
    	gc.queueResearch(UnitType.Ranger); //25 turns
    	gc.queueResearch(UnitType.Healer); //25 turns
    	gc.queueResearch(UnitType.Ranger); //100 turns
    	gc.queueResearch(UnitType.Healer); //25 turns
    	gc.queueResearch(UnitType.Rocket); //50 turns
    	//total: 250 turns of research.
    	if (round > 26) workerMineRate++; //effect of Worker Upgrade 1
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
		//doesn't include healers in count at all.
		
		
		//reset targetting list, in case something died.
		juicyIndex = 0;
		for (int i = 0; i < juicyTargets.length; i++){
			juicyTargets[i] = null;
		}
		HPTIndex = 0;
		for (int i = 0; i < attackedHighPT.length; i++){
			attackedHighPT[i] = null;
		}
		LPTIndex = 0;
		for (int i = 0; i < juicyTargets.length; i++){
			juicyTargets[i] = null;
		}
		
		MapLocation temp = ProcessedMapEarth.sortedList.get(karbWayPointIndex).loc;
		if (ProcessedMapEarth.mapInfo[temp.getX()][temp.getY()].getKarbonite() <= 1){
			karbWayPointIndex+=karbIndexChange;
			System.out.println("Karbonite Waypoint Changed to : " + karbWayPointIndex);
		}
	}
	

	//TODO: need to optimize starting
    private static void runWorker(Unit worker) {
		//TODO: Replicate ability
		//Logic order:
		//build factories
		//set blueprint for factories (rocket if round > 600)
		//mine
		//move
    	if (!worker.location().isInGarrison()){

			MapLocation location = worker.location().mapLocation();
			int uid = worker.id();
			
    		if (numWorkers < maxWorkers && teamKarbonite > 20){
    			for (int i = 0; i < 7; i++) { //tries to replicate in a direction
    				int temp = rnd.nextInt(8);
    				if (gc.canReplicate(uid, directions[temp])){
    					 gc.replicate(uid, directions[temp]);
    					 numWorkers++;
    					 teamKarbonite -= replicateWorkerCost;
    					 break;
    				}
    			}
    		}
    		
	    	if (structureNeedsBuilding){
	    	VecUnit nearby = gc.senseNearbyUnits(location, 10);
		    	for (int i = 0; i < nearby.size(); i++){ //detect if need to build a blueprint
		    		if (nearby.get(i).unitType().equals(UnitType.Factory) || nearby.get(i).unitType().equals(UnitType.Rocket)){
		    			//not yet built
		    			if (nearby.get(i).structureIsBuilt() == 0){
			    			int tid = nearby.get(i).id();
		    				MapLocation nearbyObjML = nearby.get(i).location().mapLocation();
		    				if (gc.canBuild(uid, tid)){
		    	    			gc.build(uid, tid);
		    	                //System.out.println(uid + " helped build stuff.");
		    	               
		    	                //avoid any other movement for a turn.
		    		    	}
		    				else if (location.distanceSquaredTo(nearbyObjML) > 1){
		    	    			if (gc.isMoveReady(uid)){
		    	    				bugGoto(worker, nearbyObjML);

		    	    			}
			    			}
		    			}
		    		}
		    	}
	    	}
	    	int ARBITRARYBUILDDIRECTION = 0; //arbitrarily build to the left.
	    	//TODO: Don't build factories next to each other.
	    	int HARDCODEDROCKETBUILDTIME = 300;
	try{
	    	if (location.getPlanet().equals(Planet.Earth)){
		    	if (round > HARDCODEDROCKETBUILDTIME){ //at 300, start building rockets. 
		    		if (teamKarbonite > buildRocketCost 
		    				&& numRockets < maxNumRockets){
		    			for (int i = 0; i < 8; i ++){
				    		if (gc.canBlueprint(worker.id(), UnitType.Rocket, directions[ARBITRARYBUILDDIRECTION])){
			    				gc.blueprint(worker.id(), UnitType.Rocket, directions[ARBITRARYBUILDDIRECTION + i]);
					    		System.out.println(worker.id() + " put down a blueprint for a rocket.");
					    		numRockets++;
					    		teamKarbonite -= buildRocketCost;
					    		break;
				    		}
			    		}
			    	}
		    	} else if (teamKarbonite > buildFactoryCost + 20 * numFactories ){ 
		    		//Blueprint a factor if its not time to build rockets.
	    			//need different condition for early game
		    		
		    		for (int i = 0; i < 7; i ++){ //fix this random build direction
			    		if (gc.canBlueprint(worker.id(), UnitType.Factory, directions[ARBITRARYBUILDDIRECTION + i])){
			    			gc.blueprint(worker.id(), UnitType.Factory, directions[ARBITRARYBUILDDIRECTION + i]);
			    			numFactories++;
			    			teamKarbonite -= buildFactoryCost;
				    		break;
			    		}
			    		//System.out.println(worker.id() + " put down a blueprint for a factory." + "on round" + round);
			    		
		    		}
		    	}
	    	}
	    	//else on mars:
	    	else if (teamKarbonite > buildFactoryCost + 100 * numFactoriesMars){
	    		for (int i = 0; i < 8; i ++){
		    		if (gc.canBlueprint(worker.id(), UnitType.Factory, directions[ARBITRARYBUILDDIRECTION + i])){
		    			gc.blueprint(worker.id(), UnitType.Factory, directions[ARBITRARYBUILDDIRECTION + i]);
		    			numFactoriesMars++;
		    			teamKarbonite -= buildFactoryCost;
			    		break;
		    		}
		    		
	    		}
	    	}
	} catch (Exception e){
		System.out.println("System says I don't have enough karbonite, even though I have checks...?");
	}
	
			if (worker.location().isOnPlanet(Planet.Earth)){  //search for nearbyKarbonite
				if (battleStateEarth.equals(BSEarth.KERBALSPACEPROGRAM) && gc.isMoveReady(worker.id())){
					bugGoto(worker, earthStartPoint);
				}
			}
				
			
			workerSearchAroundForKarbonite(worker, 10);
			//this doesn't work well for mars
    	}
	}

	private static void workerSearchAroundForKarbonite(Unit worker, int radiusSquared){ ///search range of 3 is hardcoded
		MapLocation loc = worker.location().mapLocation();
		int uid = worker.id();
		//TODO: re-optimize this, with expanding radius. 
		//for (int searchRadius = 1; searchRadius < radiusSquared; searchRadius++){
    	Direction mineHere = null;
    	//first check any adjacent.
    	VecMapLocation nextTo = gc.allLocationsWithin(loc, sqrt2);
    	for (int j = 0; j < nextTo.size(); j++){ //area around worker
    		MapLocation searchSquare = nextTo.get(j);
    		if (ProcessedMapEarth.karboniteAt(searchSquare) > 0){
    			mineHere = loc.directionTo(searchSquare);
    			if (gc.canHarvest(uid, mineHere)){
    	    		gc.harvest(uid, mineHere);
    	    		ProcessedMapEarth.subtractKarbonite(searchSquare, workerMineRate);
    	    		//System.out.println(worker.id() + " is mining");
	    			//break;
    	    		return;
	    			//doesn't move after mining
    	    	}
    		}
    	}
    	
    	VecMapLocation nearby = gc.allLocationsWithin(loc, radiusSquared);
    	for (int j = 0; j < nearby.size(); j++){ //area around worker
    		MapLocation searchSquare = nearby.get(j);
    		if (ProcessedMapEarth.karboniteAt(searchSquare) > 0){
    			mineHere = loc.directionTo(searchSquare);
    			if (gc.canHarvest(uid, mineHere)){
    	    		gc.harvest(uid, mineHere);
    	    		ProcessedMapEarth.subtractKarbonite(searchSquare, workerMineRate);
    	    		//System.out.println(worker.id() + " is mining");
	    			//break;
    	    		return;
	    			//doesn't move after mining
    	    	}
    			else{ //cannot mine, but can see karbonite
    				if (gc.isMoveReady(uid)){ //move towards karbonite. 
    		    		fuzzyGoto(worker, searchSquare);
    		    		//System.out.println(worker.id() + " is looking for karbonite");
    		    		return;
    		    	}
    			}
    		}
    	}
		//}
		//if no karbonite nearby:
		
		if (gc.isMoveReady(uid)){
			if (worker.location().isOnPlanet(Planet.Earth)){
				fuzzyGoto(worker, ProcessedMapEarth.sortedList.get(karbWayPointIndex).loc); //tries to go to closest karbonite deposit
			}
			//note that the 0 is hard coded.
			//idk if can do different numbers or increment them, because tileValue does not change.
			//random wouldn't work either, because it would falter between multiple places
			//could be fixed by making worker into a class, like I should have a long time ago.
			//TODO: fix workers trying to mine unreachable deposits
			else{ //worker is on mars
				//TODO: figure out how karbonite on mars works.
				int arbitrary = rnd.nextInt(7);
				tryMoveInDirection(uid, directions[arbitrary]);
			}
		}
	}
	
	private static void runFactory(Unit factory){
	
		if (factory.structureIsBuilt() == 0) structureNeedsBuilding = true; //APPARENTLY FOR SHORTS, 0 = false, !0 = true
		else{ 
			
			inGarrison = (int) factory.structureGarrison().size();
			boolean shouldStop = false;
			if (factory.location().isOnPlanet(Planet.Earth) && round > 250) shouldStop = true;
			if (shouldStop && numWorkers <=4) shouldStop = false;
			
			if (shouldStop) return;
			
			
			if (factory.isFactoryProducing() == 0 && numCombatUnits < maxNumUnits){
				String nextUnit = trainOrder[buildIndex % trainOrder.length];
				try{
					if ((int)teamKarbonite > buildKnightCost * 2 && inGarrison < 7){
	
						//System.out.println("Next unit: " + nextUnit);
						//System.out.println("Next unit: " + nextUnit);
						
						switch (nextUnit){
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
						teamKarbonite -= buildKnightCost;
						
					}
				} catch (Exception e){
					System.out.println("Somehow, code skipped the if requirement");
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

	private static void runRocket(Unit rocket) {
		// TODO: write rocket
		
		if (rocket.structureIsBuilt() == 0) structureNeedsBuilding = true; //APPARENTLY FOR SHORTS, 0 = false, !0 = true
		else{ 
			MapLocation loc = rocket.location().mapLocation();
			VecUnitID garrison = rocket.structureGarrison();
			int uid = rocket.id();
			
			if (loc.getPlanet().equals(Planet.Earth)){
				rocketAvailible = true;
				
				VecUnit nearby = gc.senseNearbyUnits(loc, sqrt2);
				
				for (int i = 0; i < nearby.size(); i++){
					int tid = nearby.get(i).id();
					if (gc.canLoad(uid, tid)){
						if (rocket.structureGarrison().size() <= 6){
							if (!nearby.get(i).unitType().equals(UnitType.Worker)) gc.load(uid, tid);
						//don't load up workers.
						}
						else {
							gc.load(uid, tid);
							numWorkers--;
						}
						//loads 1 worker, last
						
					}
				}
				if (round > 500 || rocket.structureGarrison().size() >= 6){
					System.out.println("Rocket launching.");
					for (int i = 0; i < 8; i++){ //try 8 times
						int randOffsetX = rnd.nextInt(20);
						int randOffsetY = rnd.nextInt(10);
						if (randOffsetX > 10) randOffsetX -= 10;
						if (randOffsetY > 10) randOffsetY -= 10;
						MapLocation toMars = offSetMapLocation(marsStartPoint, randOffsetX, randOffsetY);
						
						if (gc.canLaunchRocket(uid, toMars)) {
							gc.launchRocket(uid, toMars);
							rocketAvailible = false;
							System.out.println("WE GOING TO MARS BOIS");
							break;
						}
					}
				}
				System.out.println(uid + " has " + rocket.structureGarrison().size() + " units");
			}
			else{ //on mars
				if (rocket.structureGarrison().size() == 0){} //self destruct somehow
				for (int i = 0; i < 7; i++){
					if (gc.canUnload(uid, directions[i])) gc.unload(uid, directions[i]);
				}
				
			}
		}
	}
//TODOL healers broken. not sure if battlecode, or my fault.
	private static void RunHealer(Unit healer) {
		// TODO: write healer
		if (!healer.location().isInGarrison()){
			boolean canHeal = gc.isHealReady(healer.id());
			MapLocation location = healer.location().mapLocation();
			int uid = healer.id();
			if (canHeal){
				for (int timesHealed = 0; timesHealed < 2; timesHealed++){
					VecUnit allies = gc.senseNearbyUnitsByTeam(location, healerAttRange, myTeam);
					if (allies.size() < 0) break;
					
					int targetId = -1;
					int mostValue = 0;
					for (int i = 0; i < allies.size(); i++){
						int targetValue = findHealValue(allies.get(i).unitType(), (int)allies.get(i).health());
						if (targetValue > mostValue){
							mostValue = targetValue;
							targetId = allies.get(i).id();
						}
					}
					if (gc.isHealReady(uid) && gc.canHeal(uid, targetId)) gc.heal(uid, targetId);
					
					canHeal = gc.isHealReady(healer.id());
					if (!canHeal) return; //no need to move or go on in code.
				}
			}
			
			if (gc.isMoveReady(uid)){
				if (battleStateEarth.equals(BSEarth.KERBALSPACEPROGRAM)) fuzzyGoto(healer, earthStartPoint);
				else fuzzyGoto(healer, standingDefenseRallyPoint);
			}
			//duplicate healing code. to cover heal-move and move-heal
			if (canHeal){
				for (int timesHealed = 0; timesHealed < 2; timesHealed++){
					VecUnit allies = gc.senseNearbyUnitsByTeam(location, healerAttRange, myTeam);
					if (allies.size() < 0) break;
					
					int targetId = -1;
					int mostValue = 0;
					for (int i = 0; i < allies.size(); i++){
						int targetValue = findHealValue(allies.get(i).unitType(), (int)allies.get(i).health());
						if (targetValue > mostValue){
							mostValue = targetValue;
							targetId = allies.get(i).id();
						}
					}
					if (gc.isHealReady(uid) && gc.canHeal(uid, targetId)) gc.heal(uid, targetId);
					
					canHeal = gc.isHealReady(healer.id());
				}
			}
		}
	}
	
	private static void RunMage(Unit mage) {
		// TODO: write mage
		if (!mage.location().isInGarrison()){
			int uid = mage.id();
			boolean isAttackReady = gc.isAttackReady(uid);
			String moveType = "";
			String attackType = "";
			MapLocation location = mage.location().mapLocation();
			
			//first check if can attack already
			if (isAttackReady){
				MapLocation runFrom = tryAttackReturnTargetLoc(UnitType.Mage,uid, mageAttRange, location);
				//bad coding probably; tryAttack attacks and also returns target.
				isAttackReady = gc.isAttackReady(uid); //resets if ranger did attack
				if (runFrom != null && gc.isMoveReady(uid)) tryMoveInDirection(mage.id(), OppositeDirectionOf(location.directionTo(runFrom)));
			}
			
			//movement
			//TODO: needs to start kiting as soon as it sees an enemy.
			if (gc.isMoveReady(uid)){
				if (battleStateEarth.equals(BSEarth.KERBALSPACEPROGRAM)) fuzzyGoto(mage, earthStartPoint);
				else{
					moveToNearestEnemy(mage, rangerAttRange, true); //ranger will attack anything. 
				}
			}
			
			//check attack again, if you moved in range to attack
			if (isAttackReady){
				MapLocation runFrom = tryAttackReturnTargetLoc(UnitType.Mage, uid, mageAttRange, location);
				if (runFrom != null && gc.isMoveReady(uid)) tryMoveInDirection(mage.id(), OppositeDirectionOf(location.directionTo(runFrom)));
				
			}
			
			
		}
	}

	//TODO: knigghs should use the kiting code for knight 1v1's. current though, that is unlikely to happen
	//Knights need gamestmates fixed too. Currently don't do much.
	private static void runKnight(Unit knight) {
		//TODO: need knight 1v1 micro code.Also, they don't work on mars.
		
		if (!knight.location().isInGarrison() && knight.location().isOnPlanet(Planet.Earth)){
			
			boolean isAttackReady = gc.isAttackReady(knight.id());
			String moveType = "wanderTo";
			String attackType = "close";
			MapLocation location = knight.location().mapLocation();
			
			
			//first check if can attack
			if (isAttackReady){
				tryAttack(knight.id(), knightAttRange, location);
				isAttackReady = gc.isAttackReady(knight.id()); //resets if knight did attack
			}
			
			//handles different cases for battlestates
			if (battleStateEarth.equals(BSEarth.KERBALSPACEPROGRAM) && gc.isMoveReady(knight.id())) fuzzyGoto(knight, earthStartPoint);
			else if (!battleStateEarth.equals(BSEarth.ATTACK)){
				moveType = "wanderTo";
				//else, moveType = "bugGoto"
			}
			//else moveType = "bugGoto", set by default.
			if (battleStateEarth.equals(BSEarth.ATTACK) || battleStateEarth.equals(BSEarth.SEARCHANDDESTROY)){
				attackType = "seek";
			}
			
			
			//movement
			if (gc.isMoveReady(knight.id())){
				if (moveType.equals("wanderTo")) {
					wanderTo(knight, standingRallyPoint, 12);
				}else if (attackType.equalsIgnoreCase("seek")){ //special movement code for seeking enemy
					int searchDist = 9;
					if (battleStateEarth.equals(BSEarth.SEARCHANDDESTROY)) searchDist = 50;
					
					moveToNearestEnemy(knight, searchDist, true);	
				
				}
				else{ 
					fuzzyGoto(knight, standingRallyPoint);	
				}
			}
			
			//check attack again, if you moved in range to attack
			if (isAttackReady){
				tryAttack(knight.id(), knightAttRange, location);
			}
		
		} //dw bout this double bracket
	}
	
	private static void runRanger(Unit ranger) {
		if (!ranger.location().isInGarrison()){

			int uid = ranger.id();
			boolean isAttackReady = gc.isAttackReady(uid);
			String moveType = "kite";
			String attackType = "close";
			MapLocation location = ranger.location().mapLocation();
			

			identifyJuicyTargets(location, knightAttRange);
			
			//first check if can attack already
			if (isAttackReady){
				MapLocation runFrom = tryAttackReturnTargetLoc(UnitType.Ranger, uid, rangerAttRange, location);
				//bad coding probably; tryAttack attacks and also returns target.
				isAttackReady = gc.isAttackReady(uid); //resets if ranger did attack
				if (runFrom != null && gc.isMoveReady(uid)) tryMoveInDirection(ranger.id(), OppositeDirectionOf(location.directionTo(runFrom)));
			}
			
			//movement
			//TODO: needs to start kiting as soon as it sees an enemy.
			if (gc.isMoveReady(uid)){
				if (battleStateEarth.equals(BSEarth.KERBALSPACEPROGRAM)) fuzzyGoto(ranger, earthStartPoint);
				else if (isAttackReady){
					moveToNearestEnemy(ranger, rangerAttRange, true); //ranger will attack anything. 
				}
			}
			
			//check attack again, if you moved in range to attack
			if (isAttackReady){
				MapLocation runFrom = tryAttackReturnTargetLoc(UnitType.Ranger, uid, rangerAttRange, location);
				if (runFrom != null && gc.isMoveReady(uid)) tryMoveInDirection(ranger.id(), OppositeDirectionOf(location.directionTo(runFrom)));
				
			}
		}
	}

	private static void moveToNearestEnemy(Unit u, int searchRange, boolean canGoOff){
		MapLocation location = u.location().mapLocation();
		int uid = u.id();
		
		
		if (attackedHighPT.length > 0){
			//finds nearest enemy in priority targets
			double closest = 9001;
			Unit target = null;
			for (int i = 0; i < attackedHighPT.length; i++){
				if (attackedHighPT[i] == null) break;
				double distToSq = (double)location.distanceSquaredTo(attackedHighPT[i].location().mapLocation());
				if (distToSq < closest){
					closest = distToSq;
					target = attackedHighPT[i];
				}
			}
			if (target != null){ //go to nearest enemy
				Direction toClosest = location.directionTo(target.location().mapLocation());
				if (toClosest != null && gc.canMove(uid, toClosest)){
					fuzzyGoto(u, target.location().mapLocation());
					return;
				}
			}
		}
		if (juicyTargets.length > 0){
			//finds nearest enemy in priority targets
			double closest = 9001;
			Unit target = null;
			for (int i = 0; i < juicyTargets.length; i++){
				if (juicyTargets[i] == null) break;
				double distToSq = (double)location.distanceSquaredTo(juicyTargets[i].location().mapLocation());
				if (distToSq < closest){
					closest = distToSq;
					target = juicyTargets[i];
				}
			}
			if (target != null){ //go to nearest enemy
				Direction toClosest = location.directionTo(target.location().mapLocation());
				if (toClosest != null && gc.canMove(uid, toClosest)){
					fuzzyGoto(u, target.location().mapLocation());
					return;
				}
			}
		}
		if (attackedLowPT.length > 0){
			//finds nearest enemy in priority targets
			double closest = 9001;
			Unit target = null;
			for (int i = 0; i < attackedLowPT.length; i++){
				if (attackedLowPT[i] == null) break;
				double distToSq = (double)location.distanceSquaredTo(attackedLowPT[i].location().mapLocation());
				if (distToSq < closest){
					closest = distToSq;
					target = attackedLowPT[i];
				}
			}
			if (target != null){ //go to nearest enemy
				Direction toClosest = location.directionTo(target.location().mapLocation());
				if (toClosest != null && gc.canMove(uid, toClosest)){
					fuzzyGoto(u, target.location().mapLocation());
					return;
				}
			}
		}
		MapLocation goOffTo = null;
		if (canGoOff){
			VecUnit enemies = gc.senseNearbyUnitsByTeam(location, searchRange, enemyTeam);
			if (enemies.size() > 0){
				//finds nearest enemy
				double closest = 9001;
				int targetIndex = -1;
				for (int i = 0; i < enemies.size(); i++){
					if (enemies.get(i) == null) return; //this one can be return because theres no more searching after
					double distToSq = (double)location.distanceSquaredTo(enemies.get(i).location().mapLocation());
					if (distToSq < closest){
						closest = distToSq;
						targetIndex = i;
					}
				}
				if (targetIndex != -1){ //go to nearest enemy
					Direction toClosest = location.directionTo(enemies.get(targetIndex).location().mapLocation());
					if (toClosest != null && gc.canMove(uid, toClosest)){
						fuzzyGoto(u, enemies.get(targetIndex).location().mapLocation());
						return;
					}
				}
			}
			else {
				if (u.location().isOnPlanet(Planet.Earth)) {
					goOffTo = guessEnemyEarthStart;

					fuzzyGoto(u, goOffTo);
				}
				else {
					tryMoveRandomDirection(u); 
					//TODO: mars nav sucks. need fix.
				}
			}
		}
	}
	private static void tryAttack(int uid, int range, MapLocation location){
		//TODO: note the I hard coded range	
		
		//try hitting high priority targets. Juicy - alreadyattackedHPT, low priority, any.
		for (int i = 0; i < attackedHighPT.length; i++){
			if (attackedHighPT[i] == null) break;
			
			int target = attackedHighPT[i].id();
			if (gc.canAttack(uid, target)){
				gc.attack(uid, target);
				return;
			}
		}
		for (int i = 0; i < juicyTargets.length; i++){
			if (juicyTargets[i] == null) break;
			
			int target = juicyTargets[i].id();
			if (gc.canAttack(uid, target)){
				gc.attack(uid, target);
				return;
			}
		}
		
		
		//try hitting low priority targets
		for (int i = 0; i < attackedLowPT.length; i++){
			if (attackedLowPT[i] == null) break;
			
			int target = attackedLowPT[i].id();
			
			if (gc.canAttack(uid, target)){
				gc.attack(uid, target);
				return;
			}
		}
		
		//try hitting anything. Then, add to priorities lists if possible.
		VecUnit enemies = gc.senseNearbyUnitsByTeam(location, range, enemyTeam);
		if (enemies.size() > 0 && enemies != null){	
			int mostValue = -1;
			Unit target = null;
			for (int i = 0; i < enemies.size(); i++){ //I assume that this only works on targets in range.
				int targetValue = findSingleTargetValue(enemies.get(i).unitType());
				if (targetValue > mostValue){
					mostValue = targetValue;
					target = enemies.get(i);
				}
	    	}
			
			if (gc.canAttack(uid, target.id())){
				gc.attack(uid, target.id());
				updateAttackedTargets(target, mostValue); 
				return;
			}
		}
	}
	private static MapLocation tryAttackReturnTargetLoc(UnitType unitType, int uid, int range, MapLocation location){
		//TODO: note the I hard coded range	
		
		//try hitting high priority targets. Juicy - alreadyattackedHPT, low priority, any.
		for (int i = 0; i < attackedHighPT.length; i++){
			if (attackedHighPT[i] == null) break;
			
			int target = attackedHighPT[i].id();
			if (gc.canAttack(uid, target)){
				gc.attack(uid, target);
				return attackedHighPT[i].location().mapLocation();
			}
		}
		for (int i = 0; i < juicyTargets.length; i++){
			if (juicyTargets[i] == null) break;
			
			int target = juicyTargets[i].id();
			if (gc.canAttack(uid, target)){
				gc.attack(uid, target);
				return juicyTargets[i].location().mapLocation();
			}
		}
		
		
		//try hitting low priority targets
		for (int i = 0; i < attackedLowPT.length; i++){
			if (attackedLowPT[i] == null) break;
			
			int target = attackedLowPT[i].id();
			
			if (gc.canAttack(uid, target)){
				gc.attack(uid, target);
				return attackedLowPT[i].location().mapLocation();
			}
		}
		
		//try hitting anything. Then, add to priorities lists if possible.
		VecUnit enemies = gc.senseNearbyUnitsByTeam(location, range, enemyTeam);
		if (enemies.size() > 0 && enemies != null){	
			int mostValue = -1;
			Unit target = null;
			for (int i = 0; i < enemies.size(); i++){ //I assume that this only works on targets in range.
				int targetValue = 0;
				if (unitType.equals(UnitType.Mage)) findAreaTargetValue(enemies.get(i).location().mapLocation());
				else findSingleTargetValue(enemies.get(i).unitType());
				
				if (targetValue > mostValue){
					mostValue = targetValue;
					target = enemies.get(i);
				}
	    	}
			
			if (gc.canAttack(uid, target.id())){
				gc.attack(uid, target.id());
				updateAttackedTargets(target, mostValue); 
				return target.location().mapLocation();
			}
		}
		
		return null;
	}
	private static void identifyJuicyTargets(MapLocation observer, int range){
		VecUnit enemies = gc.senseNearbyUnitsByTeam(observer, range, enemyTeam);
		for (int i = 0; i < enemies.size(); i ++){
			if (juicyIndex < juicyTargets.length){
				if (enemies.get(i).unitType().equals(UnitType.Healer) || enemies.get(i).unitType().equals(UnitType.Mage)){
					juicyTargets[juicyIndex]= enemies.get(i);
					juicyIndex++;
				}
			}
		}
	}
	private static void updateAttackedTargets(Unit target, int value){
		int HPTLength = attackedHighPT.length;
		int LPTLength = juicyTargets.length;
		if (value >= mageValue){ //priority is healers than mages, then everything else
			if (HPTIndex < HPTLength){
				attackedHighPT[HPTIndex]= target;
				HPTIndex++;
				//System.out.println("Added a unit to the high priority targets list.");
				return;
			}
		}
		else{
			if (LPTIndex < LPTLength){
				juicyTargets[LPTIndex] = target;
				LPTIndex++;
				//System.out.println("Added a unit to the low priority targets list.");
				return;
			}
		}
	}

	private static int findSingleTargetValue(UnitType type){
		//perhaps add 'health' as a weight?
		int value = -1;
		String unit = type.toString();
		switch (unit){
			//high priority:
			case "Healer": {
				value = healerValue;
				break;
			}
			case "Mage": {
				value = mageValue;
				break;
			}
			
			//lower priority
			case "Ranger": {
				value = rangerValue;
				break;
			}
			case "Knight": {
				value = knightValue;
				break;
			}
			case "Worker": {
				value = workerValue;
				break;
			}
			case "Factory": {
				value = factoryValue;
				break;
			}
			case "Rocket": {
				value = rocketValue;
				break;
			}
		}
		return value;
		
	}
	private static int findAreaTargetValue(MapLocation target){
		int value = 0;
		VecUnit enemies = gc.senseNearbyUnitsByTeam(target, mageAttRange, enemyTeam);
		for (int i = 0; i < enemies.size(); i ++){
			value += findSingleTargetValue(enemies.get(i).unitType());
		}
		return value;
	}
	private static int findHealValue(UnitType type, int health){
		
		
		int value = -1;
		String unit = type.toString();
		switch (unit){
			//high priority:
			case "Healer": {
				if (health > 90) return 0;
				value = healerValue;
				break;
			}
			case "Mage": {
				if (health > 70) return 0;
				value = mageValue;
				break;
			}
			
			//lower priority
			case "Ranger": {
				if (health > 180) return 0;
				value = rangerValue;
				break;
			}
			case "Knight": {
				if (health > 240) return 0;
				value = knightValue;
				break;
			}
			case "Worker": {
				if (health > 80) return 0;
				value = workerValue;
				break;
			}
		}
		for (int i = 0; i < criticalHealthValues.length; i++){
			if (health == criticalHealthValues[i]){
				value += 40;
				break;
			}
		}
		
		return value;
	}
	
	public enum BSEarth{ //Stands for BattleStateEarth. Too lazy to type that all out though.
		PREP,
		ADVANCE,
		ATTACK,
		SEARCHANDDESTROY,
		DEFEND,
		KERBALSPACEPROGRAM
	}
	public static BSEarth battleStateEarth = BSEarth.PREP;
	
	//TODO: prob should do this, because knight code sucks. 
	//Also need some kind of "free seek." With current knight code, 
	//after they rush enemy base, they just kinda sit there and do nothing.
	
	//TODO: put earth as an argument. 
	//TODO: make somewhat adequete
	private static void battleStateManagerEarth(){ 
		try{
			
		int someValueThatNeedsToBeAdjustedByMapSize = 8;
		int someOtherValueThatNeedsToBeAdjustedByMapSize = 14;
		
		//System.out.println("Nearby friendlies: " + gc.senseNearbyUnitsByTeam(guessEnemyEarthStart, 4, myTeam).size());
		if (rocketAvailible || round > 300){
			battleStateEarth = BSEarth.KERBALSPACEPROGRAM;
			standingRallyPoint = earthStartPoint;
			standingDefenseRallyPoint = offSetMapLocation(earthStartPoint, 5, 5);
			if (structureNeedsBuilding)maxWorkers = 10; //HARDCAP OF 10
			
		}
		else if (battleStateEarth == BSEarth.ATTACK && gc.senseNearbyUnitsByTeam(guessEnemyEarthStart, 4, myTeam).size() > 6){
			battleStateEarth = BSEarth.SEARCHANDDESTROY;
			standingRallyPoint = guessEnemyEarthStart;
			standingDefenseRallyPoint = offSetMapLocation(guessEnemyEarthStart, 4, 4);
			//System.out.println("NO SURVIVORS!!!");
		}
		else if (numCombatUnits > someOtherValueThatNeedsToBeAdjustedByMapSize){
			battleStateEarth = BSEarth.ATTACK;
			standingRallyPoint = guessEnemyEarthStart;
			standingDefenseRallyPoint = offSetMapLocation(guessEnemyEarthStart, 4, 4);
			//System.out.println("FOR THE HORDE!!");
		}
		else if (numCombatUnits > someValueThatNeedsToBeAdjustedByMapSize){
			battleStateEarth = BSEarth.ADVANCE;
			standingRallyPoint = earthMidPoint;
			standingDefenseRallyPoint = offSetMapLocation(earthMidPoint, 4, 4);
		}
		else if (numCombatUnits < someValueThatNeedsToBeAdjustedByMapSize){
			battleStateEarth = BSEarth.PREP;
			standingRallyPoint = earthDefendPoint;
			standingDefenseRallyPoint = offSetMapLocation(earthDefendPoint, 4, 4);
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
			//System.out.println("bugGoto failed on " + unit.id() + ". They might have died.");
		}
	}
	
	private static void wanderTo(Unit unit, MapLocation dest, long wanderDist){
		try{
			if (gc.isMoveReady(unit.id())){
				if (unit.location().mapLocation().isWithinRange(wanderDist, dest)){
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

    public static boolean tryMoveInDirection(int uid, Direction dir){
    	if (gc.canMove(uid, dir)) {
			gc.moveRobot(uid, dir);
			return true;
		}
		Direction left = rotateLeft(dir);
		if (gc.canMove(uid, left)) {
			gc.moveRobot(uid, left);
			return true;
		}
		Direction right = rotateRight(dir);
		if (gc.canMove(uid, right)) {
			gc.moveRobot(uid, right);
			return true;
		}
		return false;
	}
    
    
	private static MapLocation bugDest = null;
	
	
	private static boolean bugTracing = false;
	private static MapLocation bugLastWall = null;
	private static int closestDistWhileBugging = Integer.MAX_VALUE;	
	private static int generousBugOffset = 0;
	private static int bugNumTurnsWithNoWall = 0;
	private static boolean bugWallOnLeft = true; // whether the wall is on our left or our right
	//TODO: some robot may want to switch this
	private static boolean flipBugging = false;
	private static boolean[][] bugVisitedLocations = new boolean[100][100];	
	
	public static void bugGoto(Unit u, MapLocation theDest) {
		MapLocation here = u.location().mapLocation();
		int uid = u.id();
		if (theDest != null){
			bugDest = theDest;
			bugTracing = false;
		}
		
		if (here.equals(bugDest)) {return;}
		
		if (!bugTracing) {
			// try to go direct; start bugging on failure
			Direction destDir = here.directionTo(bugDest);
			if (tryMoveInDirection(uid, destDir)) {
				return;
			} else {
				bugStartTracing(u, here);
			}
		} else { // state == State.BUGGING
			// try to stop bugging
			if (here.distanceSquaredTo(bugDest) < closestDistWhileBugging + generousBugOffset) {
				if (tryMoveInDirection(uid, here.directionTo(bugDest))) {
					bugTracing = false;
					return;
				}
			}
		}
		bugTraceMove(u, uid, here, false);
	    
	    if (bugNumTurnsWithNoWall >= 2) {
	    	bugTracing = false;
	    }
	}

	public static void bugReset() {
		bugTracing = false;
	}

	static void bugStartTracing(Unit u, MapLocation here) {
		int uid = u.id();
		bugTracing = true;
		if (u.location().isOnPlanet(Planet.Earth)) bugVisitedLocations = new boolean[(int) earthMap.getWidth()][(int) earthMap.getHeight()];
		else bugVisitedLocations = new boolean[(int) marsMap.getWidth()][(int) marsMap.getHeight()];
			
		closestDistWhileBugging = (int) here.distanceSquaredTo(bugDest);
		bugNumTurnsWithNoWall = 0;
		
		Direction dirToDest = here.directionTo(bugDest);
		Direction leftDir = dirToDest;
		int leftDistSq = Integer.MAX_VALUE;
		for (int i = 0; i < 8; ++i) {
			leftDir = rotateLeft(leftDir);
			if (gc.canMove(uid, leftDir)) {
				leftDistSq = (int) here.add(leftDir).distanceSquaredTo(bugDest);
				break;
			}
		}
		Direction rightDir = dirToDest;
		int rightDistSq = Integer.MAX_VALUE;
		for (int i = 0; i < 8; ++i) {
			rightDir = rotateRight(rightDir);
			if (gc.canMove(uid, rightDir)) {
				rightDistSq = (int)here.add(rightDir).distanceSquaredTo(bugDest);
				break;
			}
		}
		if (rightDistSq < leftDistSq) {
			bugWallOnLeft = true;
			bugLastWall = here.add(rotateLeft(rightDir));
		} else {
			bugWallOnLeft = false;
			bugLastWall = here.add(rotateRight(leftDir));
		}
	}
	
	static void bugTraceMove(Unit u, int uid, MapLocation here, boolean recursed){
		Direction tryDir = here.directionTo(bugLastWall);
		bugVisitedLocations[here.getX() % 100][here.getY() % 100] = true;
		if (gc.canMove(uid, tryDir)) {
			bugNumTurnsWithNoWall += 1;
		} else {
			bugNumTurnsWithNoWall = 0;
		}
		for (int i = 0; i < 8; ++i) {
			if (bugWallOnLeft) {
				tryDir = rotateRight(tryDir);
			} else {
				tryDir = rotateLeft(tryDir);
			}
			MapLocation dirLoc = here.add(tryDir);
			if (!gc.canMove(uid, tryDir) && !recursed) { //idk if this works
				// if we hit the edge of the map, reverse direction and recurse
				bugWallOnLeft = !bugWallOnLeft;
				bugTraceMove(u, uid, here, true);
				return;
			}
			if (gc.canMove(uid, tryDir)) {
				gc.moveRobot(uid, tryDir);
				here = u.location().mapLocation(); // we just moved
				if (bugVisitedLocations[here.getX() % 100][here.getY() % 100]) {
					bugTracing = false;
					//generousBugOffset++;
					//TODO: uncomment above code when this code is in a class.
				}
				return;
			} else {
				bugLastWall = here.add(tryDir);
			}
		}
	}
	
	
	public static Direction rotateLeft(Direction dir){
		int newIndex = dir.ordinal() - 1;
		if (newIndex == -1) newIndex = 6; //if it was pointing north (0) change index to NW (6)
	
		Direction left = directions[newIndex];
		return left;
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
	

	public static Direction rotateRight(Direction dir){
		int newIndex = dir.ordinal() + 1;
		if (newIndex > 7) newIndex = 0; //if it was pointing NW (7) change index to N (0)
	
		Direction right = directions[newIndex];
		return right;
	}
	
	
	  //some MapLocation utility functions.
	public static MapLocation offSetMapLocation(MapLocation a, int offX, int offY){
		int aX = a.getX();
		int aY = a.getY();
		Planet aP = a.getPlanet();
		int flipX = 1;
		int flipY = 1;
		if (!startLowerX) flipX = -1;
		if (!startLowerY) flipY = -1;
		
		aX += offX * flipX;
		aY += offY * flipY;
		
		return new MapLocation(aP, aX, aY);
	}
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
    	int newX = -1;
		int newY = -1;
		if (loc.getPlanet().equals(Planet.Earth)){
			newX = (int) (earthMap.getWidth() - loc.getX());
			newY =(int) (earthMap.getHeight() - loc.getY());
		}
		else {
			newX = (int) (marsMap.getWidth() - loc.getX());
			newY =(int) (marsMap.getHeight() - loc.getY());
		}
		//only works on Earth (Mars not symmetrical)
		if (loc.getPlanet().equals(Planet.Earth)){
			//TODO: add in earth equivilant.S   
			MapLocation inverted = new MapLocation(Planet.Earth, newX, newY);
			return inverted;
		}
		else return null;
	}
    private static Direction OppositeDirectionOf(Direction towards){
    	int away = towards.ordinal() + 4;
    	if (away > 7) away -= 8; //this makes sense I swear.
    	return directions[away];
    }
    public static MapLocation getLocInDirection(MapLocation loc, int dir){
		int xOffset = 0;
		int yOffset = 0;
		switch (dir){
		case 1: 
			yOffset = 1;
			break;
		case 2:
			xOffset = 1;
			yOffset = 1;
			break;
		case 3:
			xOffset = 1;
			break;
		case 4:
			xOffset = 1;
			yOffset = -1;
			break;
		case 5:
			yOffset = -1;
			break;
		case 6:
			xOffset = -1;
			yOffset = -1;
			break;
		case 7:
			xOffset = -1;
			break;
		case 8:
			xOffset = 1;
			yOffset = -1;
			break;
		}
		
		MapLocation newLoc = new MapLocation(loc.getPlanet(), 
				loc.getX() + xOffset, 
				loc.getY() + yOffset);	
        if (earthMap.isPassableTerrainAt(newLoc) != 0) return newLoc;
        else return null;
	}
}