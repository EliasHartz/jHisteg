package tests;

import mainPackage.SomeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SomeMinimalTest {

   @Test
   public void unitTest() {
      assertTrue(SomeClass.echoAnInt(12345)==12345);
   }

}
