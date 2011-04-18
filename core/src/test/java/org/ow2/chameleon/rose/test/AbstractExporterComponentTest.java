package org.ow2.chameleon.rose.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.ow2.chameleon.rose.AbstractExporterComponent;
import org.ow2.chameleon.rose.ExporterService;
import org.ow2.chameleon.rose.registry.ExportRegistryProvisioning;

/**
 *  Test Suite of the {@link AbstractExporterComponent} class.
 * @author barjo
 */
public class AbstractExporterComponentTest {
	private static final int EXPORT_MAX = 10; //Number max of export to be tested within a single test.
	
	//Mock object
	@Mock LogService logservice;
	@Mock EventAdmin eventadmin;
	@Mock ExportRegistryProvisioning registry;
	
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
	 * Test the {@link ExporterService#exportService(ServiceReference, Map)
	 * exportService(mock sref,null)} while the endpoint-creator has not been
	 * validate
	 */
	@Test
	public void testExportServiceWhileInvalid(){
		ServiceReference sref = mock(ServiceReference.class);
		
		ExportRegistration reg = creator.exportService(sref, null);
		
		assertNull(reg.getExportReference()); //The export must have failed
		assertNotNull(reg.getException()); //An exception must have been thrown
		
		reg.close(); //Close the registration
		assertNull(reg.getException()); //Now that the registration has been closed, getException must return null.
		
		creator.stop(); //Invalidate the instance
	}
	
	/**
	 * Test the {@link ExporterService#exportService(ServiceReference, Map)
	 * exportService(mock sref,null)} while the endpoint-creator is valid.
	 */
	@Test
	public void testExportServiceWhileValid(){
		ServiceReference sref = mock(ServiceReference.class);
		
		creator.start(); //validate the simulated instance
		ExportRegistration reg = creator.exportService(sref, null);
		ExportReference xref = reg.getExportReference(); //get the ExportReference
		
		assertNotNull(xref); //Export is a success
		assertNull(reg.getException()); //No Exception
		
		//Configure the registry behavior
		when(registry.remove(xref)).thenReturn(xref);
		
		assertEquals(xref, creator.getExportReference(sref)); //no strange side effect on the reference
		
		//Check that the ExportReference is published in the ExportRegistry
		verify(registry).put(xref,xref);
		
		reg.close(); //Unexport !
		
		assertNull(reg.getExportReference()); //Now that is has been closed
		assertNull(creator.getExportReference(sref)); //No more ExportReferences, that was the last registration

		verify(registry).remove(xref); //Verify the unregister method has been called once.
		
		creator.stop(); //Invalidate the instance
	}
	
	/**
	 * Test the {@link ExporterService#exportService(ServiceReference, Map)
	 * exportService(mock sref,null)} while the endpoint-creator is valid and with multiple export of different service.
	 */
	@Test
	public void testExportServiceWhileValidMultiple(){
		Collection<ExportRegistration> regs = new HashSet< ExportRegistration>();
		
		creator.start(); //validate the simulated instance
		for (int i=0;i<EXPORT_MAX;i++){
			ServiceReference sref = mock(ServiceReference.class); //Create a Mock ServiceReference to be exported
			
			ExportRegistration reg = creator.exportService(sref, null); //export
			ExportReference xref = reg.getExportReference(); //get the ExportReference
			
			//Configure the registry behavior
			when(registry.remove(xref)).thenReturn(xref);
			
		
			assertNotNull(xref); //Export is a success
			assertNull(reg.getException()); //No Exception
		
			assertEquals(xref, creator.getExportReference(sref)); //no strange side effect on the reference
			
			//assertEquals(i+1, creator.getAllExportReference().size()); //one ExportReference per ServiceReference
			regs.add(reg);
		}
		
		for (ExportRegistration reg : regs) {
			ExportReference xref = reg.getExportReference(); //get the ExportReference
			ServiceReference sref = xref.getExportedService(); //get the ServiceReference
			
			reg.close(); //Unexport !
			assertNull(reg.getExportReference()); //Now that is has been closed
			assertNull(creator.getExportReference(sref)); //No more exported
			
			//assertEquals(--count, creator.getAllExportReference().size()); //verify that one and only one has been removed
			verify(registry).remove(xref); //Verify that the ExportReference has been removed from the ExportRegistry.
		}
		
		creator.stop(); //Invalidate the instance
	}
	
	/**
	 * Test the {@link ExporterService#exportService(ServiceReference, Map)
	 * exportService(mock sref,null)} while the endpoint-creator is valid and with multiple export of the same service.
	 */
	@Test
	public void testExportServiceWhileValidMultipleSame(){
		ExportReference xref = null;
		ServiceReference sref = mock(ServiceReference.class); //Create a Mock ServiceReference to be exported (same for all)
		
		Collection<ExportRegistration> regs = new HashSet< ExportRegistration>();
		
		creator.start(); //validate the simulated instance
		for (int i=0;i<EXPORT_MAX;i++){
			ExportRegistration reg = creator.exportService(sref, null); //export
			xref = reg.getExportReference(); //get the ExportReference
			
			//Configure the registry behavior
			when(registry.remove(xref)).thenReturn(xref);
		
			assertNotNull(reg.getExportReference()); //Export is a success
			assertNull(reg.getException()); //No Exception
		
			assertEquals(reg.getExportReference(), creator.getExportReference(sref)); //no strange side effect on the reference
			//assertEquals(1, creator.getAllExportReference().size()); //one ExportReference per ServiceReference
			regs.add(reg);
		}
		
		int count = EXPORT_MAX;
		for (ExportRegistration reg : regs) {			
			reg.close(); //Unexport !
			assertNull(reg.getExportReference()); //Now that is has been closed
			
			if(--count > 0){
				assertNotNull(creator.getExportReference(sref)); //ServiceReference is still exported since there is other exportRegistration linked to it
			}
		}
		
		//assertEquals(0, creator.getAllExportReference().size()); //The ExportReference has been closed since there is no more exportRegistration linked to it.
		verify(registry).remove(xref); //Verify that the ExportReference has been removed from the ExportRegistry.
		
		creator.stop(); //Invalidate the instance
	}
	
	
	/**
	 * Convenient implementation of {@link AbstractExporterComponent} in order to test it.
	 * @author barjo
	 */
	public class TestedClass extends AbstractExporterComponent {
		private Collection<EndpointDescription> descs = new HashSet<EndpointDescription>();
		
		@Override
		protected EndpointDescription createEndpoint(ServiceReference sref,
				Map<String, Object> extraProperties) {
			EndpointDescription desc = mock(EndpointDescription.class);
			descs.add(desc);
			
			return desc;
		}

		@Override
		protected void destroyEndpoint(EndpointDescription endesc) {
			descs.remove(endesc);
		}

		@Override
		protected LogService getLogService() {
			return logservice;
		}

		@Override
		protected EventAdmin getEventAdmin() {
			return eventadmin;
		}		
		
		@Override
		protected ExportRegistryProvisioning getExportRegistry() {
			return registry;
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

	}
}
