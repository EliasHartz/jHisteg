package hartz.masterThesis.historyGuidedImpactAnalysis.core.versions;

import hartz.masterThesis.historyGuidedImpactAnalysis.configuration.constants.Globals;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONArray;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class VersionList implements Iterable<Version>{

	private final ArrayList<Version> versionsByIndex;	
	private final HashMap<String, Version> versionsByIdentifier;

	/**
	 * Creates a new commit list, managing the commits by both their index in the history
	 * and their version identifier (e.g. their hash).
	 * 
	 * @param indexToVersion : simply a list of all versions
	 * @param identifierToVersion : an (unordered) HashMap that maps each identifier to a commit
	 */
	public VersionList(ArrayList<Version> indexToVersion, HashMap<String, Version> identifierToVersion) {
		this.versionsByIndex = indexToVersion;
		this.versionsByIdentifier = identifierToVersion;
	}
	
	/**
	 * Loads a version list from a JSONArray.
	 * 
	 * @param array : array loaded from a previously exported commit list
	 */
	public VersionList(JSONArray array){
		versionsByIndex = new ArrayList<>();
		versionsByIdentifier = new HashMap<>();
		for (int i = 0; i<array.length(); ++i){
			//load each and every commit
			Version c = new Version(array.getJSONObject(i));
			versionsByIndex.add(c.index, c);
			versionsByIdentifier.put(c.identifier, c);
		}
	}

	@Override
	public Iterator<Version> iterator() {
		return versionsByIndex.iterator();
	}

	/**
	 * Obtains a specific version by its numeric index in
	 * the version history of this list (first commit is 0).
	 * 
	 * @param index : desired version's index
	 * @return the version associated with the identifier or 
	 *         'null' if the given index is invalid
	 */
	public Version getVersionByIndex(int index){
		if (index>=0 && index<versionsByIndex.size())
			return versionsByIndex.get(index);
		else{
		   Tool.printError("Version identifier'"+index+"' is invalid.");
			return null;
		}
	}
	
	/**
	 * Exports this Version's data into a given file.
	 * Will store the data as JSON.
	 * 
	 * @param file : where to store
	 * @throws Exception : in case export fails due to IO problems
	 */
	
	public void writeIntoFile(File file) throws IOException{
		file.createNewFile();
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write(toJSONArray().toString(Globals.jsonIndentFactor));//store as JSON
		writer.close();
	}
	
	public int indexOf(Version v){
	  return versionsByIndex.indexOf(v);
	}

	/**
	 * Obtains a specific version by its identifier, e.g.
	 * a hash for some repositories.
	 * 
	 * @param identifier : a string identifying a specific commit
	 * @return the version associated with the identifier or 
	 *         'null' if the given identifier cannot be mapped
	 */
	public Version getVersion(String identifier){
		if (versionsByIdentifier==null){
			//fall-back if no versions have been specified, try using the index:
			int i = -1;
			try{
				i = Integer.parseInt(identifier);
			}
			catch (NumberFormatException e){
				try{
					//second attempt, remove first character (e.g. for SVN-style identifiers like 'r1242')
					identifier = identifier.substring(1);
					if (identifier.isEmpty())
						throw new NumberFormatException();
					i = Integer.parseInt(identifier);
				}
				catch (NumberFormatException e2){
				   Tool.printError("Cannot convert version identifier'"+identifier+"' to an integer.");
					return null;
				}
			}
			return getVersionByIndex(i);
		}
		else //normal behavior 
			return versionsByIdentifier.get(identifier);
	}

	public JSONArray toJSONArray() {
		JSONArray a = new JSONArray();
		for (Version c : versionsByIndex){
			a.put(c.toJSON());
		}
		return a;
	}

	public int getAmountOfCommits() {
		return versionsByIndex.size();
	}

   public int size() {
      return versionsByIdentifier.size();
   }

}
