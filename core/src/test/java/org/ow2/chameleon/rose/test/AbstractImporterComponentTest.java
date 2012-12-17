package org.ow2.chameleon.rose.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.ow2.chameleon.rose.AbstractImporterComponent;
import org.ow2.chameleon.rose.ImporterService;
import org.ow2.chameleon.rose.RoseMachine;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 *  Test Suite of the {@link AbstractImporterComponent} class.
 * @author barjo
 */
public class AbstractImporterComponentTest {
	private static final int IMPORT_MAX = 10; //Number max of Import to be tested within a single test.
	
	//Mock object
	@Mock LogService logservice;
	@Mock RoseMachine machine;
	@Mock Importer importermock;
	
	//Tested Object
	TestedClass creator;

	@Before
	public void setUp(){
		MockitoAnnotations.initMocks(this); //initialize the object with mocks annotations
		creator = new TestedClass(); 
	}
	
	@After
	public void tearDown(){
		
	}

	/**
	 * Test the {@link ImporterService#importService(EndpointDescription, Map)
	 * importService(mock desc,null)} while the proxy-creator has not been
	 * validate
	 */
	@Test
	public void testImportEndpointWhileInvalid(){
		EndpointDescription desc = mock(EndpointDescription.class);
		
		ImportRegistration reg = creator.importService(desc, null);
		
		assertNull(reg.getImportReference()); //The import must have failed
		assertNotNull(reg.getException()); //An exception must have been thrown
		
		reg.close(); //Close the registration
		assertNull(reg.getException()); //Now that the registration has been closed, getException must return null.
		
		creator.stop(); //Invalidate the instance
	}
	
	/**
	 * Test the {@link ImporterService#importService(EndpointDescription, Map)
	 * importService(mock desc,null)} while the proxy-creator is valid.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testImportServiceWhileValid(){
		EndpointDescription desc = mock(EndpointDescription.class);
		
		creator.start(); //validate the simulated instance
		ImportRegistration reg = creator.importService(desc, null);

		//verify that createProxy has been called
		verify(importermock,times(1)).createProxy(eq(desc), anyMap());

		
		assertEquals(1, creator.nbProxies()); //Verify that the create method has been called
		
		ImportReference iref = reg.getImportReference(); //get the ImportReference
		
		assertNotNull(iref); //Import is a success
		assertNull(reg.getException()); //No Exception
		
		reg.close(); //UnImport !
		
		//verify that the destroyproxy method has been called only once
		verify(importermock,times(1)).destroyProxy(eq(desc), any(ServiceRegistration.class));
		
		assertNull(reg.getImportReference()); //Now that is has been closed
		assertTrue(creator.getAllImportReference().isEmpty()); //No more ImportReferences, that was the last registration

		assertEquals(0, creator.nbProxies()); //Verify that the destroy method has been called
		
		creator.stop(); //Invalidate the instance
	}
	
	/**
	 * Test the {@link ImporterService#importService(EndpointDescription, Map)
	 * importService(mock desc,null)} while the proxy-creator is valid and then stop the importer.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testImportServiceWhileValidAndThenStop(){
		EndpointDescription desc = mock(EndpointDescription.class);
		
		creator.start(); //validate the simulated instance
		ImportRegistration reg = creator.importService(desc, null);
		
		//verify that createProxy has been called
		verify(importermock,times(1)).createProxy(eq(desc), anyMap());
		
		assertEquals(1, creator.nbProxies()); //Verify that the create method has been called
		
		ImportReference iref = reg.getImportReference(); //get the ImportReference
		
		assertNotNull(iref); //Import is a success
		assertNull(reg.getException()); //No Exception
		
		creator.stop(); //stop the importer
		
		//verify that the destroyproxy method has been called only once
		verify(importermock,times(1)).destroyProxy(eq(desc), any(ServiceRegistration.class));
		
		assertNull(reg.getImportReference().getImportedEndpoint()); //Now that is has been closed
		assertTrue(creator.getAllImportReference().isEmpty()); //No more ImportReferences, that was the last registration

		assertEquals(0, creator.nbProxies()); //Verify that the destroy method has been called
		
		//No unexpected call to destroy or import
		verifyNoMoreInteractions(importermock);
	}
	
	/**
	 * Test the {@link ImporterService#importService(EndpointDescription, Map)} )
	 * ImportService(mock sref,null)} while the proxy-creator is valid.
	 * Import , close and then re Import
	 */
	@Test
	public void testReImportServiceWhileValid(){
		EndpointDescription desc = mock(EndpointDescription.class); //Create the mock EndpointDEscription to be imported
		
		creator.start(); //validate the simulated instance
		ImportRegistration reg = creator.importService(desc, null); //Import
		
		reg.close(); //UnImport !
		
		reg = creator.importService(desc, null); //reImport
		
		assertEquals(1, creator.nbProxies()); //Verify that the create method has been called
		
		ImportReference xref = reg.getImportReference(); //get the ImportReference
		
		assertNotNull(xref); //Import is a success
		assertNull(reg.getException()); //No Exception
		
		assertTrue(creator.getAllImportReference().contains(xref)); //Checked that AllimportReferences is up to date.
		reg.close(); //Re unImport !
		
		assertNull(reg.getImportReference()); //Now that is has been closed
		assertTrue(creator.getAllImportReference().isEmpty()); //No more ImportReferences, that was the last registration

		assertEquals(0, creator.nbProxies()); //Verify that the destroy method has been called
		
		creator.stop(); //Invalidate the instance
	}
	
