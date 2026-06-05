package dev.morkom.keymanager.service;

import com.mifmif.common.regex.Generex;
import dev.morkom.keymanager.dto.XmlGenerateRequest;
import dev.morkom.keymanager.dto.XmlGenerateResponse;
import dev.morkom.keymanager.dto.XmlTransformResponse;
import dev.morkom.keymanager.dto.XmlValidationResponse;
import dev.morkom.keymanager.util.ClasspathResourceResolver;
import jlibs.core.graph.Path;
import jlibs.xml.sax.XMLDocument;
import jlibs.xml.xsd.XSInstance;
import jlibs.xml.xsd.XSParser;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSMultiValueFacet;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTypeDefinition;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
            return doValidation(xmlStream, xsdStream, null);
        } catch (IOException e) {
            return new XmlValidationResponse(false, "Error reading file: " + e.getMessage());
        }
    }

    public XmlValidationResponse validateWithPrepackagedXsd(MultipartFile xmlFile, String xsdPath) {
        if (xmlFile.isEmpty()) {
            return new XmlValidationResponse(false, "XML file is required.");
        }
        
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
            
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResource("classpath:" + fullResourcePath);
            String systemId = resource.getURL().toString();
            
            return doValidation(xmlStream, xsdStream, systemId);
        } catch (IOException e) {
            return new XmlValidationResponse(false, "Error reading file: " + e.getMessage());
        }
    }

    public XmlTransformResponse transform(MultipartFile xmlFile, MultipartFile xsltFile) {
        if (xmlFile.isEmpty() || xsltFile.isEmpty()) {
            return new XmlTransformResponse(null, "Both XML and XSLT files are required.");
        }

        try (InputStream xmlStream = xmlFile.getInputStream();
             InputStream xsltStream = xsltFile.getInputStream()) {

            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            Transformer transformer = factory.newTransformer(new StreamSource(xsltStream));
            StringWriter rawResultWriter = new StringWriter();
            transformer.transform(new StreamSource(xmlStream), new StreamResult(rawResultWriter));

            Transformer prettyPrintTransformer = factory.newTransformer();
            prettyPrintTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
            prettyPrintTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            
            StringWriter prettyResultWriter = new StringWriter();
            prettyPrintTransformer.transform(new StreamSource(new StringReader(rawResultWriter.toString())), new StreamResult(prettyResultWriter));

            return new XmlTransformResponse(prettyResultWriter.toString(), null);

        } catch (Exception e) {
            return new XmlTransformResponse(null, "XSLT transformation failed: " + e.getMessage());
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



    public XmlGenerateResponse generateXmlFromXsd(XmlGenerateRequest request) {
        if (request.xsdFilename() == null || request.xsdFilename().isEmpty()) {
            return new XmlGenerateResponse(null, "XSD filename is required.", null);
        }

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResource("classpath:xsd/" + request.xsdFilename());

            if (!resource.exists()) {
                 return new XmlGenerateResponse(null, "Prepackaged XSD not found: " + request.xsdFilename(), null);
            }

            String systemId = resource.getURL().toString();
            XSModel xsModel = new XSParser().parse(systemId);

            XSNamedMap elements = xsModel.getComponents(XSConstants.ELEMENT_DECLARATION);
            List<String> rootElements = new ArrayList<>();
            for (int i = 0; i < elements.getLength(); i++) {
                XSElementDeclaration element = (XSElementDeclaration) elements.item(i);
                rootElements.add(element.getName());
            }

            if (request.rootElement() == null || request.rootElement().isEmpty()) {
                 return new XmlGenerateResponse(null, null, rootElements);
            }

            XSInstance xsInstance = new XSInstance();
            xsInstance.minimumElementsGenerated = 1;
            xsInstance.maximumElementsGenerated = 1;
            xsInstance.generateOptionalElements = true;
            xsInstance.generateDefaultAttributes = true;
            xsInstance.generateAllChoices = true;
            xsInstance.sampleValueGenerator = new XSInstance.SampleValueGenerator() {

                @Override
                public String generateSampleValue(XSElementDeclaration element, XSSimpleTypeDefinition simpleType, Path path) {
                    return generateRandomFromPattern(simpleType);
                }

                @Override
                public String generateSampleValue(XSAttributeDeclaration attribute, XSSimpleTypeDefinition simpleType, Path path) {
                    return generateRandomFromPattern(simpleType);
                }

                @Override
                public XSTypeDefinition selectSubType(XSElementDeclaration element) {
                    return null; // Let jlibs handle xsi:type
                }

                private String generateRandomFromPattern(XSSimpleTypeDefinition simpleType) {
                    if (simpleType == null) return null;

                    String typeName = simpleType.getName();
                    String baseTypeName = simpleType.getBaseType() != null
                            ? simpleType.getBaseType().getName()
                            : null;

                    String effectiveType = typeName != null ? typeName : baseTypeName;

                    if ("dateTime".equalsIgnoreCase(effectiveType)) {
                        return DateTimeFormatter.ISO_INSTANT.format(Instant.now());
                    }

                    if ("date".equalsIgnoreCase(effectiveType)) {
                        return LocalDate.now().toString();
                    }

                    if ("time".equalsIgnoreCase(effectiveType)) {
                        return LocalTime.now().withNano(0).toString();
                    }

                    String minLenStr = simpleType.getLexicalFacetValue(XSSimpleTypeDefinition.FACET_MINLENGTH);
                    String maxLenStr = simpleType.getLexicalFacetValue(XSSimpleTypeDefinition.FACET_MAXLENGTH);
                    String exactLenStr = simpleType.getLexicalFacetValue(XSSimpleTypeDefinition.FACET_LENGTH);

                    int min = minLenStr != null ? Integer.parseInt(minLenStr) : 1;
                    int max = maxLenStr != null ? Integer.parseInt(maxLenStr) : Integer.MAX_VALUE;
                    Integer exact = exactLenStr != null ? Integer.parseInt(exactLenStr) : null;

                    if (exact != null) {
                        min = exact;
                        max = exact;
                    }

                    // ==========================================================
                    // ENUMERATIONS
                    // ==========================================================
                    XSSimpleTypeDefinition currentType = simpleType;
                    while (currentType != null) {
                        StringList enums = currentType.getLexicalEnumeration();
                        if (enums != null && enums.getLength() > 0) {
                            return enums.item((int) (Math.random() * enums.getLength()));
                        }

                        if (currentType.getMultiValueFacets() != null) {
                            XSObjectList facets = currentType.getMultiValueFacets();
                            for (int i = 0; i < facets.getLength(); i++) {
                                XSMultiValueFacet f = (XSMultiValueFacet) facets.item(i);
                                if (f.getFacetKind() == XSSimpleTypeDefinition.FACET_ENUMERATION) {
                                    StringList vals = f.getLexicalFacetValues();
                                    if (vals != null && vals.getLength() > 0) {
                                        return vals.item((int) (Math.random() * vals.getLength()));
                                    }
                                }
                            }
                        }

                        XSTypeDefinition base = currentType.getBaseType();
                        if (base instanceof XSSimpleTypeDefinition && base != currentType) {
                            currentType = (XSSimpleTypeDefinition) base;
                        } else break;
                    }

                    // ==========================================================
                    // PATTERN HANDLING
                    // ==========================================================
                    if (simpleType.getMultiValueFacets() != null) {
                        XSObjectList facets = simpleType.getMultiValueFacets();

                        for (int i = 0; i < facets.getLength(); i++) {
                            XSMultiValueFacet facet = (XSMultiValueFacet) facets.item(i);

                            if (facet.getFacetKind() == XSSimpleTypeDefinition.FACET_PATTERN) {
                                StringList patterns = facet.getLexicalFacetValues();

                                if (patterns.getLength() > 0) {

                                    // ---- split alternatives ----
                                    List<String> patternList = new ArrayList<>();
                                    for (int j = 0; j < patterns.getLength(); j++) {
                                        String p = patterns.item(j);
                                        if (p.contains("|")) {
                                            patternList.addAll(Arrays.asList(p.split("\\|")));
                                        } else {
                                            patternList.add(p);
                                        }
                                    }

                                    // ---- pick ONE pattern ----
                                    String selectedPattern = patternList.get(
                                            (int) (Math.random() * patternList.size())
                                    ).trim();

                                    // ---- FAST PATH (important for your case) ----
                                    if (selectedPattern.matches("\\\\d\\{\\d+}")) {
                                        int len = Integer.parseInt(
                                                selectedPattern.replaceAll("\\D+", "")
                                        );
                                        return randomDigits(len);
                                    }

                                    if (selectedPattern.matches("\\\\c\\{\\d+}")) {
                                        int len = Integer.parseInt(
                                                selectedPattern.replaceAll("\\D+", "")
                                        );
                                        return randomNameChars(len);
                                    }

                                    try {
                                        String javaRegex = selectedPattern
                                                .replace("\\c", "[A-Za-z0-9._:-]")   // FIXED
                                                .replace("\\i", "[A-Za-z_]")         // FIXED
                                                .replace("\\d", "[0-9]")
                                                .replace("\\D", "[^0-9]")
                                                .replace("\\w", "[A-Za-z0-9_]")
                                                .replace("\\W", "[^A-Za-z0-9_]")
                                                .replace("\\s", "\\\\s")
                                                .replace("\\S", "\\\\S");

                                        javaRegex = "^" + javaRegex + "$";

                                        Generex generex = new Generex(javaRegex);
                                        String randomStr = generex.random();

                                        // ---- apply length ONLY if pattern doesn't define it ----
                                        boolean hasExplicitLength =
                                                selectedPattern.matches(".*\\{\\d+(,\\d+)?}.*");

                                        if (!hasExplicitLength) {
                                            if (exact != null) {
                                                randomStr = adjustLength(randomStr, exact);
                                            } else {
                                                randomStr = adjustLengthRange(randomStr, min, max);
                                            }
                                        }

                                        // ---- sanitize ----
                                        randomStr = sanitizeXml(randomStr);

                                        // ---- FINAL VALIDATION ----
                                        if (!randomStr.matches(javaRegex)) {
                                            return fallbackValue(exact, min);
                                        }

                                        return randomStr;

                                    } catch (Exception e) {
                                        return fallbackValue(exact, min);
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================================
                    // LENGTH ONLY
                    // ==========================================================
                    int target = Math.max(min, 1);
                    return fallbackValue(exact, target);
                }
            };
            StringWriter writer = new StringWriter();
            XMLDocument sampleXml = new XMLDocument(new StreamResult(writer), true, 4, null);
            
            String targetNamespace = null;
            for (int i = 0; i < elements.getLength(); i++) {
                XSElementDeclaration element = (XSElementDeclaration) elements.item(i);
                if (element.getName().equals(request.rootElement())) {
                    targetNamespace = element.getNamespace();
                    break;
                }
            }

            QName rootQName = new QName(targetNamespace, request.rootElement());
            xsInstance.generate(xsModel, rootQName, sampleXml);

            return new XmlGenerateResponse(writer.toString(), null, rootElements);

        } catch (Exception e) {
            return new XmlGenerateResponse(null, "XML generation failed: " + e.getMessage(), null);
        }
    }
    private String randomDigits(int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append((int)(Math.random() * 10));
        }
        return sb.toString();
    }
    private static final char[] NAME_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789._:-".toCharArray();

    private String randomNameChars(int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(NAME_CHARS[(int)(Math.random() * NAME_CHARS.length)]);
        }
        return sb.toString();
    }

    private String adjustLength(String s, int targetLen) {
        if (s == null) return null;

        StringBuilder sb = new StringBuilder();
        int i = 0;

        while (i < s.length() && sb.codePointCount(0, sb.length()) < targetLen) {
            int cp = s.codePointAt(i);
            sb.appendCodePoint(cp);
            i += Character.charCount(cp);
        }

        // pad if too short
        while (sb.codePointCount(0, sb.length()) < targetLen) {
            sb.append('0');
        }

        return sb.toString();
    }


    private String adjustLengthRange(String s, int min, int max) {
        if (s == null) return null;

        int len = s.codePointCount(0, s.length());

        if (len > max) {
            return adjustLength(s, max);
        }

        if (len < min) {
            StringBuilder sb = new StringBuilder(s);
            while (sb.codePointCount(0, sb.length()) < min) {
                sb.append('0');
            }
            return sb.toString();
        }

        return s;
    }

    private String sanitizeXml(String input) {
        if (input == null) return null;

        StringBuilder sb = new StringBuilder(input.length());

        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);

            if (ch == 0x9 || ch == 0xA || ch == 0xD ||
                    (ch >= 0x20 && ch <= 0xD7FF) ||
                    (ch >= 0xE000 && ch <= 0xFFFD)) {

                sb.append(ch);

            } else if (Character.isHighSurrogate(ch)) {
                if (i + 1 < input.length() && Character.isLowSurrogate(input.charAt(i + 1))) {
                    sb.append(ch).append(input.charAt(++i));
                }
                // else skip invalid
            }
            // skip invalid low surrogate
        }

        return sb.toString();
    }


    private String fallbackValue(Integer exact, int min) {
        int len = exact != null ? exact : Math.max(min, 1);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append('1');
        }
        return sb.toString();
    }

    private XmlValidationResponse doValidation(InputStream xmlStream, InputStream xsdStream, String systemId) {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            
            if (systemId != null) {
                // Determine base path from systemId (URL)
                String basePath = systemId;
                int lastSlash = basePath.lastIndexOf('/');
                if (lastSlash != -1) {
                    basePath = basePath.substring(0, lastSlash + 1);
                } else {
                     basePath = "/xsd/"; // fallback
                }
                factory.setResourceResolver(new ClasspathResourceResolver(basePath));
            }

            Schema schema = factory.newSchema(new StreamSource(xsdStream, systemId));
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
