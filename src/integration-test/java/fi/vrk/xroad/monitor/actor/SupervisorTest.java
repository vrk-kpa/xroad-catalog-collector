/**
 * The MIT License
 * Copyright (c) 2017, Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package fi.vrk.xroad.monitor.actor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.routing.SmallestMailboxPool;
import akka.testkit.TestActorRef;
import akka.testkit.javadsl.TestKit;
import fi.vrk.xroad.monitor.base.ElasticsearchTestBase;
import fi.vrk.xroad.monitor.elasticsearch.EnvMonitorDataStorageDaoImpl;
import fi.vrk.xroad.monitor.elasticsearch.EnvMonitorDataStorageServiceImpl;
import fi.vrk.xroad.monitor.extensions.SpringExtension;
import fi.vrk.xroad.monitor.extractor.MonitorDataExtractor;
import fi.vrk.xroad.monitor.extractor.MonitorDataRequestBuilder;
import fi.vrk.xroad.monitor.extractor.MonitorDataResponseParser;
import fi.vrk.xroad.monitor.parser.SecurityServerInfo;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link Supervisor}
 */
@Slf4j
@SpringBootTest(classes = {
        SpringExtension.class,
        Supervisor.class,
        ApplicationContext.class,
        MonitorDataHandlerActor.class,
        ResultCollectorActor.class,
        ElasticsearchInitializerActor.class,
        MonitorDataExtractor.class,
        MonitorDataRequestBuilder.class,
        MonitorDataResponseParser.class,
        EnvMonitorDataStorageDaoImpl.class,
        EnvMonitorDataStorageServiceImpl.class})
@RunWith(SpringRunner.class)
public class SupervisorTest extends ElasticsearchTestBase {

    @Autowired
    SpringExtension springExtension;

    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    /**
     * Cleanup test data
     */
    @Before
    @After
    public void cleanup() throws IOException {
        removeCurrentIndexAndAlias();
    }

    /**
     * Tests the system logic so that when supervisor starts processing
     * the collector receives the processing results.
     */
    @Test
    public void testSupervisor() {

        Set<SecurityServerInfo> securityServerInfos = new HashSet<>();
        securityServerInfos.add(new SecurityServerInfo("Eka", "Osoite", "memberClass", "memberCode"));
        securityServerInfos.add(new SecurityServerInfo("", "", "", ""));
        securityServerInfos.add(new SecurityServerInfo("Toka", "osoite", "memberClass", "memberCode"));

        final TestActorRef<ResultCollectorActor> resultCollectorActor = TestActorRef.create(
                system, Props.create(ResultCollectorActor.class));

        final TestActorRef<MonitorDataHandlerActor> monitorDataRequestPoolRouter =
                TestActorRef.create(system, new SmallestMailboxPool(2).props(
                        springExtension.props(
                                "monitorDataHandlerActor",
                                resultCollectorActor))
                );

        final TestActorRef<ElasticsearchInitializerActor> elasticsearchInitializerActor = TestActorRef.create(
            system, Props.create(ElasticsearchInitializerActor.class));

        // create supervisor
        final Props supervisorProps = springExtension.props("supervisor");
        final TestActorRef<Supervisor> supervisorRef = TestActorRef.create(system, supervisorProps, "supervisor");
        Supervisor underlying = supervisorRef.underlyingActor();
        underlying.overrideResultCollectorActor(resultCollectorActor);
        underlying.overrideMonitorDataRequestPoolRouter(monitorDataRequestPoolRouter);
        underlying.overrideElasticsearchInitializerActor(elasticsearchInitializerActor);

        // send message to supervisor to start processing
        supervisorRef.receive(new Supervisor.StartCollectingMonitorDataCommand(securityServerInfos),
            ActorRef.noSender());

        // assert that all the results have been received
        assertEquals(securityServerInfos.size(), resultCollectorActor.underlyingActor().getNumProcessedResults());

    }

}
