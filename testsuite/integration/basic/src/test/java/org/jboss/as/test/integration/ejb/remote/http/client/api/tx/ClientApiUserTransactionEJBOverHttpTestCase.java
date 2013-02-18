/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.remote.http.client.api.tx;

import javax.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.ejb.remote.client.api.tx.Batch;
import org.jboss.as.test.integration.ejb.remote.client.api.tx.BatchCreationBean;
import org.jboss.as.test.integration.ejb.remote.client.api.tx.BatchFetchingBean;
import org.jboss.as.test.integration.ejb.remote.client.api.tx.BatchRetriever;
import org.jboss.as.test.integration.ejb.remote.client.api.tx.CMTBean;
import org.jboss.as.test.integration.ejb.remote.client.api.tx.CMTRemote;
import org.jboss.as.test.integration.ejb.remote.client.api.tx.RemoteBatch;
import org.jboss.as.test.integration.ejb.remote.client.api.tx.SimpleAppException;
import org.jboss.as.test.integration.ejb.remote.http.client.api.AbstractClientApiEJBOverHttpTestCase;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBClientTransactionContext;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Eduardo Martins
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ClientApiUserTransactionEJBOverHttpTestCase extends AbstractClientApiEJBOverHttpTestCase {

    @Deployment
    public static Archive<?> getDeployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(Batch.class.getPackage());
        jar.addAsManifestResource(Batch.class.getPackage(), "persistence.xml",
                "persistence.xml");
        ear.addAsModule(jar);
        return ear;
    }

    /**
     * Create and setup the EJB client context backed by the remoting receiver
     *
     * @throws Exception
     */
    @Before
    public void before() throws Exception {
        super.before();
        final EJBClientTransactionContext localUserTxContext = EJBClientTransactionContext.createLocal();
        // set the tx context
        EJBClientTransactionContext.setGlobalContext(localUserTxContext);
    }

    /**
     * Tests a empty begin()/commit()
     *
     * @throws Exception
     */
    @Test
    public void testEmptyTxCommit() throws Exception {
        final UserTransaction userTransaction = EJBClient.getUserTransaction(NODENAME);
        userTransaction.begin();
        userTransaction.commit();
    }

    /**
     * Tests a empty begin()/rollback()
     *
     * @throws Exception
     */
    @Test
    public void testEmptyTxRollback() throws Exception {
        final UserTransaction userTransaction = EJBClient.getUserTransaction(NODENAME);
        userTransaction.begin();
        userTransaction.rollback();
    }

    /**
     * Tests a call to a bean method with a Mandatory tx attribute, by initiating a UserTransaction on the remote client side.
     * This test ensures that the tx is propagated to the server during the bean invocation
     *
     * @throws Exception
     */
    @Test
    public void testMandatoryTxOnSLSB() throws Exception {
        final StatelessEJBLocator<CMTRemote> cmtRemoteBeanLocator = new StatelessEJBLocator<CMTRemote>(CMTRemote.class,
                APP_NAME, MODULE_NAME, CMTBean.class.getSimpleName(), DISTINCT_NAME);
        final CMTRemote cmtRemoteBean = EJBClient.createProxy(cmtRemoteBeanLocator);

        final UserTransaction userTransaction = EJBClient.getUserTransaction(NODENAME);
        userTransaction.begin();
        cmtRemoteBean.mandatoryTxOp();
        userTransaction.commit();
    }

    @Test
    public void testBatchOperationsInTx() throws Exception {
        final StatelessEJBLocator<RemoteBatch> batchBeanLocator = new StatelessEJBLocator<RemoteBatch>(RemoteBatch.class,
                APP_NAME, MODULE_NAME, BatchCreationBean.class.getSimpleName(), "");
        final RemoteBatch batchBean = EJBClient.createProxy(batchBeanLocator);

        final StatelessEJBLocator<BatchRetriever> batchRetrieverLocator = new StatelessEJBLocator<BatchRetriever>(
                BatchRetriever.class, APP_NAME, MODULE_NAME, BatchFetchingBean.class.getSimpleName(), DISTINCT_NAME);
        final BatchRetriever batchRetriever = EJBClient.createProxy(batchRetrieverLocator);

        final UserTransaction userTransaction = EJBClient.getUserTransaction(NODENAME);
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
        Assert.assertEquals("Unexpected steps in batch, after step1", successFullyCompletedSteps,
                batchAfterStep1.getStepNames());

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
        Assert.assertEquals("Unexpected steps in batch, after app exception step2", successFullyCompletedSteps,
                batchAfterAppExceptionStep2.getStepNames());

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
        Assert.assertEquals("Unexpected steps in batch, after step2 before commit", successFullyCompletedSteps,
                batchAfterStep2BeforeCommit.getStepNames());

        // now commit
        userTransaction.commit();
        // keep track of successfully completely steps
        successFullyCompletedSteps = successFullyCompletedSteps + "," + step2;

        // now retrieve and check the batch
        final Batch batchAfterStep2 = batchRetriever.fetchBatch(batchName);
        Assert.assertNotNull("Batch after step2 was null", batchAfterStep2);
        Assert.assertEquals("Unexpected steps in batch, after step2", successFullyCompletedSteps,
                batchAfterStep2.getStepNames());

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
        Assert.assertEquals("Unexpected steps in batch, after step3", successFullyCompletedSteps,
                batchAfterStep3.getStepNames());

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
            // Assert.assertEquals("Unexpected transaction state", Status.STATUS_ROLLEDBACK,
            // userTransaction.getStatus());
            userTransaction.rollback();
        }

        // now retrieve and check the batch
        final Batch batchAfterSysException = batchRetriever.fetchBatch(batchName);
        Assert.assertNotNull("Batch after system exception was null", batchAfterSysException);
        Assert.assertEquals("Unexpected steps in batch, after system exception", successFullyCompletedSteps,
                batchAfterSysException.getStepNames());
    }

}
