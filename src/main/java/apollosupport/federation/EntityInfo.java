package apollosupport.federation;

import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import java.util.Map;
import lombok.Data;

@Data
public class EntityInfo {
  private String entityTypeName;
  private Map<String, RequiredFieldsInfo> requiredFields;
  private Map<String, ExternalFieldsInfo> externalFields;
  private ServiceMetadata serviceMetadata;
}
