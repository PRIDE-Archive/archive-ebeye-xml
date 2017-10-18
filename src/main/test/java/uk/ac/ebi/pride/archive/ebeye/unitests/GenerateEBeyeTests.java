package uk.ac.ebi.pride.archive.ebeye.unitests;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.method.P;
import uk.ac.ebi.pride.archive.ebeye.GenerateEBeyeXML;

public class GenerateEBeyeTests {
  private final static Logger logger = LoggerFactory.getLogger(GenerateEBeyeTests.class);

  @Test
  public void emptyTest() {
    GenerateEBeyeXML generateEBeyeXML = new GenerateEBeyeXML();
    generateEBeyeXML.generate();
  }

  @Test
  public void nonEmptyTest() {
    GenerateEBeyeXML generateEBeyeXML = new GenerateEBeyeXML();
    generateEBeyeXML.setProject(null);
    generateEBeyeXML.setSubmission(null);
    generateEBeyeXML.setOutputDirectory(null);
    generateEBeyeXML.setProteins(null);
    generateEBeyeXML.setFromPride(false);
    generateEBeyeXML.generate();
  }

  @Test
  public void prideProjectTest() {
    //todo stub project, test output file OK
    GenerateEBeyeXML generateEBeyeXML = new GenerateEBeyeXML(null, null, null, null, true);
    generateEBeyeXML.generate();
  }

  @Test
  public void nonPrideProjectTest() {
    //todo stub project, test output file OK
    GenerateEBeyeXML generateEBeyeXML = new GenerateEBeyeXML(null, null, null, null);
    generateEBeyeXML.generate();
  }
}
