package org.apache.camel.component.as2.api.entity;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.apache.camel.component.as2.api.AS2Header;
import org.apache.camel.component.as2.api.AS2MediaType;
import org.apache.camel.component.as2.api.AS2MimeType;
import org.apache.camel.component.as2.api.AS2SignedDataGenerator;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.io.AbstractMessageParser;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.ParserCursor;
import org.apache.http.util.Args;
import org.apache.http.util.CharArrayBuffer;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;

public class MultipartSignedEntity extends MultipartMimeEntity {

    public MultipartSignedEntity(MimeEntity data, AS2SignedDataGenerator signer, String signatureCharSet, String signatureTransferEncoding, boolean isMainBody, String boundary) throws Exception {
        super(null, isMainBody, boundary);
        ContentType contentType = signer.createMultipartSignedContentType(this.boundary);
        this.contentType = new BasicHeader(AS2Header.CONTENT_TYPE, contentType.toString());
        addPart(data);
        ApplicationPkcs7SignatureEntity signature = new ApplicationPkcs7SignatureEntity(data, signer, signatureCharSet, signatureTransferEncoding, false);
        addPart(signature);
    }
    
    protected MultipartSignedEntity(String boundary, boolean isMainBody) {
        this.boundary = boundary;
        this.isMainBody = isMainBody;
    }
    
    public boolean isValid() throws CMSException, Exception {
        ApplicationEDIEntity applicationEDIEntity = getSignedDataEntity();
        ApplicationPkcs7SignatureEntity applicationPkcs7SignatureEntity = getSignatureEntity();
        
        if (applicationEDIEntity == null || applicationPkcs7SignatureEntity == null) {
            return false;
        }
        
        String ediMessage = applicationEDIEntity.getEdiMessage();
        ContentType ediMessageContentType = ContentType.parse(applicationEDIEntity.getContentTypeValue());
        CMSProcessable signedContent = new CMSProcessableByteArray(ediMessage.getBytes(ediMessageContentType.getCharset()));
        
        byte[] signature = applicationPkcs7SignatureEntity.getSignature();
        
        
        CMSSignedData signedData = new CMSSignedData(EntityUtils.decode(signature, applicationPkcs7SignatureEntity.getContentTransferEncodingValue()));
        
        return true;
    }
    
    public ApplicationEDIEntity getSignedDataEntity() {
        if (getPartCount() > 0 && getPart(0) instanceof ApplicationEDIEntity) {
            return (ApplicationEDIEntity)  getPart(0);
        }
        
        return null;
    }
    
    public ApplicationPkcs7SignatureEntity getSignatureEntity() {
        if (getPartCount() > 1 && getPart(0) instanceof ApplicationPkcs7SignatureEntity) {
            return (ApplicationPkcs7SignatureEntity)  getPart(1);
        }
        
        return null;
    }
    
