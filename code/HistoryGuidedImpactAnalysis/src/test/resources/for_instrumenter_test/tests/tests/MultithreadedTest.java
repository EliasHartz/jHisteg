package tests;

import mainPackage.SomeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class MultithreadedTest {

   @Test
   public void unitTest() throws Exception {
      final SomeClass c = new SomeClass();

      Thread t1 = new Thread(new Runnable() {
         @Override
         public void run() {
	      assertTrue(c.getBoolean());
	      assertTrue(c.getInteger()==42);
	      assertTrue(c.getLong()==42L);
	      assertTrue(c.getDouble()==42.0);
	      assertTrue(c.getFloat()==42.0f);
	      assertFalse(c.equals(new SomeClass(41)));
	      assertEquals(2, c.recursiveCall(2));
         }
      });
      
      Thread t2 = new Thread(new Runnable() {
         @Override
         public void run() {
	      SomeClass c2 = new SomeClass(41);
	      assertFalse(c2.getBoolean());
	      assertTrue(c2.getInteger()==41);
	      assertTrue(c2.getLong()==41L);
	      assertTrue(c2.getDouble()== (double) 41);
	      assertTrue(c2.getFloat()== (float) 41);
	      assertTrue(c2.equals(new SomeClass(41)));
	      assertFalse(c.equals(c2));
	      assertEquals(2, c.recursiveCall(2));
         }
      });

      t1.start();
      t2.start();

      t1.join();
      t2.join();
   }

}
