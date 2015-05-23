package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.fileLevelChanges;

import hartz.masterThesis.historyGuidedImpactAnalysis.core.versions.Version;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;

import java.io.File;

/**
 * Models a change on the file-level.
 */
public class FileLevelChange {

	private final FileDiffType type;
	private final String relativePathInRepo; //this is always in the newer version BUT for 'deletion' of course!
	private final boolean partOfSource;

	/**
	 * Constructs a new file-level change.
	 *
	 * @param type : type of change
	 * @param partOfSource : 'true' if this change is believed to affect a file containing
	 *                       project source code (e.g. not a test or some other file). In
	 *                       the current implementation, this is ALWAYS based on the new
	 *                       version and therefore models if e.g. if a now eliminated file
	 *                       would have been part of this version's source!
	 * @param relativePathInNewVersion : the relative path inside the current version leading
	 *                                   to this file, a relative path is a path that treats
	 *                                   the 'extracted'-directory has the root of the file
	 *                                   system.
	 * @param relativePathInOldVersion : the relative path inside the previous version leading
	 *                                   to this file.  *NOTE:* We do not model a 'move' at the
	 *                                   moment hence only *ONE* relative path parameter must
	 *                                   be provided!
	 */
	public FileLevelChange(FileDiffType type, boolean partOfSource, String relativePathInNewVersion, String relativePathInOldVersion){
		assert (relativePathInNewVersion != null || relativePathInOldVersion != null); //we have some path
		assert (!(relativePathInNewVersion != null && relativePathInOldVersion != null));  //but not both are set!

		String relativePath = relativePathInNewVersion!=null ? relativePathInNewVersion : relativePathInOldVersion;
		if (partOfSource && !relativePath.toLowerCase().endsWith(".java")){
			Tool.printDebug("relative path '"+relativePath+"' is not a java file but was flagged as part of the source code!");
			partOfSource = false;
		}

		this.type = type;
		this.partOfSource = partOfSource;
		this.relativePathInRepo = relativePath;
	}

	/**
	 * Loads a file-level change from a JSON-object.
	 *
	 * @param jsonObject : the change to load
	 */
	public FileLevelChange(JSONObject jsonObject) {
		this.type = FileDiffType.valueOf(jsonObject.getString("type"));
		this.relativePathInRepo = jsonObject.getString("pathInRepo");
		this.partOfSource = jsonObject.optBoolean("partOfSource", true);
	}

    /**
     * @return a JSONObject containing all relevant data of this instance
     */
	public JSONObject toJSONObject() {
		JSONObject o = new JSONObject();
		o.put("type", type.name());
		o.put("pathInRepo",relativePathInRepo);
		o.put("partOfSource",partOfSource);
		return o;
	}

    /**
     * @return a JSONObject that does not contain the partOfSource-information
     */
	public JSONObject toJSONObjectWithoutPartOfSource() {
		JSONObject o = new JSONObject();
		o.put("type", type.name());
		o.put("pathInRepo",relativePathInRepo);
		return o;
	}

	@Override
	public int hashCode(){
		return (type.ordinal()*10000 * (partOfSource?-1:1)) + relativePathInRepo.hashCode() ;
	}

	@Override
	public String toString(){
		return "["+type.name()+"] "+ relativePathInRepo;
	}


	@Override
	public boolean equals(Object o){
		if (o instanceof FileLevelChange){
			return
					((FileLevelChange) o).type == type &&
					((FileLevelChange) o).partOfSource == partOfSource &&
					relativePathInRepo.equals(((FileLevelChange) o).relativePathInRepo);
		}
		return false;
	}

	public FileDiffType getTypeOfChange(){ return type;}
	public boolean isPartOfSourceCode(){ return partOfSource;}

	/**
	 * Attempts to map this file-level change to an actual file inside
	 * the extracted data of a version. If no mapping can be found, 'null'
	 * will be returned.
	 *
	 * @param v : version to map change to
	 * @return the real file or 'null' if no real path exists for the relative one
	 */
	public File getFile(Version v) {
		/** if we would support moving files at some point, one would need to make a distinction here */
		File f = new File(v.getActualExtractedRepo().getAbsoluteFile()+"/"+relativePathInRepo);
		if (f.exists()) return f;
		else return null;
	}

	/**
	 * @return the relative path that makes most sense in context (considering the type of change)
	 */
	public String getRelativePathForDebug() {
		return relativePathInRepo;
	}
}
