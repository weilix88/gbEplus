package main.java.plugins.ashraebaseline.system7;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import main.java.model.data.EplusObject;
import main.java.model.data.KeyValuePair;
import main.java.model.gbXML.CampusTranslator;
import main.java.model.gbXML.GbXMLSpace;
import main.java.model.idf.IDFWriter;
import main.java.plugins.ashraebaseline.HVACSystemImplUtil;

/**
 * PSZ-AC, every zone has its own AHU
 * 
 * @author Weili
 *
 */
public class HVACSystem7 implements SystemType7 {
	// recording all the required data for HVAC system type 7
	private HashMap<String, ArrayList<EplusObject>> objectLists;
	
	private CampusTranslator building;	

	// building object contains building information and energyplus data
	public HVACSystem7(HashMap<String, ArrayList<EplusObject>> objects, CampusTranslator building) {
		objectLists = objects;
		this.building = building;
		
		processSystems();
//		processSystems2();
	}

	@Override
	public HashMap<String, ArrayList<EplusObject>> getSystemData() {
		return objectLists;
	}

	private void processSystems() {
		ArrayList<EplusObject> supplySideSystem = new ArrayList<EplusObject>();
		ArrayList<EplusObject> demandSideSystem = new ArrayList<EplusObject>();

		ArrayList<EplusObject> supplySideSystemTemplate = objectLists.get("Supply Side System");
		ArrayList<EplusObject> demandSideSystemTemplate = objectLists.get("Demand Side System");
		ArrayList<EplusObject> plantSideSystemTemplate = objectLists.get("Plant");

		HashMap<String, GbXMLSpace> spaceMap = building.getSpaceMap();
		
		ArrayList<String> floors = new ArrayList<String>();
		ArrayList<String> zones = new ArrayList<String>();
		
		int numberOfRoom=0;
		
		Iterator<String> Inte = spaceMap.keySet().iterator();
		ArrayList<String> zonefloorInfo = new ArrayList<String>();
		while (Inte.hasNext()) {
			numberOfRoom=numberOfRoom+1;
			String key = Inte.next();
			GbXMLSpace space = spaceMap.get(key);
			String[] zoneNameInfo = space.getSpaceName().split(":");
			zonefloorInfo.add(space.getSpaceName());
			zones.add(zoneNameInfo[1]);
			if (!floors.contains(zoneNameInfo[0])) {
				floors.add(zoneNameInfo[0]);
//				System.out.print(zoneNameInfo[0]);
			}
		}

		Iterator<String> spaceKeyItr = spaceMap.keySet().iterator();
		ArrayList<String> floorMap = new ArrayList<String>(); 
		while (spaceKeyItr.hasNext()) {
			String key = spaceKeyItr.next();
			GbXMLSpace space = spaceMap.get(key);
			String[] zoneNameInfo = space.getSpaceName().split(":");
			if (!floorMap.contains(zoneNameInfo[0])) {
				floorMap.add(zoneNameInfo[0]);
				supplySideSystem.addAll(processSupplyTemp(zoneNameInfo[0],space.getSpaceName(), supplySideSystemTemplate, zonefloorInfo));
			}
			demandSideSystem.addAll(processDemandTemp(zoneNameInfo[0],space.getSpaceName(), demandSideSystemTemplate));
		}
		
		EplusObject temp = new EplusObject("BranchList","Plant");
		KeyValuePair PairBranch1 = new KeyValuePair("Name","Chilled Water Loop Chilled Water Demand Side Branches");
		KeyValuePair PairBranch2 = new KeyValuePair("Branch 1 Name","Chilled Water Loop Chilled Water Demand Inlet Branch");
		KeyValuePair PairBranch3 = new KeyValuePair("Branch 2 Name","Chilled Water Loop Chilled Water Demand Bypass Branch");
		temp.addField(PairBranch1);
		temp.addField(PairBranch2);
		temp.addField(PairBranch3);
		
		Iterator<String> spaceKeyItr2 = spaceMap.keySet().iterator();
		ArrayList<String> floorMap2 = new ArrayList<String>(); 
		int number=3;
		while(spaceKeyItr2.hasNext()) {
			String key = spaceKeyItr2.next();
			GbXMLSpace space = spaceMap.get(key);
			String[] zoneNameInfo = space.getSpaceName().split(":");
			if (!floorMap2.contains(zoneNameInfo[0])) {
				floorMap2.add(zoneNameInfo[0]);
				KeyValuePair chillerPair = new KeyValuePair("Branch "+number+" Name",zoneNameInfo[0]+" Cooling Coil Chilled Water Branch");
				number=number+1;
				temp.addField(chillerPair);
			}
		}
		
		KeyValuePair endPair = new KeyValuePair("Branch 6 Name","Chilled Water Loop Chilled Water Demand Outlet Branch");
		temp.addField(endPair);
		
		demandSideSystem.add(temp);
		
		Iterator<String> spaceKeyItr3 = spaceMap.keySet().iterator();
		ArrayList<String> floorMap3 = new ArrayList<String>(); 
		int number2=2;
		while(spaceKeyItr3.hasNext()) {
			String key = spaceKeyItr3.next();
			GbXMLSpace space = spaceMap.get(key);
			String[] zoneNameInfo = space.getSpaceName().split(":");
			if (!floorMap3.contains(zoneNameInfo[0])) {
				EplusObject temp2 = new EplusObject("NodeList","Plant");
				floorMap3.add(zoneNameInfo[0]);
				KeyValuePair namePair = new KeyValuePair("Name",zoneNameInfo[0]+" Supply Fan");
				temp2.addField(namePair);
				KeyValuePair fanPair = new KeyValuePair("Node 1 Name",zoneNameInfo[0]+" Supply Fan Outlet");
				temp2.addField(fanPair);
				for(String zonefloor:zonefloorInfo) {
					String[] zonefloorName = zonefloor.split(":");
					if (zonefloorName[0].equals(zoneNameInfo[0])) {
						KeyValuePair VAVPair = new KeyValuePair("Node"+number2+"Name",zonefloor+" Zone Equip Inlet");
						temp2.addField(VAVPair);
					}
				}
				demandSideSystem.add(temp2);
			}
		}
		
		ArrayList<String> floorMap4 = new ArrayList<String>(); 
		EplusObject tempSplitter = new EplusObject("Connector:Splitter","Plant");
		EplusObject tempMixer = new EplusObject("Connector:Mixer","Plant");
		KeyValuePair PairSplitter1 = new KeyValuePair("Name","Chilled Water Loop Chilled Water Demand Splitter");
		KeyValuePair PairSplitter2 = new KeyValuePair("Inlet Branch Name","Chilled Water Loop Chilled Water Demand Inlet Branch");
		KeyValuePair PairSplitter3 = new KeyValuePair("Outlet Branch 1 Name","Chilled Water Loop Chilled Water Demand Bypass Branch");
		tempSplitter.addField(PairSplitter1);
		tempSplitter.addField(PairSplitter2);
		tempSplitter.addField(PairSplitter3);
		KeyValuePair PairMixer1 = new KeyValuePair("Name","Chilled Water Loop Chilled Water Demand Mixer");
		KeyValuePair PairMixer2 = new KeyValuePair("Outlet Branch Name","Chilled Water Loop Chilled Water Demand Outlet Branch");
		KeyValuePair PairMixer3 = new KeyValuePair("Inlet Branch 1 Name","Chilled Water Loop Chilled Water Demand Bypass Branch");
		tempMixer.addField(PairMixer1);
		tempMixer.addField(PairMixer2);
		tempMixer.addField(PairMixer3);
		for(String zonefloor:zonefloorInfo) {
			String[] zonefloorName = zonefloor.split(":");
			if (!floorMap4.contains(zonefloorName[0])) {
				floorMap4.add(zonefloorName[0]);
				KeyValuePair SplitterPair = new KeyValuePair("Outlet Branch 2 Name", zonefloorName[0] + " Cooling Coil Chilled Water Branch");
				KeyValuePair MixerPair = new KeyValuePair("Inlet Branch 2 Name", zonefloorName[0] + " Cooling Coil Chilled Water Branch");
				tempSplitter.addField(SplitterPair);
				tempMixer.addField(MixerPair);
			}
		}
		supplySideSystem.add(tempSplitter);
		supplySideSystem.add(tempMixer);
		
		objectLists.put("Supply Side System", supplySideSystem);
		objectLists.put("Demand Side System", demandSideSystem);
		
	}

