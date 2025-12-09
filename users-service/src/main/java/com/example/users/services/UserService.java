package com.example.users.services;

import com.example.users.dtos.AuthDTO;
import com.example.users.dtos.UserDTO;
import com.example.users.dtos.UserDetailsDTO;
import com.example.users.dtos.builders.UserBuilder;
import com.example.users.entities.User;
import com.example.users.handlers.exceptions.model.ResourceNotFoundException;
import com.example.users.repositories.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    // --- 1. DEFINIM VARIABILA ---
    private final RestTemplate restTemplate;

    @Autowired
    // --- 2. O INJECTĂM ÎN CONSTRUCTOR ---
    public UserService(UserRepository userRepository, RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
    }

    public List<UserDTO> findUsers() {
        List<User> userList = userRepository.findAll();
        return userList.stream()
                .map(UserBuilder::toUserDTO)
                .collect(Collectors.toList());
    }

    public UserDetailsDTO findUserById(UUID id) {
        Optional<User> prosumerOptional = userRepository.findById(id);
        if (prosumerOptional.isEmpty()) {
            LOGGER.error("Person with id {} was not found in db", id);
            throw new ResourceNotFoundException(User.class.getSimpleName() + " with id: " + id);
        }
        return UserBuilder.toUserDetailsDTO(prosumerOptional.get());
    }

    public UserDetailsDTO findUserByEmail(String email) {
        Optional<User> prosumerOptional = userRepository.findByEmail(email);
        if (prosumerOptional.isEmpty()) {
            LOGGER.error("Person with email {} was not found in db", email);
            throw new ResourceNotFoundException(User.class.getSimpleName() + " with email: " + email);
        }
        return UserBuilder.toUserDetailsDTO(prosumerOptional.get());
    }

    @Transactional
    public UUID insert(UserDetailsDTO userDetailsDTO) {
        // 1. VERIFICARE ANTI-DUPLICAT
        // Verificăm dacă userul există deja în baza de date
        Optional<User> existingUser = userRepository.findByEmail(userDetailsDTO.getEmail());

        if (existingUser.isPresent()) {
            LOGGER.warn("Userul cu emailul {} exista deja. Se sare peste insert.", userDetailsDTO.getEmail());
            // Returnăm ID-ul existent și ne oprim, pentru a nu crea dubluri sau erori
            return existingUser.get().getId();
        }

        // 2. SALVARE ÎN USERS DB (Dacă nu există deja)
        User user = UserBuilder.toEntity(userDetailsDTO);
        user = userRepository.save(user);
        LOGGER.debug("User with id {} was inserted in db", user.getId());

        // 3. SINCRONIZARE CU AUTH SERVICE
        // Trimitem un request HTTP către Auth pentru a crea contul de login
        try {
            String authUrl = "http://auth-service:8080/auth/register";

            // --- MODIFICARE AICI: Convertim Enum-ul în String ---
            // Verificăm dacă getRole() returnează null, altfel luăm .name()
            String roleToSend = (userDetailsDTO.getRole() != null) ? userDetailsDTO.getRole().name() : "USER";

            AuthDTO authPayload = new AuthDTO(user.getEmail(), "1234", roleToSend);

            restTemplate.postForEntity(authUrl, authPayload, String.class);
        } catch (Exception e) {
            LOGGER.error("FAILED to sync user with Auth Service: " + e.getMessage());
        }

        return user.getId();
    }

    public boolean existsById(UUID id) {
        Optional<User> prosumerOptional = userRepository.findById(id);
        return prosumerOptional.isPresent();
    }

    @Transactional
    public void remove(UUID id) {
        // 1. Găsim userul întâi ca să îi știm email-ul
        Optional<User> userOptional = userRepository.findById(id);

        if (userOptional.isPresent()) {
            String email = userOptional.get().getEmail();

            // 2. Ștergem din Users DB
            userRepository.deleteById(id);
            LOGGER.info("User deleted from Users DB: {}", id);

            // 3. Sincronizare: Ștergem și din Auth DB
            try {
                // Apelăm endpoint-ul creat la Etapa 1
                String authUrl = "http://auth-service:8080/auth/delete/" + email;
                restTemplate.delete(authUrl);
                LOGGER.info("Synced delete to Auth Service for email: {}", email);
            } catch (Exception e) {
                // Logăm eroarea dar nu oprim procesul (userul e deja șters local)
                LOGGER.error("FAILED to delete credentials from Auth Service: " + e.getMessage());
            }
        } else {
            LOGGER.warn("User with ID {} not found, cannot delete.", id);
            throw new ResourceNotFoundException(User.class.getSimpleName() + " with id: " + id);
        }
    }

    @Transactional
    public UserDetailsDTO updateFully(UUID id, @Valid UserDetailsDTO dto) {
        User entity = null;
        try {
            entity = userRepository.findById(id).orElseThrow(ChangeSetPersister.NotFoundException::new);
        } catch (ChangeSetPersister.NotFoundException e) {
            LOGGER.error("User with id {} was not found in db", id);
            throw new RuntimeException(e);
        }
        entity.setName(dto.getName());
        entity.setEmail(dto.getEmail());
        entity.setAge(dto.getAge());
        entity.setRole(dto.getRole());
        return UserBuilder.toUserDetailsDTO(userRepository.save(entity));
    }
}