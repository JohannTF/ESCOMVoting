package com.github.escom.escomvoting.service;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.escom.escomvoting.exception.VotingException;
import com.github.escom.escomvoting.model.dto.CreateUserRequest;
import com.github.escom.escomvoting.model.dto.UpdateProfileRequest;
import com.github.escom.escomvoting.model.dto.UpdateUserRequest;
import com.github.escom.escomvoting.model.dto.UserDTO;
import com.github.escom.escomvoting.model.entity.User;
import com.github.escom.escomvoting.model.entity.UserRole;
import com.github.escom.escomvoting.repository.UserRepository;
import com.github.escom.escomvoting.util.PasswordGenerator;
import com.github.escom.escomvoting.util.StringUtils;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }


    @Transactional
    public UserDTO createUser(CreateUserRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw VotingException.badRequest("El correo ya está registrado");
        }
        if (userRepository.existsByInstitutionalId(req.institutionalId())) {
            throw VotingException.badRequest("El número de boleta/empleado ya está registrado");
        }

        String plainPassword = (req.password() == null || req.password().isBlank())
                ? PasswordGenerator.generate()
                : req.password().trim();

        User user = new User();
        user.setInstitutionalId(req.institutionalId());
        user.setEmail(req.email());
        user.setName(req.name());
        user.setRole(UserRole.fromString(req.role()));
        user.setPasswordHash(passwordEncoder.encode(plainPassword));
        user.setAdmin(req.admin());

        User saved = userRepository.save(user);
        emailService.sendWelcomeEmail(saved.getEmail(), saved.getName(), plainPassword);
        return UserDTO.from(saved);
    }

    public UserDTO getUser(UUID id) {
        return UserDTO.from(userRepository.findByIdOrThrow(id));
    }

    @Transactional
    public UserDTO updateUser(UUID id, UpdateUserRequest req) {
        User user = userRepository.findByIdOrThrow(id);

        String institutionalId = StringUtils.trimmed(req.institutionalId());
        String email = StringUtils.trimmed(req.email());
        String name = StringUtils.trimmed(req.name());
        if (institutionalId.isBlank() || email.isBlank() || name.isBlank()
                || req.role() == null || req.role().isBlank()) {
            throw VotingException.badRequest("Boleta/empleado, correo, nombre y rol son obligatorios");
        }

        if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(email)) {
            throw VotingException.badRequest("El correo ya está registrado");
        }
        if (!institutionalId.equals(user.getInstitutionalId())
                && userRepository.existsByInstitutionalId(institutionalId)) {
            throw VotingException.badRequest("El número de boleta/empleado ya está registrado");
        }

        user.setInstitutionalId(institutionalId);
        user.setEmail(email);
        user.setName(name);
        user.setRole(UserRole.fromString(req.role()));
        user.setAdmin(req.admin());
        user.setActive(req.active());
        return UserDTO.from(userRepository.save(user));
    }

    @Transactional
    public UserDTO resetCredentials(UUID id) {
        User user = userRepository.findByIdOrThrow(id);
        String newPassword = PasswordGenerator.generate();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        User saved = userRepository.save(user);
        emailService.sendCredentialsResetEmail(saved.getEmail(), saved.getName(), newPassword);
        return UserDTO.from(saved);
    }

    @Transactional
    public void deleteUser(UUID id, User actingAdmin) {
        if (actingAdmin != null && actingAdmin.getId().equals(id)) {
            throw VotingException.badRequest("No puedes eliminar tu propia cuenta");
        }
        userRepository.delete(userRepository.findByIdOrThrow(id));
    }

    @Transactional
    public UserDTO updateProfile(User authenticatedUser, UpdateProfileRequest req) {
        User user = userRepository.findById(authenticatedUser.getId())
                .orElseThrow(() -> VotingException.notFound("Usuario no encontrado"));

        if (req.name() != null && !req.name().trim().isEmpty()) {
            user.setName(req.name().trim());
        }

        if (req.newPassword() != null && !req.newPassword().trim().isEmpty()) {
            if (req.currentPassword() == null || req.currentPassword().trim().isEmpty()) {
                throw VotingException.badRequest("Debe proporcionar la contraseña actual");
            }
            if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
                throw VotingException.badRequest("La contraseña actual es incorrecta");
            }
            user.setPasswordHash(passwordEncoder.encode(req.newPassword().trim()));
        }

        if (req.admin() != null) {
            user.setAdmin(req.admin());
        }

        return UserDTO.from(userRepository.save(user));
    }
}
