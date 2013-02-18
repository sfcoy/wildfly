/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.ejb.http.extension;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * @author sfcoy
 */
class ConnectorResourceDefinition extends SimpleResourceDefinition {

    static final SimpleAttributeDefinition VIRTUAL_HOST_ATTR = createAttributeWithDefault(ConnectorModel.VIRTUAL_HOST_ATTR,
            ConnectorAttribute.VIRTUAL_HOST.getLocalName(), ConnectorModel.DEFAULT_VIRTUAL_HOST);

    static final SimpleAttributeDefinition CONTEXT_ATTR = createAttribute(ConnectorModel.CONTEXT_ATTR,
            ConnectorAttribute.CONTEXT.getLocalName());

    static final SimpleAttributeDefinition SECURITY_DOMAIN_ATTR = createNullableAttribute(ConnectorModel
            .SECURITY_DOMAIN_ATTR, ConnectorAttribute.SECURITY_DOMAIN.getLocalName());

    private static final PathElement CONNECTOR_PATH = PathElement.pathElement(ConnectorModel.NAME);

    static final ConnectorResourceDefinition INSTANCE = new ConnectorResourceDefinition();

    private static final AttributeDefinition[] attributes = {VIRTUAL_HOST_ATTR, CONTEXT_ATTR, SECURITY_DOMAIN_ATTR};

    private ConnectorResourceDefinition() {
        super(CONNECTOR_PATH,
                ResourceDescriptionResolver.build(ConnectorModel.NAME),
                EjbOverHttpServletDeployerServiceAddStepHandler.INSTANCE,
                EjbOverHttpServletDeployerServiceRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.setRuntimeOnly(false);
        super.registerOperations(resourceRegistration);
        //you can register aditional operations here
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        ReloadRequiredWriteAttributeHandler reloadRequiredWriteAttributeHandler = new ReloadRequiredWriteAttributeHandler(this.attributes);
        for (AttributeDefinition attr : this.attributes) {
            resourceRegistration.registerReadWriteAttribute(attr, null, reloadRequiredWriteAttributeHandler);
            // resist the temptation to replace the null above with a ReadAttributeHandler. Strange and ugly things
            // happen.
        }
    }

    static void parseAndSetConnectorDefinition(ConnectorSpecification connectorSpecification, ModelNode addOperation,
                                               XMLExtendedStreamReader reader) throws XMLStreamException {
        VIRTUAL_HOST_ATTR.parseAndSetParameter(connectorSpecification.getVirtualHost(), addOperation, reader);
        CONTEXT_ATTR.parseAndSetParameter(connectorSpecification.getContext(), addOperation, reader);
        if (connectorSpecification.getSecurityDomain() != null)
            SECURITY_DOMAIN_ATTR.parseAndSetParameter(connectorSpecification.getSecurityDomain(), addOperation, reader);
    }

    private static SimpleAttributeDefinition createAttributeWithDefault(String name, String xmlLocalName,
                                                                  ModelNode defaultValue) {
        return SimpleAttributeDefinitionBuilder.create(name, ModelType.STRING)
                .setXmlName(xmlLocalName)
                .setAllowExpression(true)
                .setDefaultValue(defaultValue)
                .build();
    }

    private static SimpleAttributeDefinition createAttribute(String name, String xmlLocalName) {
        return SimpleAttributeDefinitionBuilder.create(name, ModelType.STRING)
                .setXmlName(xmlLocalName)
                .setAllowExpression(true)
                .build();
    }

    private static SimpleAttributeDefinition createNullableAttribute(String name, String xmlLocalName) {
        return SimpleAttributeDefinitionBuilder.create(name, ModelType.STRING)
                .setXmlName(xmlLocalName)
                .setAllowExpression(true)
                .setAllowNull(true)
                .build();
    }

}
