/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.deployment.managedbean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.naming.Context;
import javax.naming.LinkRef;

import org.jboss.as.deployment.Phase;
import org.jboss.as.deployment.chain.DeploymentChain;
import org.jboss.as.deployment.chain.DeploymentChainImpl;
import org.jboss.as.deployment.chain.DeploymentChainService;
import org.jboss.as.deployment.managedbean.container.ManagedBeanContainer;
import org.jboss.as.deployment.managedbean.container.ManagedBeanService;
import org.jboss.as.deployment.managedbean.processors.ManagedBeanAnnotationProcessor;
import org.jboss.as.deployment.managedbean.processors.ManagedBeanDeploymentProcessor;
import org.jboss.as.deployment.module.DeploymentModuleLoaderImpl;
import org.jboss.as.deployment.module.DeploymentModuleLoaderProcessor;
import org.jboss.as.deployment.module.ManifestAttachmentProcessor;
import org.jboss.as.deployment.module.ModuleConfigProcessor;
import org.jboss.as.deployment.module.ModuleDependencyProcessor;
import org.jboss.as.deployment.module.ModuleDeploymentProcessor;
import org.jboss.as.deployment.naming.ContextNames;
import org.jboss.as.deployment.naming.ModuleContextProcessor;
import org.jboss.as.deployment.processor.AnnotationIndexProcessor;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test case to do some basic Managed bean deployment functionality checking.
 *
 * @author John E. Bailey
 */
public class ManagedBeanDeploymentTestCase extends AbstractManagedBeanTest {
    private static final DeploymentChain deploymentChain = new DeploymentChainImpl();
    private static DeploymentModuleLoaderProcessor deploymentModuleLoaderProcessor = new DeploymentModuleLoaderProcessor(new DeploymentModuleLoaderImpl());

    Context javaContext;

    @BeforeClass
    public static void setupChain() {
        deploymentChain.addProcessor(new ManifestAttachmentProcessor(), Phase.MANIFEST_ATTACHMENT_PROCESSOR);
        deploymentChain.addProcessor(new AnnotationIndexProcessor(), Phase.ANNOTATION_INDEX_PROCESSOR);
        deploymentChain.addProcessor(new ManagedBeanAnnotationProcessor(), Phase.MANAGED_BEAN_ANNOTATION_PROCESSOR);
        deploymentChain.addProcessor(new ModuleDependencyProcessor(), Phase.MODULE_DEPENDENCY_PROCESSOR);
        deploymentChain.addProcessor(new ModuleConfigProcessor(), Phase.MODULE_CONFIG_PROCESSOR);
        deploymentChain.addProcessor(new ModuleDeploymentProcessor(), Phase.MODULE_DEPLOYMENT_PROCESSOR);
        deploymentChain.addProcessor(new ModuleContextProcessor(), Phase.MODULE_CONTEXT_PROCESSOR);
        deploymentChain.addProcessor(new ManagedBeanDeploymentProcessor(), Phase.MANAGED_BEAN_DEPLOYMENT_PROCESSOR);
    }

    @Override
    protected void setupServices(final ServiceTarget target) throws Exception {
        super.setupServices(target);
        deploymentChain.removeProcessor(deploymentModuleLoaderProcessor, Phase.DEPLOYMENT_MODULE_LOADER_PROCESSOR);
        deploymentModuleLoaderProcessor = new DeploymentModuleLoaderProcessor(new DeploymentModuleLoaderImpl());
        deploymentChain.addProcessor(deploymentModuleLoaderProcessor, Phase.DEPLOYMENT_MODULE_LOADER_PROCESSOR);

        javaContext = new MockContext();
        target.addService(ContextNames.JAVA_CONTEXT_SERVICE_NAME, new PassthroughService<Context>(javaContext))
            .install();
        final Context globalContext = javaContext.createSubcontext("global");
        target.addService(ContextNames.GLOBAL_CONTEXT_SERVICE_NAME, new PassthroughService<Context>(globalContext))
            .install();
        globalContext.bind("someNumber", Integer.valueOf(99));

        target.addService(DeploymentChain.SERVICE_NAME, new DeploymentChainService(deploymentChain))
            .install();
    }

