package io.mosip.resident.service.impl;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.kernel.core.http.ResponseWrapper;

@RunWith(MockitoJUnitRunner.class)
@RefreshScope
@ContextConfiguration
public class ResidentConfigServiceImplTest {

	@InjectMocks
	private ResidentConfigServiceImpl configServiceImpl;

	@Mock
	private Environment env;

	@Mock
	private Resource residentUiSchemaJsonFile;

	@Mock
	private Resource identityMappingJsonFile;

	@Before
	public void setUp() throws Exception {
		Mockito.when(residentUiSchemaJsonFile.getInputStream())
				.thenReturn(new ByteArrayInputStream("{\"name\":\"ui-schema\"}".getBytes()));
		Mockito.when(identityMappingJsonFile.getInputStream())
				.thenReturn(new ByteArrayInputStream("{\"name\":\"identity-mapping\"}".getBytes()));
	}

	private ResidentConfigServiceImpl createTestSubject() {
		return configServiceImpl;
	}

	@Test
	public void testGetUIProperties_emptyPropArray() throws Exception {
		ResidentConfigServiceImpl testSubject;
		ResponseWrapper<?> result;

		// default test
		testSubject = createTestSubject();
		ReflectionTestUtils.setField(testSubject, "propKeys", new String[0]);
		ReflectionTestUtils.setField(testSubject, "env", env);
		result = testSubject.getUIProperties();
		Set resultProps = ((Map) result.getResponse()).keySet();
		assertTrue(resultProps.size() == 0);
	}

	@Test
	public void testGetUIProperties_nonEmptyPropArray() throws Exception {
		ResidentConfigServiceImpl testSubject;
		ResponseWrapper<?> result;

		// default test
		testSubject = createTestSubject();
		String[] propKeys = new String[] { "aaa.key", "bbb.key", "ccc.key" };
		ReflectionTestUtils.setField(testSubject, "propKeys", propKeys);
		when(env.getProperty("aaa.key", Object.class)).thenReturn("aaa");
		when(env.getProperty("bbb.key", Object.class)).thenReturn("bbb");
		ReflectionTestUtils.setField(testSubject, "env", env);
		result = testSubject.getUIProperties();
		Set resultProps = ((Map) result.getResponse()).keySet();
		assertTrue(resultProps.size() == 2);
		assertTrue(resultProps.contains("aaa.key"));
		assertTrue(resultProps.contains("bbb.key"));

	}

	@Test
	public void testGetUISchema() throws Exception {
		ResidentConfigServiceImpl testSubject;

		testSubject = createTestSubject();
		String uiSchema = "ui-schema-json";
		ReflectionTestUtils.setField(testSubject, "uiSchema", uiSchema);
		String result = testSubject.getUISchema();
		assertTrue(result.contains(uiSchema));
	}

	@Test
	public void testGetUISchemaTry() throws Exception {
		ResidentConfigServiceImpl testSubject;

		testSubject = createTestSubject();
		String uiSchema = null;
		ReflectionTestUtils.setField(testSubject, "uiSchema", uiSchema);
		uiSchema = "{\"name\":\"ui-schema\"}";
		String result = testSubject.getUISchema();
		assertTrue(result.contains(uiSchema));
	}

	@Test
	public void testGetIdentityMapping() throws Exception {
		ResidentConfigServiceImpl testSubject;

		testSubject = createTestSubject();
		String identityMapping = "identity-mapping-json";
		ReflectionTestUtils.setField(testSubject, "identityMapping", identityMapping);
		String result = testSubject.getIdentityMapping();
		assertTrue(result.contains(identityMapping));
	}

	@Test
	public void testGetIdentityMappingTry() throws Exception {
		ResidentConfigServiceImpl testSubject;

		testSubject = createTestSubject();
		String identityMapping = null;
		ReflectionTestUtils.setField(testSubject, "identityMapping", identityMapping);
		identityMapping = "{\"name\":\"identity-mapping\"}";
		String result = testSubject.getIdentityMapping();
		assertTrue(result.contains(identityMapping));
	}
}