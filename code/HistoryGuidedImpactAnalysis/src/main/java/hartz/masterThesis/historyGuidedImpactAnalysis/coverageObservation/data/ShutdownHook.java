package hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data;

import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.CoverageObserver;

public class ShutdownHook implements Runnable {

   public void run() {
      CoverageObserver.getCurrentInstance().export();
   }
}
