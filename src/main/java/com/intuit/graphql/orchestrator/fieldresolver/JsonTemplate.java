package com.intuit.graphql.orchestrator.fieldresolver;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;

import java.io.StringWriter;
import java.util.Map;

public class JsonTemplate {

    VelocityEngine engine = new VelocityEngine();

    public JsonTemplate(String stringTemplate) {
        engine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.Log4JLogChute");
        //engine.setProperty("runtime.log.logsystem.log4j.logger", LOGGER.getName());
        engine.setProperty(Velocity.RESOURCE_LOADER, "string");
        engine.addProperty("string.resource.loader.class", StringResourceLoader.class.getName());
        engine.addProperty("string.resource.loader.repository.static", "false");
        //  engine.addProperty("string.resource.loader.modificationCheckInterval", "1");
        engine.init();


        // Initialize my template repository. You can replace the "Hello $w" with your String.
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

