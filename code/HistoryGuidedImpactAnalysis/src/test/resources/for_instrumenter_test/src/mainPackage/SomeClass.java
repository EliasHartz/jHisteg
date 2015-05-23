package mainPackage;

public class SomeClass {

   protected final String someString;
   protected final boolean someBoolean;
   protected final int someInteger;
   protected final char someChar;
   protected final byte someByte;
   protected final short someShort;
   protected final long someLong;
   protected final double someDouble;
   protected final float someFloat;
   
   /**
    * Creates a new object.
    */
   public SomeClass(){
      someString = "someStringValueWith42inIt";
      someBoolean = true;
      someChar = 'c';
      someShort = 42;
      someByte = 42;
      someInteger = 42;
      someLong = 42L;
      someDouble = 42.0;
      someFloat = 42.0f;
   }
   
   /**
    * Creates a new object with a number
    */
   public SomeClass(int number){
      someString = "someStringValueWith"+number+"inIt";
      someBoolean = false;
      someChar = (char) number;
      someShort = (short) number;
      someByte = (byte) number;
      someInteger = number;
      someLong = number;
      someDouble = number;
      someFloat = number;
   }
  
   public SomeClass getObject() {
      return this;
   }
   
   public String getString() {
      return someString;
   }

   public boolean getBoolean() {
      return someBoolean;
   }

   public short getShort() {
      return someShort;
   }

   public byte getByte() {
      return someByte;
   }

   public char getChar() {
      return someChar;
   }

   public int getInteger() {
      return someInteger;
   }

   public long getLong() {
      return someLong;
   }

   public double getDouble() {
      return someDouble;
   }

   public float getFloat() {
      return someFloat;
   }
   
   public int recursiveCall(int numberOfTimesToRecurse) {
      if (numberOfTimesToRecurse <= 0) return 0;
      else {
         recursiveCall(numberOfTimesToRecurse-1);
         return numberOfTimesToRecurse;
      }
   }
   
   public int doSingleLineLoop(int i){
      while(i!=0) i--;
      return i;
   }

   public static int echoAnInt(int i) {
      return i;
   }

   public static boolean echoABool(boolean b) {
      return b;
   }
   
   @Override
   public String toString(){
      return someString;
   }
   
   @Override
   public int hashCode() {
      return someBoolean ? someInteger : -someInteger ;
   }
   
   @Override
   public boolean equals(Object o){
      if (o instanceof SomeClass){
         return ((SomeClass) o).getInteger() == someInteger && ((SomeClass) o).getBoolean() == someBoolean;
      }
      return false;
   }
}