    public static HttpEntity parseMultipartSignedEntity(HttpEntity entity, boolean isMainBody) throws Exception{
        Args.notNull(entity, "Entity");
        Args.check(entity.isStreaming(), "Entity is not streaming");
        MultipartSignedEntity multipartSignedEntity = null;
        Header[] headers = null;

        try {
            // Determine and validate the Content Type
            Header contentTypeHeader = entity.getContentType();
            if (contentTypeHeader == null) {
                throw new HttpException("Content-Type header is missing");
            }
            ContentType multipartSignedContentType =  ContentType.parse(entity.getContentType().getValue());
            if (!multipartSignedContentType.getMimeType().equals(AS2MimeType.MULTIPART_SIGNED)) {
                throw new HttpException("Entity has invalid MIME type '" + multipartSignedContentType.getMimeType() + "'");
            }

            SessionInputBufferImpl inBuffer = new SessionInputBufferImpl(new HttpTransportMetricsImpl(), 8 * 1024);
            inBuffer.bind(entity.getContent());
            
            // Parse Headers
            if (!isMainBody) {
               headers = AbstractMessageParser.parseHeaders(
                        inBuffer,
                        -1,
                        -1,
                        BasicLineParser.INSTANCE,
                        new ArrayList<CharArrayBuffer>());
            }
            
            // Get Boundary Value
            String boundary = multipartSignedContentType.getParameter("boundary");
            if (boundary == null) {
                throw new HttpException("Failed to retrive boundary value");
            }
            
            multipartSignedEntity = new MultipartSignedEntity(boundary, true);
            
            if (headers != null) {
                multipartSignedEntity.setHeaders(headers);
            }

            //
            // Parse EDI Message Body Part
            //
            
            // Skip Preamble and Start Boundary line
            skipPreambleAndStartBoundary(inBuffer, boundary);
            
            // Read EDI Message Body Part Headers
            headers = AbstractMessageParser.parseHeaders(
                    inBuffer,
                    -1,
                    -1,
                    BasicLineParser.INSTANCE,
                    new ArrayList<CharArrayBuffer>());
            
            // Get Content-Type and Content-Transfer-Encoding
            ContentType ediMessageContentType = null;
            String ediMessageContentTransferEncoding = null;
            for (Header header : headers) {
                switch (header.getName()) {
                case AS2Header.CONTENT_TYPE:
                    ediMessageContentType = ContentType.parse(header.getValue());
                    break;
                case AS2Header.CONTENT_TRANSFER_ENCODING:
                    ediMessageContentTransferEncoding = header.getValue();
                    break;
                }
            }
            if (ediMessageContentType == null) {
                throw new HttpException("Failed to find Content-Type header in EDI message body part");
            }
            if (!isEDIMessageContentType(ediMessageContentType)) {
                throw new HttpException("Invalid content type '" + ediMessageContentType.getMimeType() + "' for EDI message body part");
            }
            
           
            // - Read EDI Message Body Part Content
            CharArrayBuffer ediMessageContentBuffer = new CharArrayBuffer(1024);
            CharArrayBuffer lineBuffer = new CharArrayBuffer(1024);
            boolean foundMultipartEndBoundary = false;
            while(inBuffer.readLine(lineBuffer) != -1) {
                if (isBoundaryDelimiter(lineBuffer, null, boundary)) {
                    foundMultipartEndBoundary = true;
                    lineBuffer.clear();
                    break;
                }
                lineBuffer.append("\r\n"); // add line delimiter
                ediMessageContentBuffer.append(lineBuffer);
                lineBuffer.clear();
            }
            if (!foundMultipartEndBoundary) {
                throw new HttpException("Failed to find end boundary delimiter for EDI message body part");
            }
            
            // Decode Content
            Charset ediMessageCharset = ediMessageContentType.getCharset();
            if (ediMessageCharset == null) {
                ediMessageCharset = StandardCharsets.US_ASCII;
            }
            byte[] bytes = EntityUtils.decode(ediMessageContentBuffer.toString().getBytes(ediMessageCharset), ediMessageContentTransferEncoding);
            String ediMessageContent = new String(bytes, ediMessageCharset);
            
            // Build application EDI entity and add to multipart.
            ApplicationEDIEntity applicationEDIEntity = EntityUtils.createEDIEntity(ediMessageContent, ediMessageContentType, ediMessageContentTransferEncoding, false);
            applicationEDIEntity.removeAllHeaders();
            applicationEDIEntity.setHeaders(headers);
            multipartSignedEntity.addPart(applicationEDIEntity);
            
            //
            // End EDI Message Body Parts
            
            //
            // Parse Signature Body Part
            //
            
            // Read Signature Body Part Headers
            headers = AbstractMessageParser.parseHeaders(
                    inBuffer,
                    -1,
                    -1,
                    BasicLineParser.INSTANCE,
                    new ArrayList<CharArrayBuffer>());
            
            // Get Content-Type and Content-Transfer-Encoding
            ContentType signatureContentType = null;
            String signatureContentTransferEncoding = null;
            for (Header header : headers) {
                switch (header.getName()) {
                case AS2Header.CONTENT_TYPE:
                    signatureContentType = ContentType.parse(header.getValue());
                    break;
                case AS2Header.CONTENT_TRANSFER_ENCODING:
                    signatureContentTransferEncoding = header.getValue();
                    break;
                }
            }
            if (signatureContentType == null) {
                throw new HttpException("Failed to find Content-Type header in signature body part");
            }
            if (!isPkcs7SignatureType(signatureContentType)) {
                throw new HttpException("Invalid content type '" + ediMessageContentType.getMimeType() + "' for signature body part");
            }
            
            // - Read Signature Body Part Content
            CharArrayBuffer signatureContentBuffer = new CharArrayBuffer(1024);
            lineBuffer = new CharArrayBuffer(1024);
            foundMultipartEndBoundary = false;
            while(inBuffer.readLine(lineBuffer) != -1) {
                if (isBoundaryDelimiter(lineBuffer, null, boundary)) {
                    foundMultipartEndBoundary = true;
                    lineBuffer.clear();
                    break;
                }
                signatureContentBuffer.append(lineBuffer);
                signatureContentBuffer.append("\r\n"); // add line delimiter
                lineBuffer.clear();
            }
            if (!foundMultipartEndBoundary) {
                throw new HttpException("Failed to find end boundary delimiter for signature body part");
            }

            // Decode Content
            Charset signatureCharset = signatureContentType.getCharset();
            if (signatureCharset == null) {
                signatureCharset = StandardCharsets.US_ASCII;
            }
            byte[] signature = EntityUtils.decode(signatureContentBuffer.toString().getBytes(signatureCharset), signatureContentTransferEncoding);

            // Build application Pkcs7 Signature entity and add to multipart.
            ApplicationPkcs7SignatureEntity applicationPkcs7SignatureEntity = new ApplicationPkcs7SignatureEntity(signatureCharset.toString(), signatureContentTransferEncoding, signature, false);
            multipartSignedEntity.addPart(applicationPkcs7SignatureEntity);

            //
            // End Signature Body Parts
            
            return multipartSignedEntity;
        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            throw new HttpException("Failed to parse entity content", e);
        }
    }
    