	/**
	 * process the HVAC supply air side system
	 * 
	 * @param zone
	 * @param supplySideSystemTemplate
	 * @return
	 */
	private ArrayList<EplusObject> processSupplyTemp(String floor, String zone, ArrayList<EplusObject> supplySideSystemTemplate, ArrayList<String> zonefloorInfo) {
		ArrayList<EplusObject> supplyTemp = new ArrayList<EplusObject>();
		for (EplusObject eo : supplySideSystemTemplate) {
			EplusObject temp = eo.clone();

			/*
			 * replace the special characters that contains floors
			 */
			if (temp.contains("Floor%")) {
				temp.replaceSpecialCharacters(floor);
			}
			// side systems
			if (temp.getObjectName().equalsIgnoreCase("AirLoopHVAC:ZoneSplitter")) {
				for(String zonefloor:zonefloorInfo) {
					String[] zonefloorName = zonefloor.split(":");
					if (temp.getKeyValuePair(0).getValue().equals(zonefloorName[0]+" Zone Splitter")) {
//						System.out.print(temp.getKeyValuePair(0).getValue());
//						System.out.print(zonefloorName[0]);
						KeyValuePair splitterPair = new KeyValuePair("Outlet Node Name", zonefloor + " Zone Equip Inlet");
						temp.addField(splitterPair);
					}
				}
			}

			// check if this is the connection between supply side and demand
			// side systems
			if (temp.getObjectName().equalsIgnoreCase("AirLoopHVAC:ZoneMixer")) {
				for(String zonefloor:zonefloorInfo) {
					String[] zonefloorName = zonefloor.split(":");
					if (temp.getKeyValuePair(0).getValue().equals(zonefloorName[0]+" Zone Mixer")) {
						KeyValuePair mixerPair = new KeyValuePair("Inlet 1 Node Name", zonefloor+" Supply Inlet");
						temp.addField(mixerPair);
					}
				}
			}
			
			supplyTemp.add(temp);
		}
		return supplyTemp;
	}

	/**
	 * process the demand side system
	 * 
	 * @param zone
	 * @param zoneTemp
	 * @return
	 */
	private ArrayList<EplusObject> processDemandTemp(String floor, String zone, ArrayList<EplusObject> zoneTemp) {
		ArrayList<EplusObject> demandTemp = new ArrayList<EplusObject>();
		for (EplusObject eo : zoneTemp) {
			EplusObject temp = eo.clone();
			if (temp.contains("Zone%")) {
				temp.replaceSpecialCharacters(zone);
			}
			demandTemp.add(temp);
		}
		return demandTemp;
	}
}
