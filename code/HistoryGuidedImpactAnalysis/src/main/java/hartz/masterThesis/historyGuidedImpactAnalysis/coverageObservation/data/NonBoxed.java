package hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data;


public abstract class NonBoxed {
   
   /* This class is a bit unusual...
    * I want to make the distinction between a method returning an object and a
    * method returning a primitive value but my mechanism stores only objects.
    * This means I need to box the primitives, to remember that I boxed them
    * however I need my own boxing class.
    * 
    * A NonBoxed object therefore boxes a primitive but its existance means
    * that a real primitive was returned.
    * 
    * This empty class exists only to make the instanceof-check easier.
    */

   public abstract String getTypeString();
   public abstract String getValueAsString();
}
