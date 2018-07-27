package org.cloudfoundry.credhub.handler;

import org.cloudfoundry.credhub.audit.CEFAuditRecord;
import org.cloudfoundry.credhub.auth.UserContext;
import org.cloudfoundry.credhub.auth.UserContextHolder;
import org.cloudfoundry.credhub.credential.CertificateCredentialValue;
import org.cloudfoundry.credhub.credential.CredentialValue;
import org.cloudfoundry.credhub.credential.StringCredentialValue;
import org.cloudfoundry.credhub.credential.UserCredentialValue;
import org.cloudfoundry.credhub.data.CertificateAuthorityService;
import org.cloudfoundry.credhub.domain.CredentialVersion;
import org.cloudfoundry.credhub.domain.PasswordCredentialVersion;
import org.cloudfoundry.credhub.entity.Credential;
import org.cloudfoundry.credhub.helper.TestHelper;
import org.cloudfoundry.credhub.request.CertificateSetRequest;
import org.cloudfoundry.credhub.request.PasswordSetRequest;
import org.cloudfoundry.credhub.request.PermissionEntry;
import org.cloudfoundry.credhub.request.StringGenerationParameters;
import org.cloudfoundry.credhub.request.UserSetRequest;
import org.cloudfoundry.credhub.service.PermissionService;
import org.cloudfoundry.credhub.service.PermissionedCredentialService;
import org.cloudfoundry.credhub.util.TestConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class SetHandlerTest {
  private PermissionedCredentialService credentialService;
  private CertificateAuthorityService certificateAuthorityService;

  private SetHandler subject;

  private StringGenerationParameters generationParameters;
  private CredentialVersion credentialVersion;
  private UUID uuid;

  private CEFAuditRecord auditRecord;

  @Before
  public void setUp() {
    TestHelper.getBouncyCastleProvider();
    credentialService = mock(PermissionedCredentialService.class);
    certificateAuthorityService = mock(CertificateAuthorityService.class);

    auditRecord = new CEFAuditRecord();

    UserContext userContext = new UserContext();
    UserContextHolder userContextHolder = new UserContextHolder();
    userContextHolder.setUserContext(userContext);
    subject = new SetHandler(credentialService, certificateAuthorityService, auditRecord);

    generationParameters = new StringGenerationParameters();
    credentialVersion = mock(PasswordCredentialVersion.class);

    uuid = UUID.randomUUID();
    String name = "federation";

    Credential cred = new Credential(name);
    cred.setUuid(uuid);

    when(credentialVersion.getCredential()).thenReturn(cred);
    when(credentialVersion.getName()).thenReturn(cred.getName());
    when(credentialVersion.getUuid()).thenReturn(cred.getUuid());
    when(credentialService.save(any(),any(), any())).thenReturn(credentialVersion);
  }

  @Test
  public void handleSetRequest_AddsTheCredentialNameToTheAuditRecord() {
    StringCredentialValue password = new StringCredentialValue("federation");
    PasswordSetRequest setRequest = new PasswordSetRequest();

    setRequest.setType("password");
    setRequest.setGenerationParameters(generationParameters);
    setRequest.setPassword(password);
    setRequest.setName("/captain");

    subject.handle(setRequest);

    verify(credentialService).save(null, password, setRequest);
    assertThat(auditRecord.getResourceName(), equalTo("federation"));
    assertThat(auditRecord.getResourceUUID(), equalTo(uuid.toString()));
    assertThat(auditRecord.getVersionUUID(), equalTo(credentialVersion.getUuid().toString()));
  }

  @Test
  public void handleSetRequest_whenPasswordSetRequest_passesCorrectParametersIncludingGeneration() {
    StringCredentialValue password = new StringCredentialValue("federation");
    PasswordSetRequest setRequest = new PasswordSetRequest();

    setRequest.setType("password");
    setRequest.setGenerationParameters(generationParameters);
    setRequest.setPassword(password);
    setRequest.setName("/captain");

    subject.handle(setRequest);

    verify(credentialService).save(null, password, setRequest);
  }

  @Test
  public void handleSetRequest_whenNonPasswordSetRequest_passesCorrectParametersWithNullGeneration() {
    UserSetRequest setRequest = new UserSetRequest();
    final UserCredentialValue userCredentialValue = new UserCredentialValue(
        "Picard",
        "Enterprise",
        "salt");

    setRequest.setType("user");
    setRequest.setName("/captain");
    setRequest.setUserValue(userCredentialValue);

    subject.handle(setRequest);

    verify(credentialService).save(null, userCredentialValue, setRequest);
  }

  @Test
  public void handleSetRequest_withACertificateSetRequest_andNoCaName_usesCorrectParameters() {
    CertificateSetRequest setRequest = new CertificateSetRequest();
    final CertificateCredentialValue certificateValue = new CertificateCredentialValue(
        null,
        "Picard",
        "Enterprise",
        null);

    setRequest.setType("certificate");
    setRequest.setName("/captain");
    setRequest.setCertificateValue(certificateValue);

    subject.handle(setRequest);

    verify(credentialService).save(null, certificateValue, setRequest);
  }

  @Test
  public void handleSetRequest_withACertificateSetRequest_andACaName_providesCaCertificate() {
    CertificateCredentialValue cerificateAuthority = new CertificateCredentialValue(
        null,
        TestConstants.TEST_CA,
        null,
        null
    );
    when(certificateAuthorityService.findActiveVersion("/test-ca-name"))
        .thenReturn(cerificateAuthority);

    CertificateSetRequest setRequest = new CertificateSetRequest();
    final CertificateCredentialValue credentialValue = new CertificateCredentialValue(
        null,
        TestConstants.TEST_CERTIFICATE,
        "Enterprise",
        "test-ca-name");

    setRequest.setType("certificate");
    setRequest.setName("/captain");
    setRequest.setCertificateValue(credentialValue);

    CertificateCredentialValue expectedCredentialValue = new CertificateCredentialValue(
        TestConstants.TEST_CA,
        TestConstants.TEST_CERTIFICATE,
        "Enterprise",
        "/test-ca-name"
    );
    ArgumentCaptor<CredentialValue> credentialValueArgumentCaptor = ArgumentCaptor.forClass(CredentialValue.class);

    subject.handle(setRequest);

    verify(credentialService).save( eq(null), credentialValueArgumentCaptor.capture(), eq(setRequest));
    assertThat(credentialValueArgumentCaptor.getValue(), samePropertyValuesAs(expectedCredentialValue));
  }
}