	/**
	 * Test the {@link ImporterService#importService(EndpointDescription, Map)
	 * ImportService(mock desc,null)} while the proxy-creator is valid and with multiple Import of different endpoint.
	 */
	@Test
	public void testImportServiceWhileValidMultiple(){
		Collection<ImportRegistration> regs = new HashSet< ImportRegistration>();
		
		creator.start(); //validate the simulated instance
		for (int i=0;i<IMPORT_MAX;i++){
			EndpointDescription desc = mock(EndpointDescription.class);

			
			ImportRegistration reg = creator.importService(desc, null); //Import
			ImportReference xref = reg.getImportReference(); //get the ImportReference
			
		
			assertNotNull(xref); //Import is a success
			assertNull(reg.getException()); //No Exception
		
			assertTrue(creator.getAllImportReference().contains(xref)); //Checked that AllimportReferences is up to date.
			
			regs.add(reg);
			
			assertEquals(creator.getAllImportReference().size(), i+1); //one more
			assertEquals(i+1, creator.nbProxies()); //Check that create has been called
		}
		
		for (ImportRegistration reg : regs) {
			ImportReference xref = reg.getImportReference(); //get the ImportReference
			
			reg.close(); //UnImport !
			assertNull(reg.getImportReference()); //Now that is has been closed
			assertFalse(creator.getAllImportReference().contains(xref)); //No more Imported
		}
		
		assertEquals(0, creator.nbProxies()); //Verify that the destroy method has been called

		creator.stop(); //Invalidate the instance
	}
	
	/**
	 * Test the {@link ImporterService#importService(EndpointDescription, Map)
	 * ImportService(mock desc,null)} while the proxy-creator is valid and with multiple Import of the same endpoint.
	 */
	@Test
	public void testImportServiceWhileValidMultipleSame(){
		EndpointDescription desc = mock(EndpointDescription.class); //Create a Mock EndpointDescription to be Imported (same for all)
		
		Collection<ImportRegistration> regs = new HashSet< ImportRegistration>();
		
		creator.start(); //validate the simulated instance
		for (int i=0;i<IMPORT_MAX;i++){
			ImportRegistration reg = creator.importService(desc, null); //Import
			
			assertNotNull(reg.getImportReference()); //Import is a success
			assertNull(reg.getException()); //No Exception
		
			assertTrue(creator.getAllImportReference().contains(reg.getImportReference())); //Checked that AllimportReferences is up to date.
			assertEquals(creator.getAllImportReference().size(), 1); //only one ImportReference for a given EndpointDescription
			assertEquals(1, creator.nbProxies()); //Check than create has been called once and only once

			regs.add(reg);
		}
		
		int count = IMPORT_MAX;
		for (ImportRegistration reg : regs) {	
			ImportReference ireg = reg.getImportReference();
			reg.close(); //UnImport !
			assertNull(reg.getImportReference()); //Now that is has been closed
			
			if(--count > 0){
				assertTrue(creator.getAllImportReference().contains(ireg)); //Endpoint is still Imported since there is other ImportRegistration linked to it
				assertEquals(creator.getAllImportReference().size(), 1); //still one
				assertEquals(1, creator.nbProxies()); //Still one importReference
			}
		}
		
		assertEquals(0, creator.getAllImportReference().size()); //The ImportReference has been closed since there is no more ImportRegistration linked to it.
		assertEquals(0, creator.nbProxies()); //Verify that the destroy method has been called

		creator.stop(); //Invalidate the instance
	}
	
	
	/**
	 * Convenient implementation of {@link AbstractImporterComponent} in order to test it.
	 * @author barjo
	 */
	public class TestedClass extends AbstractImporterComponent {
		private Collection<ServiceRegistration> regs = new HashSet<ServiceRegistration>();
		
		@Override
		protected ServiceRegistration createProxy(
				EndpointDescription description,
				Map<String, Object> extraProperties) {
			ServiceRegistration reg = mock(ServiceRegistration.class);
			regs.add(reg);
			
			importermock.createProxy(description, extraProperties); //for test purpose
			return reg;
		}

		@Override
		protected void destroyProxy(EndpointDescription description,
				ServiceRegistration registration) {
			regs.remove(registration);
			importermock.destroyProxy(description, registration); //for test purpose
		}

		

		@Override
		protected LogService getLogService() {
			return logservice;
		}

		public List<String> getConfigPrefix() {
			return null;
		}
		
		public void start() {
			super.start();
		}
		
		public void stop() {
			super.stop();
		}
		
		public int nbProxies(){
			return regs.size();
		}

		public RoseMachine getRoseMachine() {
			return machine;
		}
	}
	
	private interface Importer {
		ServiceRegistration createProxy(EndpointDescription description, Map<String, Object> extraProperties);
		void destroyProxy(EndpointDescription description, ServiceRegistration registration);
	}
}
