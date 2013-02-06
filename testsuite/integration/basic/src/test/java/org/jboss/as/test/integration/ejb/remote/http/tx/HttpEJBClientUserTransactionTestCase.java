/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.remote.http.tx;

import java.util.Properties;

import javax.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.ejb.remote.common.EJBManagementUtil;
import org.jboss.as.test.integration.ejb.remote.http.CounterBean;
import org.jboss.as.test.integration.ejb.remote.http.CounterRemote;
import org.jboss.as.test.integration.ejb.remote.http.EchoBean;
import org.jboss.as.test.integration.ejb.remote.http.EchoRemote;
import org.jboss.as.test.integration.ejb.remote.http.HttpEJBRemoteServletDeployTestCase;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBClientTransactionContext;
import org.jboss.ejb.client.PropertiesBasedEJBClientConfiguration;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.ejb.client.remoting.ConfigBasedEJBClientContextSelector;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@RunAsClient
public class HttpEJBClientUserTransactionTestCase {

    private static final Logger logger = Logger.getLogger(HttpEJBClientUserTransactionTestCase.class);

    private static final String APP_NAME = "ejb-remote-client-api-test";
    private static final String MODULE_NAME = "ejb";
    private static final String EAR_DEPLOYMENT_NAME = APP_NAME;
    private static final String SERVLET_DEPLOYMENT_NAME = "ejb3-remote";

    @ArquillianResource
    private Deployer deployer;
    /**
     * Creates an EJB deployment
     *
     * @return
     */
    @Deployment(name = HttpEJBClientUserTransactionTestCase.EAR_DEPLOYMENT_NAME, managed = false)
    public static Archive<?> createEar() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(HttpEJBClientUserTransactionTestCase.class.getPackage());
        jar.addAsManifestResource(HttpEJBClientUserTransactionTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        ear.addAsModule(jar);
        return ear;
    }

    @Deployment
    public static WebArchive createServlet() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, SERVLET_DEPLOYMENT_NAME+".war");
        war.setWebXML(HttpEJBClientUserTransactionTestCase.class.getPackage(), "web.xml");
        war.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.ejb3 \n"), "MANIFEST.MF");
        return war;
    }

