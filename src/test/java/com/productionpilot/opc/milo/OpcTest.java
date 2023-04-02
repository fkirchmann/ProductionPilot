package com.productionpilot.opc.milo;

import com.productionpilot.opc.kepserver.KepOpcConnection;
import com.productionpilot.service.OpcService;
import com.productionpilot.Application;
import com.productionpilot.util.DebugPerfTimer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.DataTypeTreeBuilder;
import org.eclipse.milo.opcua.sdk.core.DataTypeTree;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@Slf4j
public class OpcTest {
    @Autowired
    private OpcService opcService;

    @SneakyThrows
    public void browseTest() {
        browse(Identifiers.ObjectsFolder);
    }

    @SneakyThrows
    private void browse(NodeId browseRoot) {
        var conn = (MiloOpcConnection) ((KepOpcConnection) opcService.getConnection()).getConnection();
        var client = conn.getClient();
        DataTypeTree tree = DataTypeTreeBuilder.build(client);

        BrowseDescription browse = new BrowseDescription(
                browseRoot,
                BrowseDirection.Forward,
                Identifiers.References,
                true,
                uint(NodeClass.Object.getValue()
                        | NodeClass.Variable.getValue()),
                uint(BrowseResultMask.All.getValue())
        );
        BrowseResult browseResult = client.browse(browse).get();
        for(var ref : browseResult.getReferences()) {
            log.debug("RD: {}", ref);
            if(ref.getNodeId().getIdentifier().toString().contains("Buerkle")
                && !ref.getBrowseName().getName().startsWith("_")) {
                log.debug("node: {}", ref.getNodeId().getIdentifier().toString());
                browse(ref.getNodeId().toNodeId(client.getNamespaceTable()).orElse(null));
                if(ref.getNodeClass().equals(NodeClass.Variable)) {
                    log.debug("nodeIdExpanded: {}", ref.getNodeId());
                    var nodeId = ref.getNodeId().toNodeId(client.getNamespaceTable()).orElse(null);
                    log.debug("nodeId: {}", nodeId);

                    List<ReadValueId> readValueIds = List.of(new ReadValueId(
                            nodeId,
                            AttributeId.DataType.uid(),
                            null,
                            QualifiedName.NULL_VALUE
                    ));

                    var tGetDataTypeRead = DebugPerfTimer.start("getDataTypeRead");
                    NodeId dataType = (NodeId)
                            client.read(0.0, TimestampsToReturn.Neither, readValueIds)
                            .get().getResults()[0].getValue().getValue();
                    tGetDataTypeRead.endAndPrint(log);

                    Class<?> clazz = tree.getBackingClass(dataType);
                    System.out.println(clazz);

                    var tReadValue = DebugPerfTimer.start("readValue");
                    var value = client.readValue(0.0, TimestampsToReturn.Neither, nodeId)
                            .get().getValue().getValue();
                    tReadValue.endAndPrint(log);
                    System.out.println(value);
                }
            }
        }
    }
}