    @Test
    public void testDeployment() throws Exception {
        final VirtualFile deploymentRoot = initializeDeployment("/test/managedBeanDeployment.jar");

        final String expectedDeploymentName = getDeploymentName(deploymentRoot);
        final LinkRef moduleLink = new LinkRef("java:global/" + expectedDeploymentName);
        javaContext.rebind("module", moduleLink);

        executeDeployment(deploymentRoot);

        final ServiceController<?> testServiceController = serviceContainer.getService(ManagedBeanService.SERVICE_NAME.append(expectedDeploymentName, "TestBean"));
        assertNotNull(testServiceController);

        final ManagedBeanContainer<TestManagedBean> managedBeanContainer = (ManagedBeanContainer<TestManagedBean>)testServiceController.getValue();

        final TestManagedBean testManagedBean = managedBeanContainer.createInstance();
        assertNotNull(testManagedBean);
        assertFalse(testManagedBean.equals(managedBeanContainer.createInstance()));


        System.out.println(javaContext.lookup("java:global/" + expectedDeploymentName + "/TestBean"));
    }

    @Test
    public void testBasicInjection() throws Exception {
        final VirtualFile deploymentRoot = initializeDeployment("/test/managedBeanDeployment.jar");

        final String expectedDeploymentName = getDeploymentName(deploymentRoot);
        final LinkRef moduleLink = new LinkRef("java:global/" + expectedDeploymentName);
        javaContext.rebind("module", moduleLink);

        executeDeployment(deploymentRoot);

        final ServiceController<?> testServiceController = serviceContainer.getService(ManagedBeanService.SERVICE_NAME.append(expectedDeploymentName, "TestBeanWithInjection"));
        assertNotNull(testServiceController);
        final ManagedBeanContainer<TestManagedBeanWithInjection> managedBeanContainer = (ManagedBeanContainer<TestManagedBeanWithInjection>) testServiceController.getValue();
        final TestManagedBeanWithInjection testManagedBean = managedBeanContainer.createInstance();
        assertNotNull(testManagedBean);
        assertNotNull(testManagedBean.getOther());
        assertFalse(testManagedBean.equals(managedBeanContainer.createInstance()));
    }

    @Test
    public void testInterceptors() throws Exception {
        final VirtualFile deploymentRoot = initializeDeployment("/test/managedBeanDeployment.jar");

        final String expectedDeploymentName = getDeploymentName(deploymentRoot);
        final LinkRef moduleLink = new LinkRef("java:global/" + expectedDeploymentName);
        javaContext.rebind("module", moduleLink);

        executeDeployment(deploymentRoot);

        final ServiceController<?> testServiceController = serviceContainer.getService(ManagedBeanService.SERVICE_NAME.append(expectedDeploymentName, "TestBeanWithInjection"));
        assertNotNull(testServiceController);
        final ManagedBeanContainer<TestManagedBeanWithInjection> managedBeanContainer = (ManagedBeanContainer<TestManagedBeanWithInjection>) testServiceController.getValue();
        final TestManagedBeanWithInjection testManagedBean = managedBeanContainer.createInstance();
        assertNotNull(testManagedBean);

        TestInterceptor.invoked = false;
        TestManagedBeanWithInjection.invoked = false;
        assertNotNull(testManagedBean.getOther());
        assertTrue(TestInterceptor.invoked);
        assertTrue(TestManagedBeanWithInjection.invoked);
    }


    private VirtualFile initializeDeployment(final String path) throws Exception {
        final VirtualFile virtualFile = VFS.getChild(getResource(ManagedBeanDeploymentTestCase.class, path));
        copyResource(ManagedBeanDeploymentTestCase.class, "/org/jboss/as/deployment/managedbean/TestManagedBean.class", path, "org/jboss/as/deployment/managedbean");
        copyResource(ManagedBeanDeploymentTestCase.class, "/org/jboss/as/deployment/managedbean/TestManagedBeanWithInjection.class", path, "org/jboss/as/deployment/managedbean");
        copyResource(ManagedBeanDeploymentTestCase.class, "/org/jboss/as/deployment/managedbean/TestInterceptor.class", path, "org/jboss/as/deployment/managedbean");
        return virtualFile;
    }
}
