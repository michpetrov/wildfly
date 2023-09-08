/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat Middleware LLC, and individual contributors
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
 *
 */
package org.jboss.as.txn.subsystem;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamException;

import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;

/**
 * The {@link org.jboss.staxmapper.XMLElementReader} that handles the version 6.1 of Transaction subsystem xml.
 */
class TransactionSubsystem61Parser extends TransactionSubsystem60Parser {

    TransactionSubsystem61Parser() {
        super(Namespace.TRANSACTIONS_6_1);
    }

    TransactionSubsystem61Parser(Namespace namespace) {
        super(namespace);
    }

    @Override
    protected void parseJdbcStoreElementAndEnrichOperation(final XMLExtendedStreamReader reader, final ModelNode logStoreOperation, ModelNode operation) throws XMLStreamException {
        logStoreOperation.get(LogStoreConstants.LOG_STORE_TYPE.getName()).set("jdbc");

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DATASOURCE_NAME:
                    TransactionSubsystemRootResourceDefinition.JDBC_STORE_DATASOURCE_NAME.parseAndSetParameter(value, operation, reader);
                    break;
                case DATASOURCE_JNDI_NAME:
                    TransactionSubsystemRootResourceDefinition.JDBC_STORE_DATASOURCE.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {

            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case JDBC_ACTION_STORE: {
                    parseJdbcStoreConfigElementAndEnrichOperation(reader, operation, TransactionSubsystemRootResourceDefinition.JDBC_ACTION_STORE_TABLE_PREFIX, TransactionSubsystemRootResourceDefinition.JDBC_ACTION_STORE_DROP_TABLE);
                    break;
                }
                case JDBC_STATE_STORE: {
                    parseJdbcStoreConfigElementAndEnrichOperation(reader, operation, TransactionSubsystemRootResourceDefinition.JDBC_STATE_STORE_TABLE_PREFIX, TransactionSubsystemRootResourceDefinition.JDBC_STATE_STORE_DROP_TABLE);
                    break;
                }
                case JDBC_COMMUNICATION_STORE: {
                    parseJdbcStoreConfigElementAndEnrichOperation(reader, operation, TransactionSubsystemRootResourceDefinition.JDBC_COMMUNICATION_STORE_TABLE_PREFIX, TransactionSubsystemRootResourceDefinition.JDBC_COMMUNICATION_STORE_DROP_TABLE);
                    break;
                }
            }
        }


    }
}
