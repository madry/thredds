package thredds.server.viewer;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import thredds.mock.web.MockTdsContextLoader;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="/WEB-INF/applicationContext-tdsConfig.xml",  loader = MockTdsContextLoader.class)
public class ViewControllerTest{
	
	@Autowired
	private ViewerController viewerController;
			
	
	@Test
	public void testLaunchViewerIDVRequest() throws IOException{
		
		ViewerRequestParamsBean params = new ViewerRequestParamsBean(); 		
		params.setUrl("http://localhost:9080/thredds/dodsC/ncss_tests/files/GFS_CONUS_80km_20120227_0000.grib1");
		params.setViewer("idv");		
		BindingResult result = new BeanPropertyBindingResult(params, "params");
		MockHttpServletResponse res = new MockHttpServletResponse();
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setRequestURI("/thredds/view/idv.jnlp?url=http://localhost:9080/thredds/dodsC/ncss_tests/files/GFS_CONUS_80km_20120227_0000.grib1");
		viewerController.launchViewer(params, result, res, req);		
		assertEquals(200, res.getStatus() );
		assertEquals("application/x-java-jnlp-file", res.getContentType() );
	}
	
	@Test
	public void testLaunchViewerToolsUIVRequest() throws IOException{
		
		ViewerRequestParamsBean params = new ViewerRequestParamsBean(); 		
		params.setViewer("ToolsUI");
		params.setCatalog("http://localhost:9080/thredds/catalog/ncss_tests/files/catalog.xml");
		params.setDataset("ncss_tests/files/GFS_CONUS_80km_20120227_0000.grib1");
		BindingResult result = new BeanPropertyBindingResult(params, "params");
		MockHttpServletResponse res = new MockHttpServletResponse();
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setRequestURI("/thredds/view/ToolsUI.jnlp?catalog=http://localhost:9080/thredds/catalog/ncss_tests/files/catalog.xml&dataset=ncss_tests/files/GFS_CONUS_80km_20120227_0000.grib1");
		viewerController.launchViewer(params, result, res, req);		
		assertEquals(200, res.getStatus() );
		assertEquals("application/x-java-jnlp-file", res.getContentType() );
	}
	
	@Test
	public void testBadRequest() throws IOException{
	
		ViewerRequestParamsBean params = new ViewerRequestParamsBean();
		params.setViewer("ToolsUI");
		params.setCatalog("wrong_catalog.xml");//
		params.setDataset("wrong_dataset.nc"); //		
		BindingResult result = new BeanPropertyBindingResult(params, "params");
		MockHttpServletResponse res = new MockHttpServletResponse();
		MockHttpServletRequest req = new MockHttpServletRequest();		
		req.setRequestURI("/thredds/view/ToolsUI.jnlp?catalog=http://localhost:9080/thredds/catalog/ncss_tests/files/catalog.xml&dataset=ncss_tests/files/GFS_CONUS_80km_20120227_0000.grib1");
		viewerController.launchViewer(params, result, res, req);
		assertEquals(404, res.getStatus() );
	} 

}