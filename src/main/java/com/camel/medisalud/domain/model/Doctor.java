package com.camel.medisalud.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
public class Doctor extends BaseEntity {

    @NotBlank
    @Size(max = 150)
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @NotBlank
    @Size(max = 100)
    @Column(name = "specialty", nullable = false, length = 100)
    private String specialty;

    @Size(max = 30)
    @Column(name = "phone", length = 30)
    private String phone;

    @Email
    @Size(max = 150)
    @Column(name = "email", length = 150)
    private String email;
}
