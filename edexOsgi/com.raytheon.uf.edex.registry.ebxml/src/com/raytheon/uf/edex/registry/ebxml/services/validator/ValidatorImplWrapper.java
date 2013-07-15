/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.edex.registry.ebxml.services.validator;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;

import oasis.names.tc.ebxml.regrep.wsdl.registry.services.v4.MsgRegistryException;
import oasis.names.tc.ebxml.regrep.wsdl.registry.services.v4.Validator;
import oasis.names.tc.ebxml.regrep.xsd.spi.v4.ValidateObjectsRequest;
import oasis.names.tc.ebxml.regrep.xsd.spi.v4.ValidateObjectsResponse;

import org.springframework.transaction.annotation.Transactional;

/**
 * 
 * Wrapper for the validator service to be used with the SOAP interface.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 7/11/2013    1707        bphillip    Initial implementation
 * </pre>
 * 
 * @author bphillip
 * @version 1
 */
@Transactional
public class ValidatorImplWrapper implements Validator {

    private ValidatorImpl validator;

    public ValidatorImplWrapper() {

    }

    public ValidatorImplWrapper(ValidatorImpl validator) {
        this.validator = validator;
    }

    @Override
    @WebMethod(action = "urn:oasis:names:tc:ebxml-regrep:wsdl:spi:bindings:4.0:Validator#validateObjects")
    @WebResult(name = "ValidateObjectsResponse", targetNamespace = "urn:oasis:names:tc:ebxml-regrep:xsd:spi:4.0", partName = "partValidateObjectsResponse")
    public ValidateObjectsResponse validateObjects(
            @WebParam(name = "ValidateObjectsRequest", targetNamespace = "urn:oasis:names:tc:ebxml-regrep:xsd:spi:4.0", partName = "partValidateObjectsRequest") ValidateObjectsRequest partValidateObjectsRequest)
            throws MsgRegistryException {
        return validator.validateObjects(partValidateObjectsRequest);
    }

}