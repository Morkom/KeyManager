package dev.morkom.keymanager.util;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import java.io.IOException;
import java.net.URL;

public class ClasspathResourceResolver implements LSResourceResolver {

    private final String baseResourceUrl;

    public ClasspathResourceResolver(String baseResourceUrl) {
        // baseResourceUrl should be a fully resolved URL like "jar:file:/app.jar!/BOOT-INF/classes!/xsd/"
        this.baseResourceUrl = baseResourceUrl;
    }

    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        if (systemId == null) {
            return null;
        }

        try {
            // For Spring Boot fat jars, resolving relative URLs directly from the base URL is the most robust method.
            URL baseUrl = new URL(baseURI != null ? baseURI : baseResourceUrl);
            URL resolvedUrl = new URL(baseUrl, systemId);

            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResource(resolvedUrl.toString());

            if (resource.exists()) {
                LSInputImpl input = new LSInputImpl();
                input.setPublicId(publicId);
                input.setSystemId(resource.getURL().toString());
                input.setBaseURI(resource.getURL().toString());
                input.setByteStream(resource.getInputStream());
                return input;
            }
        } catch (IOException e) {
            // Log error if needed, let parser handle null return
        }

        return null;
    }
}
