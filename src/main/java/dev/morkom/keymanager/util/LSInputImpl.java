package dev.morkom.keymanager.util;

import org.w3c.dom.ls.LSInput;
import java.io.InputStream;
import java.io.Reader;

public class LSInputImpl implements LSInput {

    private String publicId;
    private String systemId;
    private String baseURI;
    private InputStream byteStream;
    private Reader charStream;
    private String stringData;
    private String encoding;
    private boolean certifiedText;

    @Override
    public String getPublicId() { return publicId; }
    @Override
    public void setPublicId(String publicId) { this.publicId = publicId; }
    @Override
    public String getSystemId() { return systemId; }
    @Override
    public void setSystemId(String systemId) { this.systemId = systemId; }
    @Override
    public String getBaseURI() { return baseURI; }
    @Override
    public void setBaseURI(String baseURI) { this.baseURI = baseURI; }
    @Override
    public InputStream getByteStream() { return byteStream; }
    @Override
    public void setByteStream(InputStream byteStream) { this.byteStream = byteStream; }
    @Override
    public Reader getCharacterStream() { return charStream; }
    @Override
    public void setCharacterStream(Reader characterStream) { this.charStream = characterStream; }
    @Override
    public String getStringData() { return stringData; }
    @Override
    public void setStringData(String stringData) { this.stringData = stringData; }
    @Override
    public String getEncoding() { return encoding; }
    @Override
    public void setEncoding(String encoding) { this.encoding = encoding; }
    @Override
    public boolean getCertifiedText() { return certifiedText; }
    @Override
    public void setCertifiedText(boolean certifiedText) { this.certifiedText = certifiedText; }
}