/*
    @Test
    public void testEmptyTxCommit() throws Exception {

     // there are config files in the classpath, by setting this property the client won't look at it and merge with the test config
        final String propertyName = "jboss.ejb.client.properties.skip.classloader.scan";
        final String propertyValue = System.getProperty(propertyName);
        try {
            System.setProperty(propertyName,"true");
            try {
                // deploy the unmanaged sar
                deployer.deploy(HttpEJBClientUserTransactionTestCase.EAR_DEPLOYMENT_NAME);
                // setup client config
                Properties properties = getClientAPIProperties();
                // create and activate the selector with the custom config
                final EJBClientConfiguration clientConfiguration = new PropertiesBasedEJBClientConfiguration(properties);
                final ConfigBasedEJBClientContextSelector selector = new ConfigBasedEJBClientContextSelector(clientConfiguration);
                ContextSelector<EJBClientContext> previousSelector = EJBClientContext.setSelector(selector);
                try {
                    String nodeName = "http://"+"localhost"+":"+"8080"+"/"+SERVLET_DEPLOYMENT_NAME+"/";
                    final UserTransaction userTransaction = EJBClient.getUserTransaction(nodeName);
                    userTransaction.begin();
                    userTransaction.commit();
                } finally {
                    if (previousSelector != null) {
                        EJBClientContext.setSelector(previousSelector);
                    }
                }
            } finally {
                // undeploy it
                deployer.undeploy(HttpEJBClientUserTransactionTestCase.EAR_DEPLOYMENT_NAME);
            }
        } finally {
            if (propertyValue != null) {
                System.setProperty(propertyName,propertyValue);
            } else {
                System.clearProperty(propertyName);
            }
        }
    }
*/
    private Properties getClientAPIProperties() {
        Properties properties = new Properties();
        properties.put("endpoint.name", "http");
        properties.put("remote.connections","default");
        properties.put("remote.connection.default.transport","http");
        properties.put("remote.connection.default.host","localhost");
        properties.put("remote.connection.default.port","8080");
        properties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.HTTPS","false");
        properties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.SERVLET_NAME",SERVLET_DEPLOYMENT_NAME);
        properties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.HTTP_CLIENT","jdk");
        properties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.APP_NAME",APP_NAME);
        properties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.MODULE_NAME",MODULE_NAME);
        properties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.DISTINCT_NAME","");
        return properties;
    }

    /*
    @Test
    public void testEmptyTxRollback() throws Exception {
        final UserTransaction userTransaction = EJBClient.getUserTransaction(nodeName);
        userTransaction.begin();
        userTransaction.rollback();
    }

    @Before
    public void beforeTest() throws Exception {
        final EJBClientTransactionContext localUserTxContext = EJBClientTransactionContext.createLocal();
        // set the tx context
        EJBClientTransactionContext.setGlobalContext(localUserTxContext);

    }

    @Test
    public void testMandatoryTxOnSLSB() throws Exception {
        final StatelessEJBLocator<CMTRemote> cmtRemoteBeanLocator = new StatelessEJBLocator<CMTRemote>(CMTRemote.class, APP_NAME, MODULE_NAME, CMTBean.class.getSimpleName(), "");
        final CMTRemote cmtRemoteBean = EJBClient.createProxy(cmtRemoteBeanLocator);

        final UserTransaction userTransaction = EJBClient.getUserTransaction(nodeName);
        userTransaction.begin();
        cmtRemoteBean.mandatoryTxOp();
        userTransaction.commit();
    }
*/
    @Test
    public void testBatchOperationsInTx() throws Exception {

     // there are config files in the classpath, by setting this property the client won't look at it and merge with the test config
        final String propertyName = "jboss.ejb.client.properties.skip.classloader.scan";
        final String propertyValue = System.getProperty(propertyName);
        try {
            System.setProperty(propertyName,"true");
            try {
                // deploy the unmanaged sar
                deployer.deploy(HttpEJBClientUserTransactionTestCase.EAR_DEPLOYMENT_NAME);
                // setup client config
                Properties properties = getClientAPIProperties();
                // create and activate the selector with the custom config
                final EJBClientConfiguration clientConfiguration = new PropertiesBasedEJBClientConfiguration(properties);
                final ConfigBasedEJBClientContextSelector selector = new ConfigBasedEJBClientContextSelector(clientConfiguration);
                ContextSelector<EJBClientContext> previousSelector = EJBClientContext.setSelector(selector);
                try {
                    String nodeName = "http://"+"localhost"+":"+"8080"+"/"+SERVLET_DEPLOYMENT_NAME+"/";
                    final StatelessEJBLocator<RemoteBatch> batchBeanLocator = new StatelessEJBLocator<RemoteBatch>(RemoteBatch.class, APP_NAME, MODULE_NAME, BatchCreationBean.class.getSimpleName(), "");
                    final RemoteBatch batchBean = EJBClient.createProxy(batchBeanLocator);

                    final StatelessEJBLocator<BatchRetriever> batchRetrieverLocator = new StatelessEJBLocator<BatchRetriever>(BatchRetriever.class, APP_NAME, MODULE_NAME, BatchFetchingBean.class.getSimpleName(), "");
                    final BatchRetriever batchRetriever = EJBClient.createProxy(batchRetrieverLocator);

                    final UserTransaction userTransaction = EJBClient.getUserTransaction(nodeName);
                    final String batchName = "Simple Batch";
                    // create a batch
                    userTransaction.begin();
                    try {
                        batchBean.createBatch(batchName);
                    } catch (Exception e) {
                        userTransaction.rollback();
                        throw e;
                    }
                    userTransaction.commit();

                    // add step1 to the batch
                    final String step1 = "Simple step1";
                    userTransaction.begin();
                    try {
                        batchBean.step1(batchName, step1);
                    } catch (Exception e) {
                        userTransaction.rollback();
                        throw e;
                    }
                    userTransaction.commit();

                    String successFullyCompletedSteps = step1;

                    // fetch the batch and make sure it contains the right state
                    final Batch batchAfterStep1 = batchRetriever.fetchBatch(batchName);
                    Assert.assertNotNull("Batch after step1 was null", batchAfterStep1);
                    Assert.assertEquals("Unexpected steps in batch, after step1", successFullyCompletedSteps, batchAfterStep1.getStepNames());


                    // now add a failing step2
                    final String appExceptionStep2 = "App exception Step 2";
                    userTransaction.begin();
                    try {
                        batchBean.appExceptionFailingStep2(batchName, appExceptionStep2);
                        Assert.fail("Expected a application exception");
                    } catch (SimpleAppException sae) {
                        // expected
                        userTransaction.rollback();
                    }

                    final Batch batchAfterAppExceptionStep2 = batchRetriever.fetchBatch(batchName);
                    Assert.assertNotNull("Batch after app exception step2 was null", batchAfterAppExceptionStep2);
                    Assert.assertEquals("Unexpected steps in batch, after app exception step2", successFullyCompletedSteps, batchAfterAppExceptionStep2.getStepNames());

                    // now add a successful step2
                    final String step2 = "Simple Step 2";
                    userTransaction.begin();
                    try {
                        batchBean.successfulStep2(batchName, step2);
                    } catch (Exception e) {
                        userTransaction.rollback();
                        throw e;
                    }
                    // don't yet commit and try and retrieve the batch
                    final Batch batchAfterStep2BeforeCommit = batchRetriever.fetchBatch(batchName);
                    Assert.assertNotNull("Batch after step2, before commit was null", batchAfterStep2BeforeCommit);
                    Assert.assertEquals("Unexpected steps in batch, after step2 before commit", successFullyCompletedSteps, batchAfterStep2BeforeCommit.getStepNames());

                    // now commit
                    userTransaction.commit();
                    // keep track of successfully completely steps
                    successFullyCompletedSteps = successFullyCompletedSteps + "," + step2;

                    // now retrieve and check the batch
                    final Batch batchAfterStep2 = batchRetriever.fetchBatch(batchName);
                    Assert.assertNotNull("Batch after step2 was null", batchAfterStep2);
                    Assert.assertEquals("Unexpected steps in batch, after step2", successFullyCompletedSteps, batchAfterStep2.getStepNames());

                    // now add independent Step3 (i.e. the bean method has a REQUIRES_NEW semantics, so that the
                    // client side tx doesn't play a role)
                    final String step3 = "Simple Step 3";
                    userTransaction.begin();
                    batchBean.independentStep3(batchName, step3);
                    // rollback (but it shouldn't end up rolling back step3 because that was done in server side independent tx)
                    userTransaction.rollback();
                    // keep track of successfully completely steps
                    successFullyCompletedSteps = successFullyCompletedSteps + "," + step3;

                    // now retrieve and check the batch
                    final Batch batchAfterStep3 = batchRetriever.fetchBatch(batchName);
                    Assert.assertNotNull("Batch after step3 was null", batchAfterStep3);
                    Assert.assertEquals("Unexpected steps in batch, after step3", successFullyCompletedSteps, batchAfterStep3.getStepNames());

                    // now add step4 but don't commit
                    final String step4 = "Simple Step 4";
                    userTransaction.begin();
                    batchBean.step4(batchName, step4);

                    // now add a system exception throwing step
                    final String sysExceptionStep2 = "Sys exception step2";
                    try {
                        batchBean.systemExceptionFailingStep2(batchName, sysExceptionStep2);
                        Assert.fail("Expected a system exception");
                    } catch (Exception e) {
                        // expected exception
                        // TODO: We currently don't return the tx status from the server to the client, so the
                        // client has no knowledge of the tx status. This is something that can be implemented
                        // by passing along the tx status as a return attachment from a remote method invocation.
                        // For now, let's ignore it
                        //Assert.assertEquals("Unexpected transaction state", Status.STATUS_ROLLEDBACK, userTransaction.getStatus());
                        userTransaction.rollback();
                    }

                    // now retrieve and check the batch
                    final Batch batchAfterSysException = batchRetriever.fetchBatch(batchName);
                    Assert.assertNotNull("Batch after system exception was null", batchAfterSysException);
                    Assert.assertEquals("Unexpected steps in batch, after system exception", successFullyCompletedSteps, batchAfterSysException.getStepNames());

                } finally {
                    if (previousSelector != null) {
                        EJBClientContext.setSelector(previousSelector);
                    }
                }
            } finally {
                // undeploy it
                deployer.undeploy(HttpEJBClientUserTransactionTestCase.EAR_DEPLOYMENT_NAME);
            }
        } finally {
            if (propertyValue != null) {
                System.setProperty(propertyName,propertyValue);
            } else {
                System.clearProperty(propertyName);
            }
        }


    }

}
