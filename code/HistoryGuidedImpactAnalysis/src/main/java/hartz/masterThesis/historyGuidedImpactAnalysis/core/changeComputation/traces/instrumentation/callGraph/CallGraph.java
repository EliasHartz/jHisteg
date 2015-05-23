package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.instrumentation.callGraph;

import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONArray;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Models the call graph of an entire version, aka contains data about which method *CAN* call
 * which other methods given the current source code. If such a call is ever reached cannot be
 * determined.
 */
public class CallGraph {

    private final HashMap<String, String[]> callGraph ;

    /**
     * Creates a new call graph.
     * @param callGraphWithList : HashMap derived from instrumentation
     */
    public CallGraph( HashMap<String, List<String>> callGraphWithList){
        callGraph = new HashMap<>();
        for (String s : callGraphWithList.keySet()){
            callGraph.put(s, callGraphWithList.get(s).toArray(new String[0]));
        }
    }

    /**
     * Imports a call graph from JSON.
     *
     * @param a : JSONArray containing the call graph
     */
    public CallGraph(JSONArray a){
        callGraph = new HashMap<>();
        for (int i = 0; i<a.length(); ++i){
            JSONObject o = a.getJSONObject(i);
            String caller = o.getString("method");

            JSONArray a2 = o.getJSONArray("canCall");
            String[] l = new String[a2.length()];
            for (int j = 0; j<a2.length(); ++j){
                l[j] = a2.getString(j);
            }
            callGraph.put(caller,l);
        }
    }

    /**
     * Exports this call graph to JSON.
     *
     * @return new JSONArray containing the call graph
     */
    public JSONArray toJSON(){
        JSONArray result = new JSONArray();
        for (String s : callGraph.keySet()){
            JSONObject o = new JSONObject();
            o.put("method", s);
            o.put("canCall", new JSONArray(callGraph.get(s)));
            result.put(o);
        }
        return result;
    }

    /**
     * Shortest-path method.
     *
     * @param from : origin
     * @param toFind : a set of destinations
     * @return map containing the distances of each destination from the
     *         origin, Integer.MIN_VALUE encodes that there is no path!
     */
    public HashMap<String, Integer> getDistanceTo(String from, HashSet<String> toFind){
        HashMap<String, Integer> result = new HashMap<>(); //encodes distances
        HashSet<String> visitedAlready = new HashSet<>(); //used to avoid unnecessary computation
        LinkedList<String> toVisit = new LinkedList<>(); //to visit!

        int distance = 0;
        int sizeOfCurrentDistance = 1;
        int sizeOfNextDistance = 0;
        toVisit.add(from);
        visitedAlready.add(from); //remember that we visited this one

        while(!toVisit.isEmpty()){
            String currentMethod = toVisit.poll();
            assert (visitedAlready.contains(currentMethod)); //otherwise the algorithm is broken!

            if (toFind.contains(currentMethod) && !result.containsKey(currentMethod)) {//if we have found the shortest distance
                result.put(currentMethod, distance);

                if (result.keySet().containsAll(toFind)) //if we are done ( = found all)
                    //we are done
                    return result;
            }

            int amountAdded = 0;
            if (callGraph.containsKey(currentMethod)) { //if this calls something!
                String[] calledByIt = callGraph.get(currentMethod);
                for (String s : calledByIt) {
                    if (!visitedAlready.contains(s)) { //add only methods that we have not visited yet!
                        toVisit.add(s);
                        visitedAlready.add(s); //remember we have already found a way to this one!
                        ++amountAdded;
                    }
                }
            } //else it does not call something or it is foreign source code, e.g. some java.lang.BLUB

            //update our distances counter!
            if ((--sizeOfCurrentDistance) == 0)
                ++distance;
            if ( sizeOfCurrentDistance == 0){
                    sizeOfCurrentDistance = sizeOfNextDistance + amountAdded;
                    sizeOfNextDistance = 0;
            } else {
                sizeOfNextDistance += amountAdded;
            }
        }


        //if we did not find all, construct a full map with all entries nonetheless!
        if (!result.keySet().containsAll(toFind)){
            for (String s : toFind){
                if (!result.containsKey(s))
                    result.put(s, Integer.MIN_VALUE);
            }
        }

        return result;
    }

    /**
     * Computes the minimal call distance between two methods.
     *
     * @param callee : method identifier of method being called (may not be possible though)
     * @param caller : method identifier of supposed caller
     * @return minimum number of calls between caller and callee with
     *         Integer.MIN_VALUE encoding that the caller cannot call
     *         the given method.
     */
    public int getDistanceToCaller(String callee, String caller) {
        if (!callGraph.containsKey(caller)) return Integer.MIN_VALUE; //no point in searching
        HashSet<String> h = new HashSet<>();
        h.add(callee);
        return getDistanceTo(caller, h).get(callee);
    }

}
