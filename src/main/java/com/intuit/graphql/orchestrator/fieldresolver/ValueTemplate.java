package com.intuit.graphql.orchestrator.fieldresolver;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;

import java.io.StringWriter;
import java.util.Map;

/**
 * This uses org.apache.velocity.app.VelocityEngine in a separate instance mode.  Meaning this class should be instantiated
 * for every use.
 */
public class ValueTemplate {

    private final VelocityEngine engine = new VelocityEngine();

    public ValueTemplate(String stringTemplate) {
        engine.setProperty(Velocity.RESOURCE_LOADERS, "string");
        engine.addProperty("resource.loader.string.class", StringResourceLoader.class.getName());
        engine.addProperty("resource.loader.string.repository.static", "false");
        engine.init();

        StringResourceRepository repo = (StringResourceRepository) engine.getApplicationAttribute(StringResourceLoader.REPOSITORY_NAME_DEFAULT);
        repo.putStringResource("1", stringTemplate);
    }

    public String compile(Map<String, Object> dataSource) {
        VelocityContext context = new VelocityContext();

        dataSource.keySet().stream()
                .forEach(key -> context.put(key, dataSource.get(key)));

        // Get and merge the template with my parameters.
        Template template = engine.getTemplate("1");
        StringWriter writer = new StringWriter();
        template.merge(context, writer);

        return writer.toString();
    }

}

