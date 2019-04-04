public class WorldMap {

	private int highest_pX;
	private int highest_pY;
	
	private int[][] mapTexture;
	private int[][] nextMapTexture;
	
	private int[][] mapEntities;
	private int[][] nextMapEntities;
	
	public double[][] mapAltitude;
	private int mapWidth, mapHeight;
	
	public int cpt_lave, duree_lave;
	public int cpt_pluie, cpt_evaporation;
	public double hauteur_pluie;
	public boolean is_raining, lave_coule;
	public static final double P_RAIN = 0.05, P_LAVE = 0.05;
	public static final int LAVE_DROITE = 0, LAVE_BAS = 1, LAVE_GAUCHE = 2, LAVE_HAUT = 3;
	

	public WorldMap() {
		this.mapWidth = Ecosystem.mapWidth;
		this.mapHeight = Ecosystem.mapHeight;
		
		mapTexture = new int[mapHeight][mapWidth];
		nextMapTexture = new int[mapHeight][mapWidth];
		mapEntities = new int[mapHeight][mapWidth];
		nextMapEntities = new int[mapHeight][mapWidth];
		mapAltitude = new double[mapHeight][mapWidth];
		
		initWorldMap();
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	////				 					INITIALISATION METHODS											////
	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	public void initWorldMap() {
		is_raining = false;
		cpt_pluie = cpt_evaporation = 0;
		hauteur_pluie = 0;
		initMapAltitude();
		initMapTexture();
		initMapEntities();
	}
	
			 
	public void initMapAltitude() {
		double rand = Math.random() * 256;
		for (int x = 0; x < mapHeight; x++) {
			for (int y = 0; y < mapWidth; y++)
				mapAltitude[x][y] = PerlinNoise.noise(x + rand, y + rand, PerlinNoise.RESOLUTION)
						* PerlinNoise.INTERVALLE;
		}
	}

	public void initMapTexture() {
		for (int x = 0; x < mapHeight; x++) {
			for (int y = 0; y < mapWidth; y++) {
				mapTexture[x][y] = getTexture(x, y);
			}
		}

		int center_spawnX;
		int center_spawnY;
		do {
			center_spawnX = (int) (Math.random() * mapHeight);
			center_spawnY = (int) (Math.random() * mapWidth);
		} while (mapTexture[center_spawnX][center_spawnY] != MapTextureID.GROUND || !surroundedByGround(center_spawnX, center_spawnY));
		for (int i = -1; i < 2; i++) {
			for (int j = -1; j < 2; j++) {
				int curi = (center_spawnX + i + mapWidth)%mapWidth;
				int curj = (center_spawnY + j + mapHeight)%mapHeight;
				mapTexture[curi][curj] = MapTextureID.SPAWN;
			}
		}
		Ecosystem.spawn_pos = new Position(center_spawnX, center_spawnY);
		initialisationVolcan();
	}

	private boolean surroundedByGround(int x, int y) {
		// TODO Auto-generated method stub
		return nb_of_ground_tiles_around(x,y) == 8;
	}

	public void initMapEntities() {
		for (int x = 0; x < mapEntities.length; x++) {
			for (int y = 0; y < mapEntities[0].length; y++) {
				if (Math.random() < 0.3 && mapTexture[x][y] == MapTextureID.GROUND && nb_of_water_tiles_around(x,y) == 0)
						this.mapEntities[x][y] = MapEntitiesID.GREEN_TREE;
				else
					this.mapEntities[x][y] = MapEntitiesID.NOTHING;
			}
		}
	}
	

	public int getTexture(int x, int y) {
		if (mapAltitude[x][y] < -15)
			return MapTextureID.WATER;
		else if (mapAltitude[x][y] >= -15 && mapAltitude[x][y] < 5) // -15 et 15
			return MapTextureID.GROUND;
		else
			return MapTextureID.MOUNTAIN;
	}
	
	

	public void initialisationVolcan() {
		double higherPoint = 0;
		int pX = 0, pY = 0;
		lave_coule = false;
		for (int x = 0; x < mapWidth; x++) {
			for (int y = 0; y < mapHeight; y++) {
				if (mapAltitude[x][y] > higherPoint) {
					higherPoint = mapAltitude[x][y];
					highest_pX = pX = x;
					highest_pY = pY = y;
				}

			}
		}
		mapTexture[pX][pY] = MapTextureID.CRATERE;
	}
	

	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// UPDATE METHODS
	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// method used to update maps
	public void update() {
		
		for(int x = 0; x < mapTexture.length; x++) {
			for(int y = 0; y < mapTexture[x].length; y++) 
				nextMapTexture[x][y] = mapTexture[x][y];
		}
				
		if(is_raining) {
			pluie();
			cpt_pluie++;
		}else {
			evaporation();
			if(cpt_pluie == 0)
				cpt_pluie = 0;
			else
				cpt_pluie--;
		}
		
		if(lave_coule) 
			ecoulement_lave();

		for(int x = 0; x < mapTexture.length; x++) {
			for(int y = 0; y < mapTexture[0].length; y++) {
				mapTexture[x][y] = nextMapTexture[x][y];
				if(lave_coule == false) {
					if(nextMapTexture[x][y] == MapTextureID.LAVA)
						mapTexture[x][y] = MapTextureID.COLD_LAVA;
				}
			}
		}
		mapTexture[highest_pX][highest_pY] = MapTextureID.CRATERE;
		updateMapEntities();
	}

	
	public void pluie() {
		cpt_evaporation = 0;
		if(Ecosystem.season != Ecosystem.WINTER) {	//%100 == 30

			for(int i = 0; i < mapAltitude.length; i++) {
				for(int j = 0; j < mapAltitude[i].length; j++) {
					if(mapAltitude[i][j] < ( -15 + hauteur_pluie) && mapAltitude[i][j] > -15)
						nextMapTexture[i][j] = MapTextureID.WATER;
				}
			}
			hauteur_pluie += 0.1;
		}
	}
	
	
	public void evaporation() {
		for(int i = 0; i < mapAltitude.length; i++) {
			for(int j = 0; j < mapAltitude[i].length; j++) {
				if(mapTexture[i][j] == MapTextureID.WATER && mapAltitude[i][j] > (hauteur_pluie - 15)) {
					nextMapTexture[i][j] = getTexture(i,j);
				}
			}
		}
		if(hauteur_pluie == 0)
			hauteur_pluie = 0;
		else
			hauteur_pluie-= 0.05;
	}

		
	public void ecoulement_lave() {	
		// TODO Auto-generated method stub
		if(lave_coule) {
			if((cpt_lave%4 == 0)) {
				int rand = (int)(Math.random()*4);
				for(int x = 0; x < mapTexture.length; x++) {
					for(int y = 0; y < mapTexture[x].length; y++) {
						int up = (x - 1 + mapHeight)%mapHeight;
						int down = (x + 1 + mapHeight)%mapHeight;
						int left = (y - 1 + mapWidth)%mapWidth;
						int right = (y + 1 + mapWidth)%mapWidth;
						if(mapTexture[x][y] == MapTextureID.LAVA || mapTexture[x][y] == MapTextureID.CRATERE) {
							if(rand == LAVE_DROITE) {
								if(mapAltitude[up][right] < mapAltitude[x][y] && mapTexture[up][right] != MapTextureID.WATER)
									nextMapTexture[up][right] = MapTextureID.LAVA;
								if(mapAltitude[down][right] < mapAltitude[x][y] && mapTexture[down][right] != MapTextureID.WATER)
									nextMapTexture[down][right] = MapTextureID.LAVA;
								if(mapAltitude[x][right] < mapAltitude[x][y] && mapTexture[x][right] != MapTextureID.WATER)
									nextMapTexture[x][right] = MapTextureID.LAVA;
							}else if(rand == LAVE_BAS) {
								if(mapAltitude[down][y] < mapAltitude[x][y] && mapTexture[down][y] != MapTextureID.WATER)
									nextMapTexture[down][y] = MapTextureID.LAVA;
								if(mapAltitude[down][left] < mapAltitude[x][y] && mapTexture[down][left] != MapTextureID.WATER)
									nextMapTexture[down][left] = MapTextureID.LAVA;
								if(mapAltitude[down][right] < mapAltitude[x][y] && mapTexture[down][right] != MapTextureID.WATER)
									nextMapTexture[down][right] = MapTextureID.LAVA;
							}else if(rand == LAVE_GAUCHE) {
								if(mapAltitude[x][left] < mapAltitude[x][y] && mapTexture[x][left] != MapTextureID.WATER)
									nextMapTexture[x][left] = MapTextureID.LAVA;
								if(mapAltitude[up][left] < mapAltitude[x][y] && mapTexture[up][left] != MapTextureID.WATER)
									nextMapTexture[up][left] = MapTextureID.LAVA;
								if(mapAltitude[down][left] < mapAltitude[x][y] && mapTexture[down][left] != MapTextureID.WATER)
									nextMapTexture[down][left] = MapTextureID.LAVA;
							}else if(rand == LAVE_HAUT) {
								if(mapAltitude[up][left] < mapAltitude[x][y] && mapTexture[up][left] != MapTextureID.WATER)
									nextMapTexture[up][left] = MapTextureID.LAVA;
								if(mapAltitude[up][right] < mapAltitude[x][y] && mapTexture[up][right] != MapTextureID.WATER)
									nextMapTexture[up][right] = MapTextureID.LAVA;
								nextMapTexture[down][y] = MapTextureID.LAVA;
								if(mapAltitude[up][y] < mapAltitude[x][y] && mapTexture[up][y] != MapTextureID.WATER)
									nextMapTexture[up][y] = MapTextureID.LAVA;
							}
						}
					}
				}
			}
		}else {
			for(int i = 0; i < mapTexture.length; i++) {
				for(int j = 0; j < mapTexture[i].length; j++)
					if(mapTexture[i][j] == MapTextureID.LAVA)
						nextMapTexture[i][j] = MapTextureID.COLD_LAVA;
			}
		}
		cpt_lave++;
	}

		

	private void updateMapEntities() {
		// same as updateMapTexture
		for (int x = 0; x < this.mapEntities.length; x++) {
			for (int y = 0; y < this.mapEntities[0].length; y++) {
				if (mapEntities[x][y] != 0) {
					int state = mapEntities[x][y];
					nextMapEntities[x][y] = updateStateEntities(state, x, y);
				}

			}
		}

		randomWildFire();
		for (int x = 0; x < this.mapEntities.length; x++) {
			for (int y = 0; y < this.mapEntities[0].length; y++) {
				mapEntities[x][y] = nextMapEntities[x][y];
			}
		}

	}

	private int updateStateEntities(int state, int x, int y) {
		int updatedState = state;
		switch (state) {
		case MapEntitiesID.NOTHING:
			// can become a tree if lucky and if we are on the ground and if there is no one
			// on the ground
			updatedState = MapEntitiesID.NOTHING;
			if (Ecosystem.season == Ecosystem.SPRING  && is_raining == false && Math.random() < Ecosystem.P_TREE_BIRTH && mapTexture[x][y] == MapTextureID.GROUND && nb_of_water_tiles_around(x,y) == 0) {

				updatedState = MapEntitiesID.BABY_GREEN_TREE;
			}
			break;
		case MapEntitiesID.BABY_GREEN_TREE:
			updatedState = MapEntitiesID.YOUNG_GREEN_TREE;
			break;
		case MapEntitiesID.YOUNG_GREEN_TREE:
			updatedState = MapEntitiesID.GREEN_TREE;
			break;
		case MapEntitiesID.GREEN_TREE:
			updatedState = updateTree(x, y);
			break;
		case MapEntitiesID.BURNING_TREE:
			updatedState = updateBurningTree(x, y);
			break;
		case MapEntitiesID.ASHES:
			updatedState = updateAshes(x, y);
			break;
		default:
			updatedState = state;
			break;
		}
		return updatedState;
	}

	private int updateAshes(int x, int y) {
		// the ashes disappear
		return MapEntitiesID.NOTHING;
	}

	private int updateBurningTree(int x, int y) {
		// the burning tree finish burning... it becomes ashes
		return MapEntitiesID.ASHES;
	}

	
	public int updateTree(int x, int y) {
		int up = (x-1 + mapHeight)%mapHeight;
		int down = (x-1 + mapHeight)%mapHeight;
		int left = (y-1 + mapWidth)%mapWidth;
		int right = (y+1 + mapWidth)%mapWidth;
		
		boolean burn_up = (mapEntities[up][y] == MapEntitiesID.BURNING_TREE ||nextMapTexture[up][y] == MapTextureID.LAVA);
		boolean burn_down = (mapEntities[down][y] == MapEntitiesID.BURNING_TREE || nextMapTexture[down][y] == MapTextureID.LAVA);
		boolean burn_left = (mapEntities[x][left] == MapEntitiesID.BURNING_TREE || nextMapTexture[x][left] == MapTextureID.LAVA);
		boolean burn_right = (mapEntities[x][right] == MapEntitiesID.BURNING_TREE || nextMapTexture[x][right] == MapTextureID.LAVA);
		boolean burn_up_left = (mapEntities[up][left] == MapEntitiesID.BURNING_TREE || nextMapTexture[up][left] == MapTextureID.LAVA);
		boolean burn_up_right = (mapEntities[up][right] == MapEntitiesID.BURNING_TREE || nextMapTexture[up][right] == MapTextureID.LAVA);
		boolean burn_down_left = (mapEntities[down][left] == MapEntitiesID.BURNING_TREE || nextMapTexture[down][left] == MapTextureID.LAVA);
		boolean burn_down_right = (mapEntities[down][right] == MapEntitiesID.BURNING_TREE || nextMapTexture[down][right] == MapTextureID.LAVA);
		
		if(this.mapTexture[x][y] == MapTextureID.LAVA)
			return MapEntitiesID.BURNING_TREE;
		else if(is_raining == false &&(burn_up || burn_down || burn_left || burn_right || burn_down_left || burn_down_right || burn_up_left || burn_up_right || Math.random() < Ecosystem.P_SET_ON_FIRE))
			return MapEntitiesID.BURNING_TREE;
		else 
			return MapEntitiesID.GREEN_TREE;
	}

	public void randomWildFire() {
		int randX = (int) (Math.random() * mapEntities.length);
		int randY = (int) (Math.random() * mapEntities[0].length);
		if (mapEntities[randX][randY] == MapEntitiesID.GREEN_TREE)
			nextMapEntities[randX][randY] = MapEntitiesID.BURNING_TREE;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//// 										GETTERS AND SETTERS 										////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public double[][] getMapAltitude() {
		return mapAltitude;
	}

	public int[][] getMapTexture() {
		return mapTexture;
	}

	public void setMapTexture(int[][] mapTexture) {
		this.mapTexture = mapTexture;
	}

	public int[][] getMapEntities() {
		return mapEntities;
	}

	

	public int[][] getNextMapEntities() {
		return nextMapEntities;
	}

	

	public int getWidth() {
		return mapWidth;
	}


	public int getHeight() {
		return mapHeight;
	}


	public int getMapTextureCase(int x, int y) {
		return mapTexture[x][y];
	}

	public void setTree(int i, int j) {
		// TODO Auto-generated method stub
		if (mapTexture[i][j] == MapTextureID.GROUND)
			mapEntities[i][j] = MapEntitiesID.GREEN_TREE;

	}

	public int nb_of_water_tiles_around(int x, int y) {
		int nb_of_water_tiles = 0;
		if (getTextureLeft(x, y) == MapTextureID.WATER) // left
			nb_of_water_tiles++;
		if (getTextureRight(x, y) == MapTextureID.WATER) // Right
			nb_of_water_tiles++;
		if (getTextureUpLeft(x, y) == MapTextureID.WATER) // upleft
			nb_of_water_tiles++;
		if (getTextureUpRight(x, y) == MapTextureID.WATER) // upRight
			nb_of_water_tiles++;
		if (getTextureDownLeft(x, y) == MapTextureID.WATER) // downleft
			nb_of_water_tiles++;
		if (getTextureDownRight(x, y) == MapTextureID.WATER) // downRight
			nb_of_water_tiles++;
		if (getTextureUp(x, y) == MapTextureID.WATER) // up
			nb_of_water_tiles++;
		if (getTextureDown(x, y) == MapTextureID.WATER) // down
			nb_of_water_tiles++;
		return nb_of_water_tiles;
	}

	public int nb_of_ground_tiles_around(int x, int y) {
		int nb_of_ground_tiles = 0;
		if (getTextureLeft(x, y) == MapTextureID.GROUND)
			nb_of_ground_tiles++;
		if (getTextureRight(x, y) == MapTextureID.GROUND)
			nb_of_ground_tiles++;
		if (getTextureUpLeft(x, y) == MapTextureID.GROUND)
			nb_of_ground_tiles++;
		if (getTextureUpRight(x, y) == MapTextureID.GROUND)
			nb_of_ground_tiles++;
		if (getTextureDownLeft(x, y) == MapTextureID.GROUND)
			nb_of_ground_tiles++;
		if (getTextureDownRight(x, y) == MapTextureID.GROUND)
			nb_of_ground_tiles++;
		if (getTextureUp(x, y) == MapTextureID.GROUND)
			nb_of_ground_tiles++;
		if (getTextureDown(x, y) == MapTextureID.GROUND)
			nb_of_ground_tiles++;
		return nb_of_ground_tiles;
	}
	
	
	
	
	//CORNER DE TYPE A

	public boolean is_corner_A(int x, int y) {
		int water_around = nb_of_water_tiles_around(x, y);
		if (water_around >= 3) {
			if (water_around == 3) {
				// gauche
				if (getTextureLeft(x, y) == MapTextureID.WATER && getTextureUpLeft(x, y) == MapTextureID.WATER && getTextureDownLeft(x, y) == MapTextureID.WATER)
					return false;
				// droit
				else if (getTextureRight(x, y) == MapTextureID.WATER && getTextureUpRight(x, y) == MapTextureID.WATER && getTextureDownRight(x, y) == MapTextureID.WATER)
					return false;
				// dessous
				else if (getTextureDown(x, y) == MapTextureID.WATER && getTextureDownRight(x, y) == MapTextureID.WATER && getTextureDownLeft(x, y) == MapTextureID.WATER)
					return false;
				// dessus
				else if (getTextureUp(x, y) == MapTextureID.WATER && getTextureUpLeft(x, y) == MapTextureID.WATER && getTextureUpRight(x, y) == MapTextureID.WATER)
					return false;
				else
					return true;
			} else {
				if (getTextureUp(x, y) == MapTextureID.GROUND && getTextureRight(x, y) == MapTextureID.GROUND)
					return true;
				else if (getTextureUp(x, y) == MapTextureID.GROUND && getTextureLeft(x, y) == MapTextureID.GROUND)
					return true;
				else if (getTextureDown(x, y) == MapTextureID.GROUND && getTextureLeft(x, y) == MapTextureID.GROUND)
					return true;
				else if (getTextureDown(x, y) == MapTextureID.GROUND && getTextureRight(x, y) == MapTextureID.GROUND)
					return true;
			}
		}
		return false; // ce n'est pas un corner typeA

	}

	public boolean is_corner_A_mountain(int x, int y) {
		int ground_around = nb_of_ground_tiles_around(x, y);
		if (ground_around >= 3) {
			if (ground_around == 3) {
				//gauche
				if (getTextureLeft(x, y) == MapTextureID.GROUND && getTextureUpLeft(x, y) == MapTextureID.GROUND
						&& getTextureDownLeft(x, y) == MapTextureID.GROUND)
					return false;
				// droit
				else if (getTextureRight(x, y) == MapTextureID.GROUND && getTextureUpRight(x, y) == MapTextureID.GROUND
						&& getTextureDownRight(x, y) == MapTextureID.GROUND)
					return false;
				// dessous
				else if (getTextureDown(x, y) == MapTextureID.GROUND && getTextureDownRight(x, y) == MapTextureID.GROUND
						&& getTextureDownLeft(x, y) == MapTextureID.GROUND)
					return false;
				// dessus
				else if (getTextureUp(x, y) == MapTextureID.GROUND && getTextureUpLeft(x, y) == MapTextureID.GROUND
						&& getTextureUpRight(x, y) == MapTextureID.GROUND)
					return false;
				else
					return true;
			} else {
				if (getTextureUp(x, y) == MapTextureID.MOUNTAIN && getTextureRight(x, y) == MapTextureID.MOUNTAIN)
					return true;
				else if (getTextureUp(x, y) == MapTextureID.MOUNTAIN && getTextureLeft(x, y) == MapTextureID.MOUNTAIN)
					return true;
				else if (getTextureDown(x, y) == MapTextureID.MOUNTAIN && getTextureLeft(x, y) == MapTextureID.MOUNTAIN)
					return true;
				else if (getTextureDown(x, y) == MapTextureID.MOUNTAIN
						&& getTextureRight(x, y) == MapTextureID.MOUNTAIN)
					return true;
			}
		}
		return false; // ce n'est pas un corner typeA

	}
	
	public int select_corner_typeA(int x, int y) {
		if (getTextureUp(x, y) == MapTextureID.GROUND && getTextureRight(x, y) == MapTextureID.GROUND)
			return 0;
		else if (getTextureUp(x, y) == MapTextureID.GROUND && getTextureLeft(x, y) == MapTextureID.GROUND)
			return 1;
		else if (getTextureDown(x, y) == MapTextureID.GROUND && getTextureLeft(x, y) == MapTextureID.GROUND)
			return 2;
		else if (getTextureDown(x, y) == MapTextureID.GROUND && getTextureRight(x, y) == MapTextureID.GROUND)
			return 3;
		return 4;
	}

	
	
	
	
	
	//CORNER DE TYPE B
	public boolean is_corner_B(int x, int y) {
		// Si il a une case d'eau voisine(dans un coin)
		return (nb_of_water_tiles_around(x, y) == 1 && (getTextureUpLeft(x, y) == MapTextureID.WATER
				|| getTextureUpRight(x, y) == MapTextureID.WATER || getTextureDownLeft(x, y) == MapTextureID.WATER
				|| getTextureDownRight(x, y) == MapTextureID.WATER));
	}

	public int select_corner_typeB(int x, int y) { // celui ou il n'y a qu'un seul coin d'eau
		if (nb_of_water_tiles_around(x, y) == 1) {
			if (getTextureUpLeft(x, y) == MapTextureID.WATER)
				return 0; // upleft est water
			else if (getTextureUpRight(x, y) == MapTextureID.WATER)
				return 1; // upRight est water
			else if (getTextureDownLeft(x, y) == MapTextureID.WATER)
				return 2; // downleft est water
			else if (getTextureDownRight(x, y) == MapTextureID.WATER)
				return 3; // downRight est water
		}
		return 4; // ce n'est pas un corner typeB
	}

	

	
	
	// GETTERS TEXTURE rewritten

	public int getTextureRight(int x, int y) {
		return mapTexture[x][(y + 1 + mapWidth) % mapWidth];
	}

	public int getTextureLeft(int x, int y) {
		return mapTexture[x][(y - 1 + mapWidth) % mapWidth];
	}

	public int getTextureDown(int x, int y) {
		return mapTexture[(x + 1 + mapHeight) % mapHeight][y];
	}

	public int getTextureUp(int x, int y) {
		return mapTexture[(x - 1 + mapHeight) % mapHeight][y];
	}

	public int getTextureUpRight(int x, int y) {
		return mapTexture[(x - 1 + mapHeight) % mapHeight][(y + 1 + mapWidth) % mapWidth];
	}

	public int getTextureUpLeft(int x, int y) {
		return mapTexture[(x - 1 + mapHeight) % mapHeight][(y - 1 + mapWidth) % mapWidth];
	}

	public int getTextureDownRight(int x, int y) {
		return mapTexture[(x + 1 + mapHeight) % mapHeight][(y + 1 + mapWidth) % mapWidth];
	}

	public int getTextureDownLeft(int x, int y) {
		return mapTexture[(x + 1 + mapHeight) % mapHeight][(y - 1 + mapWidth) % mapWidth];
	}
	
	
	
	
	//GETTERS ALTITUDE rewritten
	
	public double getAltitude(int x, int y) {
		return mapAltitude[x][y];
	}

	public double getAltitudeRight(int x, int y) {
		return mapAltitude[x][(y + 1 + mapWidth) % mapWidth];
	}

	public double getAltitudeLeft(int x, int y) {
		return mapAltitude[x][(y - 1 + mapWidth) % mapWidth];
	}

	public double getAltitudeDown(int x, int y) {
		return mapAltitude[(x + 1 + mapHeight) % mapHeight][y];
	}

	public double getAltitudeUp(int x, int y) {
		return mapAltitude[(x - 1 + mapHeight) % mapHeight][y];
	}

	public double getAltitudeUpRight(int x, int y) {
		return mapAltitude[(x - 1 + mapHeight) % mapHeight][(y + 1 + mapWidth) % mapWidth];
	}

	public double getAltitudeDownRight(int x, int y) {
		return mapAltitude[(x + 1 + mapHeight) % mapHeight][(y + 1 + mapWidth) % mapWidth];
	}

	public double getAltitudeUpLeft(int x, int y) {
		return mapAltitude[(x - 1 + mapHeight) % mapHeight][(y - 1 + mapWidth) % mapWidth];
	}

	public double getAltitudeDownLeft(int x, int y) {
		return mapAltitude[(x + 1 + mapHeight) % mapHeight][(y - 1 + mapWidth) % mapWidth];
	}
	
	public int getHighest_pX() {
		return highest_pX;
	}
	
	public int getHighest_pY() {
		return highest_pY;
	}
	
	
	public int getEntitiesUp(int x, int y) {
		return mapEntities[x][(y - 1 + mapWidth) % mapWidth];
	}

	public int getEntitiesDown(int x, int y) {
		return mapEntities[x][(y + 1 + mapWidth) % mapWidth];
	}

	public int getEntitiesLeft(int x, int y) {
		return mapEntities[(x - 1 + mapHeight) % mapHeight][y];
	}

	public int getEntitiesRight(int x, int y) {
		return mapEntities[(x + 1 + mapHeight) % mapHeight][y];
	}

	public int getEntitiesUpRight(int x, int y) {
		return mapEntities[(x + 1 + mapHeight) % mapHeight][(y - 1 + mapWidth) % mapWidth];
	}

	public int getEntitiesDownRight(int x, int y) {
		return mapEntities[(x + 1 + mapHeight) % mapHeight][(y + 1 + mapWidth) % mapWidth];
	}

	public int getEntitiesUpLeft(int x, int y) {
		return mapEntities[(x - 1 + mapHeight) % mapHeight][(y - 1 + mapWidth) % mapWidth];
	}

	public int getEntitiesDownLeft(int x, int y) {
		return mapEntities[(x - 1 + mapHeight) % mapHeight][(y + 1 + mapWidth) % mapWidth];
	}
	
	
	
	
	
	public void affichageBruitPerlin() {
		for (int x = 0; x < mapWidth; x++) { 
			for (int y = 0; y < mapHeight; y++)
			  System.out.print(String.format("%.2f ", mapAltitude[x][y]));
			System.out.println();
		}
	}
}