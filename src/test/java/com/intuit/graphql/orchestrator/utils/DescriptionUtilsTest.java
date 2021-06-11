package com.intuit.graphql.orchestrator.utils;

import static com.intuit.graphql.orchestrator.utils.DescriptionUtils.attachNamespace;
import static com.intuit.graphql.orchestrator.utils.DescriptionUtils.mergeDescriptions;

import org.junit.Assert;
import org.junit.Test;

public class DescriptionUtilsTest {

  @Test
  public void attachNameSpaceTest(){
    Assert.assertEquals(attachNamespace("ns","desc"),"[ns] desc");
    Assert.assertEquals(attachNamespace("ns",null),"[ns]");
  }

  @Test
  public void mergeDescrtiptionTest(){
    String firstmerge = mergeDescriptions("[ns1] desc1", "[ns2] desc2");
    Assert.assertEquals(firstmerge,"[ns1,ns2] desc1, desc2");
    Assert.assertEquals(mergeDescriptions(firstmerge, firstmerge),"[ns1,ns2,ns1,ns2] desc1, desc2, desc1, desc2");
    Assert.assertEquals(mergeDescriptions(firstmerge, "[ns3] desc3"),"[ns1,ns2,ns3] desc1, desc2, desc3");
    Assert.assertEquals(mergeDescriptions(firstmerge, "[ns3]"),"[ns1,ns2,ns3] desc1, desc2");
    Assert.assertEquals(mergeDescriptions(firstmerge, ""),firstmerge);
  }
}
