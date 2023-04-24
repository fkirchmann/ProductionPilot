/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.opc.milo;

import com.productionpilot.opc.*;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.DataTypeTreeBuilder;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.sdk.core.DataTypeTree;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.eclipse.milo.opcua.stack.core.util.EndpointUtil;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

@Slf4j
public class MiloOpcConnection implements OpcConnection {

    private final int CONNECT_RETY_INTERVAL = 5000;

    private final int timeout;
    private final String opcServerUrl, opcServerHostnameOverride, opcUser, opcPassword;

    protected OpcUaClient client;
    private DataTypeTree dataTypeTree;


    private final MiloOpcSubscriptionManager subscriptionManager = new MiloOpcSubscriptionManager(this);

    @SneakyThrows
    public MiloOpcConnection(String opcServerUrl, String opcServerHostnameOverride, String opcUser, String opcPassword,
                             int timeout) {
        this.opcServerUrl = opcServerUrl;
        this.opcServerHostnameOverride = opcServerHostnameOverride;
        this.opcUser = opcUser;
        this.opcPassword = opcPassword;
        this.timeout = timeout;

        var connectionThread = new Thread(this::connectionThreadRun);
        connectionThread.setDaemon(true);
        connectionThread.setName("MiloOpcConnection");
        connectionThread.start();
    }

    @SneakyThrows(InterruptedException.class)
    private void connectionThreadRun() {
        Exception lastConnectionException = null;
        while(client == null) {
            try {
                var client = OpcUaClient.create(
                        opcServerUrl,
                        endpoints ->
                                endpoints.stream()
                                        .filter(e -> {
                                            //log.info("Discovered endpoint: {}", e.toString());
                                            return e.getSecurityPolicyUri()
                                                    .equals(SecurityPolicy.None.getUri());
                                        })
                                        .findFirst()
                                        .map(url -> opcServerHostnameOverride == null ? url
                                                : EndpointUtil.updateUrl(url, opcServerHostnameOverride)),
                        configBuilder -> configBuilder
                                .setIdentityProvider(new UsernameProvider(opcUser, opcPassword))
                                .setMaxResponseMessageSize(uint(128 * 1000 * 1000)) // 128 MB
                                .build()
                );
                client = (OpcUaClient) client.connect().get();
                this.client = client;
                dataTypeTree = DataTypeTreeBuilder.build(client);
                log.info("Connected to OPC server");
                lastConnectionException = null;
                subscriptionManager.setClient(client);
            } catch (InterruptedException | ExecutionException | UaException e) {
                // Don't spam the log with the same error message when we're trying to connect
                if(lastConnectionException == null || !e.getMessage().equals(lastConnectionException.getMessage())) {
                    log.warn("Error connecting to OPC server, will retry every {} ms", CONNECT_RETY_INTERVAL, e);
                    lastConnectionException = e;
                }
            }
            Thread.sleep(CONNECT_RETY_INTERVAL);
        }
    }

    boolean isConnected() {
        return client != null;
    }

    private void checkConnected() throws OpcException {
        if(!isConnected()) {
            throw new OpcException("Not connected to OPC UA server");
        }
    }

