package hartz.masterThesis.historyGuidedImpactAnalysis.configuration.constants;

import hartz.masterThesis.historyGuidedImpactAnalysis.main.Commands;

/** Holds main modes of execution for this tool */
public enum Behavior {
	EXTRACT {@Override public String toString(){ return Commands.extractOnly; }},
	COMPILE {@Override public String toString(){ return Commands.compile; }},
	INSTRUMENT {@Override public String toString(){ return Commands.instrument; }},
	DATA_COMPUTATION {@Override public String toString(){ return Commands.analyze; }},
	TARGET_GENERATION {@Override public String toString(){ return Commands.compute; }}
}
