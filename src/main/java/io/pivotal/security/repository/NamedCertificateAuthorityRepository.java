package io.pivotal.security.repository;

import io.pivotal.security.entity.NamedCertificateAuthority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NamedCertificateAuthorityRepository extends JpaRepository<NamedCertificateAuthority, Long> {
  List<NamedCertificateAuthority> findAllByNameIgnoreCaseOrderByUpdatedAtDesc(String name);
  NamedCertificateAuthority findFirstByNameIgnoreCaseOrderByUpdatedAtDesc(String name);
  NamedCertificateAuthority findOneByUuid(UUID uuid);
}
