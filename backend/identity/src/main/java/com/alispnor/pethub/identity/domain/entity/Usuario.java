package com.alispnor.pethub.identity.domain.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"senhaHash", "roles"})
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(nullable = false, unique = true, length = 200)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Column(unique = true, length = 11)
    private String cpf;

    @Column(length = 20)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_usuario", nullable = false, length = 20)
    private TipoUsuario tipo;

    @ElementCollection(fetch = FetchType.EAGER, targetClass = Role.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "usuario_roles", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "role", nullable = false, length = 30)
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;

    @Column(name = "tentativas_falhas", nullable = false)
    @Builder.Default
    private int tentativasFalhas = 0;

    @Column(name = "bloqueado_ate")
    private LocalDateTime bloqueadoAte;

    @CreationTimestamp
    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @Column(name = "ultimo_login")
    private LocalDateTime ultimoLogin;

    public boolean estaBloqueado() {
        return bloqueadoAte != null && bloqueadoAte.isAfter(LocalDateTime.now());
    }

    public void registrarLoginSucesso() {
        this.tentativasFalhas = 0;
        this.bloqueadoAte = null;
        this.ultimoLogin = LocalDateTime.now();
    }

    public void registrarLoginFalha(int maxTentativas, int minutosBloqueio) {
        this.tentativasFalhas++;
        if (this.tentativasFalhas >= maxTentativas) {
            this.bloqueadoAte = LocalDateTime.now().plusMinutes(minutosBloqueio);
        }
    }
}
