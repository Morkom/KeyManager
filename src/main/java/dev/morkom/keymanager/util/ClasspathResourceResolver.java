package dev.morkom.keymanager.util;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import java.io.InputStream;

public class ClasspathResourceResolver implements LSResourceResolver {

    private final String baseResourcePath;

    public ClasspathResourceResolver(String baseResourcePath) {
        // Ensure the base path ends with a slash
        this.baseResourcePath = baseResourcePath.endsWith("/") ? baseResourcePath : baseResourcePath + "/";
    }

    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        // systemId will be the relative path like "./my-schema.xsd"
        // We resolve it against our classpath base path.
        String resourcePath = baseResourcePath + systemId;
        InputStream resourceStream = getClass().getResourceAsStream(resourcePath);

        if (resourceStream == null) {
            return null; // Let the parser handle it or fail
        }

        LSInputImpl input = new LSInputImpl();
        input.setPublicId(publicId);
        input.setSystemId(systemId);
        input.setBaseURI(baseURI);
        input.setByteStream(resourceStream);

        return input;
    }
}
