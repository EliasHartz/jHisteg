package tests;

import mainPackage.SomeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class SingleThreadedTest {

   @Test
   public void unitTest() {
      SomeClass c = new SomeClass();
      assertTrue(c.getBoolean());
      assertTrue(c.getShort()==42);
      assertTrue(c.getByte()==42);
      assertTrue(SomeClass.echoAnInt(-726)!=123);
      assertTrue(c.echoAnInt(123)==123); //stupid static access!
      assertTrue(c.getChar()=='c');
      assertTrue(c.getInteger()==42);
      assertTrue(c.getLong()==42L);
      assertTrue(c.getDouble()==42.0);
      assertTrue(c.getFloat()==42.0f);
      assertTrue(c.getString().equals("someStringValueWith42inIt"));
      assertFalse(c.equals(new SomeClass(41)));
      assertEquals(5, c.recursiveCall(5));
   }

}