    public static boolean isPkcs7SignatureType(ContentType pcks7SignatureType) {
        switch (pcks7SignatureType.getMimeType().toLowerCase()) {
        case AS2MimeType.APPLICATION_PKCS7_SIGNATURE:
            return true;
        default:
            return false;
        }
    }
    
    public static boolean isEDIMessageContentType(ContentType ediMessageContentType) {
        switch(ediMessageContentType.getMimeType().toLowerCase()) {
        case AS2MediaType.APPLICATION_EDIFACT:
            return true;
        case AS2MediaType.APPLICATION_EDI_X12:
            return true;
        case AS2MediaType.APPLICATION_EDI_CONSENT:
            return true;
        default:
            return false;
        }
    }
    
    public static void skipPreambleAndStartBoundary(SessionInputBufferImpl inBuffer, String boundary) throws HttpException {

        boolean foundStartBoundary;
        try {
            foundStartBoundary = false;
            CharArrayBuffer lineBuffer = new CharArrayBuffer(1024);
            while(inBuffer.readLine(lineBuffer) != -1) {
                final ParserCursor cursor = new ParserCursor(0, lineBuffer.length());
                if (isBoundaryDelimiter(lineBuffer, cursor, boundary)) {
                    foundStartBoundary = true;
                    break;
                }
                lineBuffer.clear();
            }
        } catch (Exception e) {
            throw new HttpException("Failed to read start boundary for body part", e);
        }
        
        if (!foundStartBoundary) {
            throw new HttpException("Failed to find start boundary for body part");
        }
        
    }
    
    public static boolean isBoundaryDelimiter(final CharArrayBuffer buffer, ParserCursor cursor, String boundary) {
        Args.notNull(buffer, "Buffer");
        Args.notNull(boundary, "Boundary");

        String boundaryDelimiter = "--" + boundary; // boundary delimiter - RFC2046 5.1.1

        if (cursor == null) {
            cursor = new ParserCursor(0, boundaryDelimiter.length());
        }
        
        int indexFrom = cursor.getPos();
        int indexTo = cursor.getUpperBound();
        
        if ((indexFrom + boundaryDelimiter.length()) > indexTo) {
            return false; 
        }
        
        for (int i = indexFrom; i < indexTo; ++i) {
            if (buffer.charAt(i) != boundaryDelimiter.charAt(i)) {
                return false;
            }
        }
        
        return true;
    }

    public static boolean isBoundaryCloseDelimiter(final CharArrayBuffer buffer, ParserCursor cursor, String boundary) {
        Args.notNull(buffer, "Buffer");
        Args.notNull(boundary, "Boundary");

        String boundaryCloseDelimiter = "--" + boundary + "--"; // boundary close-delimiter - RFC2046 5.1.1
        
        if (cursor == null) {
            cursor = new ParserCursor(0, boundaryCloseDelimiter.length());
        }

        int indexFrom = cursor.getPos();
        int indexTo = cursor.getUpperBound();
        
        if ((indexFrom + boundaryCloseDelimiter.length()) > indexTo) {
            return false; 
        }
        
        for (int i = indexFrom; i < indexTo; ++i) {
            if (buffer.charAt(i) != boundaryCloseDelimiter.charAt(i)) {
                return false;
            }
        }
        
        return true;
    }

}
