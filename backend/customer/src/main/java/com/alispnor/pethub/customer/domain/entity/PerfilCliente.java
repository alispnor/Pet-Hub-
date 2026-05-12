package com.alispnor.pethub.customer.domain.entity;

import com.alispnor.pethub.common.security.EncryptedStringConverter;
import com.alispnor.pethub.identity.domain.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "perfil_cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"cpfCriptografado", "cpfHash", "usuario"})
public class PerfilCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "cpf_criptografado", length = 255)
    private String cpfCriptografado;

    @Column(name = "cpf_hash", length = 64, unique = true)
    private String cpfHash;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Genero genero;

    @Column(name = "telefone_adicional", length = 20)
    private String telefoneAdicional;

    @Column(name = "aceite_termos", nullable = false)
    @Builder.Default
    private boolean aceiteTermos = false;

    @Column(name = "aceite_termos_em")
    private LocalDateTime aceiteTermosEm;

    @Column(name = "aceite_marketing", nullable = false)
    @Builder.Default
    private boolean aceiteMarketing = false;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
