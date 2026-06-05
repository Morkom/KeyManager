package dev.morkom.keymanager.service;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

@Service
public class XmlSigningService {

    public String signXml(InputStream xmlInputStream, InputStream keystoreInputStream, String storeType, String alias, String storePassword, String keyPassword) throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        Reference ref = fac.newReference("", fac.newDigestMethod(DigestMethod.SHA256, null),
                Collections.singletonList(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
                null, null);

        KeyStore ks = KeyStore.getInstance(storeType);
        ks.load(keystoreInputStream, storePassword.toCharArray());
        KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry) ks.getEntry(alias, new KeyStore.PasswordProtection(keyPassword.toCharArray()));
        X509Certificate cert = (X509Certificate) keyEntry.getCertificate();
        PrivateKey privateKey = keyEntry.getPrivateKey();

        String keyAlgorithm = privateKey.getAlgorithm();
        String signatureMethodUri;
        if ("RSA".equalsIgnoreCase(keyAlgorithm)) {
            signatureMethodUri = SignatureMethod.RSA_SHA256;
        } else if ("EC".equalsIgnoreCase(keyAlgorithm)) {
            signatureMethodUri = "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256";
        } else {
            throw new IllegalArgumentException("Unsupported key algorithm: " + keyAlgorithm);
        }

        SignedInfo si = fac.newSignedInfo(
                fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                fac.newSignatureMethod(signatureMethodUri, null),
                Collections.singletonList(ref)
        );

        KeyInfoFactory kif = fac.getKeyInfoFactory();
        X509Data xd = kif.newX509Data(Collections.singletonList(cert));
        KeyInfo ki = kif.newKeyInfo(Collections.singletonList(xd));

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(xmlInputStream);

        DOMSignContext dsc = new DOMSignContext(privateKey, doc.getDocumentElement());
        
        Provider bcProvider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        dsc.setProperty("org.jcp.xml.dsig.internal.dom.SignatureProvider", bcProvider);


        XMLSignature signature = fac.newXMLSignature(si, ki);
        signature.sign(dsc);

        StringWriter sw = new StringWriter();
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer trans = tf.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.transform(new DOMSource(doc), new StreamResult(sw));

        return sw.toString();
    }

    public List<String> getAliases(InputStream keystoreInputStream, String storeType, String storePassword) throws Exception {
        KeyStore ks = KeyStore.getInstance(storeType);
        ks.load(keystoreInputStream, storePassword.toCharArray());
        return Collections.list(ks.aliases());
    }
}