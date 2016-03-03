package com.marklogic.hub;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.Format;
import com.marklogic.hub.flow.Flow;
import com.marklogic.hub.flow.FlowType;

public class EndToEndTestXqyXml extends HubTestBase {
    private static final String DOMAIN = "e2edomain";
    private static File pluginsDir = new File("./ye-olde-plugins");

    @BeforeClass
    public static void setup() throws IOException {
        XMLUnit.setIgnoreWhitespace(true);

        installHub();

        Scaffolding.createDomain(DOMAIN, pluginsDir);
        Scaffolding.createFlow(DOMAIN, "testinput", FlowType.INPUT, PluginFormat.XQUERY, Format.XML, pluginsDir);
        Scaffolding.createFlow(DOMAIN, "testconformance", FlowType.CONFORMANCE, PluginFormat.XQUERY, Format.XML, pluginsDir);

        new DataHub(host, stagingPort, finalPort, user, password).installUserModules("./ye-olde-plugins");
    }

    @AfterClass
    public static void teardown() throws IOException {
        uninstallHub();
        FileUtils.deleteDirectory(pluginsDir);
    }

    @Test
    public void runFlows() throws IOException, ParserConfigurationException, SAXException {
        FlowManager fm = new FlowManager(stagingClient);
        Flow inputFlow = fm.getFlow(DOMAIN, "testinput", FlowType.INPUT);
        Flow conformanceFlow = fm.getFlow(DOMAIN, "testconformance", FlowType.CONFORMANCE);

        URL url = HubTestBase.class.getClassLoader().getResource("e2e-test/input");
        HubConfig config = getHubConfig(url.getPath());
        fm.runInputFlow(inputFlow, config);

        assertXMLEqual(getXmlFromResource("e2e-test/staged.xml"), stagingDocMgr.read("/input.xml").next().getContent(new DOMHandle()).get());

        JobFinishedListener conformanceFlowListener = new JobFinishedListener();
        fm.runFlow(conformanceFlow, 10, conformanceFlowListener);
        conformanceFlowListener.waitForFinish();
        assertXMLEqual(getXmlFromResource("e2e-test/final.xml"), finalDocMgr.read("/input.xml").next().getContent(new DOMHandle()).get());
    }
}