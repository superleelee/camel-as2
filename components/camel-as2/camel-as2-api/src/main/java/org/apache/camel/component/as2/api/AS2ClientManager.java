/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.as2.api;

import static org.apache.camel.component.as2.api.AS2Constants.APPLICATION_EDIFACT_MIME_TYPE;

import java.io.IOException;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;

/**
 * AS2 Client Manager 
 * 
 * <p>Sends EDI Messages over HTTP  
 *
 */
public class AS2ClientManager {

    private AS2ClientConnection as2ClientConnection;
    
    public AS2ClientManager(AS2ClientConnection as2ClientConnection) {
        this.as2ClientConnection = as2ClientConnection;
    }

    /**
     * Send <code>ediMessage</code> unencrypted and unsigned to trading partner.
     * 
     * @param ediMessage - EDI message to transport
     * @param subject - the subject sent in the interchange request.
     * @param as2From - the AS2 identifier for the sending trading partner
     * @param as2To - the AS2 identifier for the receiving trading partner
     * @return The AS2 Interchange
     * @throws InvalidAS2NameException 
     * @throws IOException 
     * @throws HttpException 
     */
    public AS2Interchange sendNoEncryptNoSign(String requestUri, String ediMessage, String subject,  String as2From, String as2To) throws InvalidAS2NameException, HttpException, IOException {
        AS2Interchange interchange = new AS2Interchange();
        
        BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("POST", requestUri);
        interchange.setRequest(request);
        
        /* AS2-Version header */
        request.addHeader(AS2Header.AS2_VERSION, "1.1");

        /* Content-Type header (Application/EDIFACT) */   
        request.addHeader(AS2Header.CONTENT_TYPE, APPLICATION_EDIFACT_MIME_TYPE);

        /* AS2-From header */
        Util.validateAS2Name(as2From);
        request.addHeader(AS2Header.AS2_FROM, as2From);

        /* AS2-To header */
        Util.validateAS2Name(as2To);
        request.addHeader(AS2Header.AS2_TO, as2To);

        /* Subject header */
        // SHOULD be set to aid MDN in identifying the original messaged
        request.addHeader(AS2Header.SUBJECT, subject);

        /* Message-Id header*/
        // SHOULD be set to aid in message reconciliation
        request.addHeader(AS2Header.MESSAGE_ID, Util.createMessageId(as2ClientConnection.getClientFqdn()));
        
         // Create Message Body
        /* EDI Message is Message Body */
        HttpEntity entity = new StringEntity(ediMessage, ContentType.create(APPLICATION_EDIFACT_MIME_TYPE, Consts.UTF_8));
        request.setEntity(entity);
        
        HttpResponse response = as2ClientConnection.send(request);
        interchange.setResponse(response);
        
        return interchange;
    }
    
    /**
     * Send <code>ediMessage</code> unencrypted and signed to trading partner.
     * 
     * @param ediMessage - EDI message to transport
     * @param subject - the subject sent in the interchange request.
     * @param as2From - the AS2 identifier for the sending trading partner
     * @param as2To - the AS2 identifier for the receiving trading partner
     * @return The AS2 Interchange
     * @throws InvalidAS2NameException 
     * @throws IOException 
     * @throws HttpException 
     */
    public AS2Interchange sendNoEncryptSigned(String requestUri, String ediMessage, String subject,  String as2From, String as2To) throws InvalidAS2NameException, HttpException, IOException {
        AS2Interchange interchange = new AS2Interchange();
        
        BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("POST", requestUri);
        interchange.setRequest(request);
        
        /* AS2-Version header */
        request.addHeader(AS2Header.AS2_VERSION, "1.1");

        /* Content-Type header (Application/EDIFACT) */   
        request.addHeader(AS2Header.CONTENT_TYPE, APPLICATION_EDIFACT_MIME_TYPE);

        /* AS2-From header */
        Util.validateAS2Name(as2From);
        request.addHeader(AS2Header.AS2_FROM, as2From);

        /* AS2-To header */
        Util.validateAS2Name(as2To);
        request.addHeader(AS2Header.AS2_TO, as2To);

        /* Subject header */
        // SHOULD be set to aid MDN in identifying the original messaged
        request.addHeader(AS2Header.SUBJECT, subject);

        /* Message-Id header*/
        // SHOULD be set to aid in message reconciliation
        request.addHeader(AS2Header.MESSAGE_ID, Util.createMessageId(as2ClientConnection.getClientFqdn()));
        
         // Create Message Body
        /* EDI Message is Message Body */
        HttpEntity entity = new StringEntity(ediMessage, ContentType.create(APPLICATION_EDIFACT_MIME_TYPE, Consts.UTF_8));
        request.setEntity(entity);
        
        HttpResponse response = as2ClientConnection.send(request);
        interchange.setResponse(response);
        
        return interchange;
    }
    
}
