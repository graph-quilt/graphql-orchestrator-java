package com.intuit.graphql.orchestrator.datafetcher;

import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.spy;

import com.intuit.graphql.orchestrator.schema.SchemaParseException;
import com.intuit.graphql.orchestrator.xtext.XtextResourceSetBuilder;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({XtextResourceSetBuilder.class})
public class SchemaParseExceptionTest {

  @Test(expected = SchemaParseException.class)
  public void testThrownFromXtextResourceSetBuilder() throws Exception {
    // have XtextResourceSetBuilder.createGraphqlResourceFromString throw an IOException
    // build method should throw a SchemaParseException when it catches the IOException
    XtextResourceSetBuilder mock = spy(XtextResourceSetBuilder.newBuilder());
    doThrow(new IOException("Some IO Error")).when(mock, "createGraphqlResourceFromString", anyString(), anyString());
    mock.file("somefile.graphqls","{Query {id : String}}").build();
  }

}
