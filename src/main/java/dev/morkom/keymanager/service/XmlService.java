package dev.morkom.keymanager.service;

import dev.morkom.keymanager.dto.XmlValidationResponse;
import dev.morkom.keymanager.util.ClasspathResourceResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class XmlService {

    private static final String XSD_RESOURCE_PATH = "classpath*:xsd/**/*.xsd";

    public XmlValidationResponse validate(MultipartFile xmlFile, MultipartFile xsdFile) {
        if (xmlFile.isEmpty() || xsdFile.isEmpty()) {
            return new XmlValidationResponse(false, "Both XML and XSD files are required.");
        }
        try (InputStream xmlStream = xmlFile.getInputStream();
             InputStream xsdStream = xsdFile.getInputStream()) {
            // For uploaded files, the resolver has no base path, as imports are not supported.
            return doValidation(xmlStream, xsdStream, null);
        } catch (IOException e) {
            return new XmlValidationResponse(false, "Error reading file: " + e.getMessage());
        }
    }

    public XmlValidationResponse validateWithPrepackagedXsd(MultipartFile xmlFile, String xsdPath) {
        if (xmlFile.isEmpty()) {
            return new XmlValidationResponse(false, "XML file is required.");
        }
        
        // Determine the base path for resolving relative imports
        String basePath = "/xsd/";
        int lastSlash = xsdPath.lastIndexOf('/');
        if (lastSlash != -1) {
            basePath += xsdPath.substring(0, lastSlash);
        }

        String fullResourcePath = "/xsd/" + xsdPath;
        try (InputStream xmlStream = xmlFile.getInputStream();
             InputStream xsdStream = getClass().getResourceAsStream(fullResourcePath)) {
            if (xsdStream == null) {
                return new XmlValidationResponse(false, "Prepackaged XSD not found: " + xsdPath);
            }
            return doValidation(xmlStream, xsdStream, basePath);
        } catch (IOException e) {
            return new XmlValidationResponse(false, "Error reading file: " + e.getMessage());
        }
    }

    public List<String> listPrepackagedXsds() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(XSD_RESOURCE_PATH);
        return Arrays.stream(resources)
                .map(resource -> {
                    try {
                        String fullPath = resource.getURL().getPath();
                        int xsdIndex = fullPath.indexOf("/xsd/");
                        if (xsdIndex != -1) {
                            return fullPath.substring(xsdIndex + 5);
                        }
                        return resource.getFilename();
                    } catch (IOException e) {
                        return resource.getFilename();
                    }
                })
                .collect(Collectors.toList());
    }

    private XmlValidationResponse doValidation(InputStream xmlStream, InputStream xsdStream, String basePath) {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            
            // **THE FIX:** Set the custom resource resolver if a base path is provided.
            if (basePath != null) {
                factory.setResourceResolver(new ClasspathResourceResolver(basePath));
            }

            Schema schema = factory.newSchema(new StreamSource(xsdStream));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(xmlStream));
            return new XmlValidationResponse(true, "XML is valid against the provided XSD.");
        } catch (SAXException e) {
            return new XmlValidationResponse(false, "XML validation failed: " + e.getMessage());
        } catch (IOException e) {
            return new XmlValidationResponse(false, "Error reading stream: " + e.getMessage());
        }
    }
}