    @Override
    public List<List<OpcNode>> browse(@NonNull List<OpcNode> parents) throws OpcException {
        checkConnected();
        List<NodeId> browseRoots = parents.stream().map(node -> {
            if(node == null) {
                return Identifiers.ObjectsFolder;
            } else if (node instanceof MiloOpcNode miloOpcNode) {
                return MiloOpcNodeId.from(miloOpcNode.getId()).getMiloNodeId();
            } else {
                throw new UnsupportedOperationException("Cannot browse from node " + node.getPath()
                        + " because it is not an OpcNodeImpl");
            }
        }).toList();

        var browse = browseRoots.stream().map(browseRoot -> new BrowseDescription(
                browseRoot,
                BrowseDirection.Forward,
                Identifiers.References,
                true,
                uint(NodeClass.Object.getValue() | NodeClass.Variable.getValue()),
                uint(BrowseResultMask.All.getValue())
        )).toList();
        // Perform the browse and get the one reference description for each child node
        List<List<ReferenceDescription>> references;
        try {
            references = client.browse(browse).get().stream()
                .map(browseResult ->  {
                    // For folders containing lots of nodes, browses may be paginated. To get the next page's contents,
                    // continuation points are used. This loop will get all pages of results.
                    // See https://github.com/eclipse/milo/issues/227#issuecomment-366752809
                    // Note: in case of multiple continuation points, this could be further optimized by using the other
                    // browseNext() method that takes a list of continuation points. However, this is not done for
                    // simplicity, and because this is not expected to be a common case.
                    var subReferences = new ArrayList<>(Arrays.asList(browseResult.getReferences()));
                    try {
                        var continuationPoint = browseResult.getContinuationPoint();
                        while (continuationPoint != null && !continuationPoint.isNull()) {
                            BrowseResult nextPage = client.browseNext(false, continuationPoint).get();
                            subReferences.addAll(Arrays.asList(nextPage.getReferences()));
                            continuationPoint = nextPage.getContinuationPoint();
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        throw new OpcException(e);
                    }
                    return (List<ReferenceDescription>) subReferences;
                }).toList();
        } catch (InterruptedException | ExecutionException e) {
            throw new OpcException(e);
        }
        if(references.size() != browseRoots.size()) {
            throw new OpcException("Browse result size does not match browse request size");
        }
        // Set up a table mapping the ReferenceDescription of each child node to that child node's type
        // For those child nodes that are objects, set the type accordingly
        // For all other nodes (i.e., variables), we will have to get the type in another request
        Map<ReferenceDescription, OpcNodeType> types = references.stream().flatMap(List::stream)
                .filter(rd -> rd.getNodeClass() == NodeClass.Object)
                .collect(Collectors.toMap(rd -> rd, rd -> OpcNodeType.OBJECT));

        // For all other nodes (i.e., variables), we have to get the type in another request
        List<ReferenceDescription> remainingReferences = references.stream().flatMap(List::stream)
                .filter(ref -> !types.containsKey(ref)).toList();
        if(!remainingReferences.isEmpty()) {
            List<NodeId> remainingNodeIds = remainingReferences.stream().map(rd ->
                    rd.getNodeId().toNodeId(client.getNamespaceTable()).get()).toList();
            var variableTypes = getNodeTypes(remainingNodeIds);
            for (int i = 0; i < variableTypes.size(); i++) {
                var referenceDescription = remainingReferences.get(i);
                types.put(referenceDescription, variableTypes.get(i));
            }
        }
        // Now that we have all types, return the list of child OpcNodes
        var referencesFinal = references;
        return IntStream.range(0, browseRoots.size())
            .mapToObj(i ->
                    referencesFinal.get(i).stream().map(reference -> {
                        var parent = parents.get(i);
                        var type = types.get(reference);
                        if (type == null) {
                            throw new IllegalStateException("Type is null for reference " + reference);
                        }
                        return MiloOpcNode.fromReferenceDescription(this, parent, reference, type);
                    })
                .toList()
            ).toList();
    }

    @Override
    public OpcNodeId parseNodeId(String nodeId) throws OpcException {
        return MiloOpcNodeId.from(nodeId);
    }

    @Override
    @SneakyThrows
    public List<OpcNode> getNodesFromNodeIds(List<OpcNodeId> nodeIds) {
        checkConnected();
        var miloNodeIds = nodeIds.stream()
                .map(nodeId -> MiloOpcNodeId.from(nodeId).getMiloNodeId()).toList();
        var nodeTypes = getNodeTypes(miloNodeIds);
        List<OpcNode> nodes = new ArrayList<>(nodeIds.size());
        for (int i = 0; i < nodeIds.size(); i++) {
            if(nodeTypes.get(i) == null) {
                nodes.add(null);
            } else {
                nodes.add(new MiloOpcNode(this, nodeIds.get(i), null, null, nodeTypes.get(i)));
            }
        }
        return nodes;
    }

    @Override
    public OpcSubscriptionManager getSubscriptionManager() {
        return subscriptionManager;
    }

    @Override
    public OpcMeasuredValue read(OpcNode node) throws OpcException {
        checkConnected();
        var nodeId = MiloOpcNodeId.from(node.getId()).getMiloNodeId();
        try {
            var value = client.readValue(0, TimestampsToReturn.Both, nodeId).get();
            return MiloOpcTypeMapper.mapMeasuredValue(node, value, Instant.now());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new OpcException(e.getCause());
        }
    }

    private List<OpcNodeType> getNodeTypes(List<NodeId> nodeIds) throws OpcException {
        checkConnected();
        if(nodeIds.isEmpty()) { return List.of(); }
        var readValues = nodeIds.stream()
                .flatMap(nodeId -> Stream.of(
                        new ReadValueId(
                                nodeId,
                                AttributeId.NodeClass.uid(),
                                null,
                                QualifiedName.NULL_VALUE),
                        new ReadValueId(
                                nodeId,
                                AttributeId.DataType.uid(),
                                null,
                                QualifiedName.NULL_VALUE)
                )).toList();
        DataValue[] dataValues;
        try {
            dataValues = client.read(0.0, TimestampsToReturn.Neither, readValues).get().getResults();
        } catch (InterruptedException | ExecutionException e) {
            throw new OpcException(e);
        }
        var dataValuesFinal = dataValues;
        return IntStream.range(0, dataValues.length / 2)
                .map(i -> i * 2)
                .mapToObj(i -> {
                    var nodeId = nodeIds.get(i / 2);
                    var nodeClassValue = dataValuesFinal[i].getValue();
                    if(nodeClassValue.isNull()) {
                        log.warn("Got null node class for node {}", nodeId);
                        return OpcNodeType.NOT_FOUND;
                    }
                    var nodeClass = NodeClass.from((Integer) dataValuesFinal[i].getValue().getValue());
                    if(nodeClass == NodeClass.Variable) {
                        var typeNodeId = (NodeId) dataValuesFinal[i + 1].getValue().getValue();
                        return MiloOpcTypeMapper.mapVariableType(dataTypeTree.getBackingClass(typeNodeId));
                    } else {
                        return OpcNodeType.OBJECT;
                    }
                }).toList();
    }

    @Nullable
    protected OpcUaClient getClient() {
        return client;
    }
}
