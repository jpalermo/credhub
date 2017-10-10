package io.pivotal.security.service;

import io.pivotal.security.auth.UserContext;
import io.pivotal.security.data.PermissionsDataService;
import io.pivotal.security.domain.CredentialVersion;
import io.pivotal.security.entity.Credential;
import io.pivotal.security.exceptions.EntryNotFoundException;
import io.pivotal.security.exceptions.InvalidAclOperationException;
import io.pivotal.security.request.PermissionEntry;
import io.pivotal.security.request.PermissionOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static io.pivotal.security.request.PermissionOperation.READ_ACL;
import static io.pivotal.security.request.PermissionOperation.WRITE_ACL;

@Service
public class PermissionService {

  private PermissionsDataService permissionsDataService;
  private PermissionCheckingService permissionCheckingService;

  @Autowired
  public PermissionService(PermissionsDataService permissionsDataService, PermissionCheckingService permissionCheckingService) {
    this.permissionsDataService = permissionsDataService;
    this.permissionCheckingService = permissionCheckingService;
  }

  public List<PermissionOperation> getAllowedOperationsForLogging(String credentialName, String actor) {
    return permissionsDataService.getAllowedOperations(credentialName, actor);
  }

  public void saveAccessControlEntries(UserContext userContext, CredentialVersion credentialVersion, List<PermissionEntry> permissionEntryList) {
    if (permissionEntryList.size() == 0) {
      return;
    }
    if (!permissionCheckingService.hasPermission(userContext.getAclUser(), credentialVersion.getName(), WRITE_ACL)) {
      throw new EntryNotFoundException("error.credential.invalid_access");
    }

    permissionsDataService.saveAccessControlEntries(credentialVersion.getCredential(), permissionEntryList);
  }

  public List<PermissionEntry> getAccessControlList(UserContext userContext, CredentialVersion credentialVersion) {
    if (!permissionCheckingService.hasPermission(userContext.getAclUser(), credentialVersion.getName(), READ_ACL)) {
      throw new EntryNotFoundException("error.credential.invalid_access");
    }

    return getPermissionEntries(credentialVersion.getCredential());
  }

  public boolean deleteAccessControlEntry(UserContext userContext, String credentialName, String actor) {
    if (!permissionCheckingService
        .hasPermission(userContext.getAclUser(), credentialName, WRITE_ACL)) {
      throw new EntryNotFoundException("error.credential.invalid_access");
    }

    if (!permissionCheckingService.userAllowedToOperateOnActor(userContext, actor)) {
      throw new InvalidAclOperationException("error.acl.invalid_update_operation");
    }

    return permissionsDataService.deleteAccessControlEntry(credentialName, actor);
  }

  private List<PermissionEntry> getPermissionEntries(Credential credential) {
    return permissionsDataService.getAccessControlList(credential);
  }
}
