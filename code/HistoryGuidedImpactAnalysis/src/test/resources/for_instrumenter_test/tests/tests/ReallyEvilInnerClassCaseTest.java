package tests;

import mainPackage.ReallyEvilInnerClassCase;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ReallyEvilInnerClassCaseTest {

   @Test
   public void unitTest() {
      assertTrue(ReallyEvilInnerClassCase.innerWithoutEnclosing.getValue()==-1);
      ReallyEvilInnerClassCase c = new ReallyEvilInnerClassCase();
      assertTrue(c.getValue()==1);
      assertTrue(ReallyEvilInnerClassCase.innerWithoutEnclosing.getValue()==-1);
   }

}
