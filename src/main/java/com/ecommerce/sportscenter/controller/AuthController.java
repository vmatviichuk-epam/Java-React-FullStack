package com.ecommerce.sportscenter.controller;

import com.ecommerce.sportscenter.entity.User;
import com.ecommerce.sportscenter.model.JwtRequest;
import com.ecommerce.sportscenter.model.JwtResponse;
import com.ecommerce.sportscenter.model.MandatoryPasswordChangeRequest;
import com.ecommerce.sportscenter.model.PasswordChangeRequest;
import com.ecommerce.sportscenter.model.PasswordValidationResponse;
import com.ecommerce.sportscenter.repository.UserRepository;
import com.ecommerce.sportscenter.security.JwtHelper;
import com.ecommerce.sportscenter.service.PasswordService;
import com.ecommerce.sportscenter.service.PasswordValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and authorization operations")
public class AuthController {
    private final UserDetailsService userDetailsService;
    private final AuthenticationManager manager;
    private final JwtHelper jwtHelper;
    private final PasswordService passwordService;
    private final UserRepository userRepository;

    public AuthController(UserDetailsService userDetailsService, AuthenticationManager manager, JwtHelper jwtHelper, 
                         PasswordService passwordService, UserRepository userRepository) {
        this.userDetailsService = userDetailsService;
        this.manager = manager;
        this.jwtHelper = jwtHelper;
        this.passwordService = passwordService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and generate JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful", 
                     content = @Content(mediaType = "application/json", 
                                      schema = @Schema(implementation = JwtResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials", 
                     content = @Content(mediaType = "text/plain"))
    })
    public ResponseEntity<?> login(@RequestBody JwtRequest request, HttpServletRequest httpRequest){
        try {
            this.authenticate(request.getUsername(), request.getPassword());
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            
            // Check if user needs mandatory password change
            User user = userRepository.findByUsername(request.getUsername()).orElse(null);
            if (user != null && passwordService.needsMandatoryPasswordChange(user, request.getPassword())) {
                return ResponseEntity.ok().body(new PasswordValidationResponse(
                    false, 
                    null, 
                    passwordService.getPasswordRequirements(),
                    true
                ));
            }
            
            String token = this.jwtHelper.generateToken(userDetails);
            JwtResponse response = JwtResponse.builder()
                    .username(userDetails.getUsername())
                    .token(token)
                    .build();
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadCredentialsException ex) {
            return new ResponseEntity<>("Invalid UserName or Password", HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/user")
    @Operation(summary = "Get user details", description = "Retrieve user details from JWT token")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User details retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired token")
    })
    public ResponseEntity<UserDetails> getUserDetails(@RequestHeader("Authorization") String tokenHeader){
        String token = extractTokenFromHeader(tokenHeader);
        if(token!=null){
            String username = jwtHelper.getUserNameFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            return new ResponseEntity<>(userDetails, HttpStatus.OK);
        }else{
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangeRequest request, 
                                          @RequestHeader("Authorization") String tokenHeader,
                                          HttpServletRequest httpRequest) {
        try {
            String token = extractTokenFromHeader(tokenHeader);
            if (token == null) {
                return new ResponseEntity<>("Invalid token", HttpStatus.UNAUTHORIZED);
            }
            
            String username = jwtHelper.getUserNameFromToken(token);
            
            if (!request.getNewPassword().equals(request.getRepeatPassword())) {
                return new ResponseEntity<>("Passwords do not match", HttpStatus.BAD_REQUEST);
            }
            
            String clientIp = httpRequest.getRemoteAddr();
            passwordService.changePassword(username, request.getNewPassword(), clientIp);
            
            return ResponseEntity.ok().body("Password changed successfully");
        } catch (RuntimeException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/mandatory-password-change")
    public ResponseEntity<?> mandatoryPasswordChange(@RequestBody MandatoryPasswordChangeRequest request, 
                                                   HttpServletRequest httpRequest) {
        try {
            // Authenticate user with old credentials
            this.authenticate(request.getUsername(), request.getCurrentPassword());
            
            if (!request.getNewPassword().equals(request.getRepeatPassword())) {
                return new ResponseEntity<>("Passwords do not match", HttpStatus.BAD_REQUEST);
            }
            
            String clientIp = httpRequest.getRemoteAddr();
            passwordService.changePassword(request.getUsername(), request.getNewPassword(), clientIp);
            
            // After successful password change, generate JWT token for the user
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            String token = this.jwtHelper.generateToken(userDetails);
            JwtResponse response = JwtResponse.builder()
                    .username(userDetails.getUsername())
                    .token(token)
                    .build();
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/validate-password")
    public ResponseEntity<PasswordValidationResponse> validatePassword(@RequestBody String password) {
        PasswordValidationService.ValidationResult result = passwordService.validatePasswordComplexity(password);
        PasswordValidationResponse response = PasswordValidationResponse.builder()
                .valid(result.isValid())
                .errors(result.getErrors())
                .requirements(result.getRequirements())
                .requiresMandatoryChange(false)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/password-requirements")
    public ResponseEntity<PasswordValidationResponse> getPasswordRequirements() {
        PasswordValidationResponse response = PasswordValidationResponse.builder()
                .valid(true)
                .errors(null)
                .requirements(passwordService.getPasswordRequirements())
                .requiresMandatoryChange(false)
                .build();
        return ResponseEntity.ok(response);
    }

    private String extractTokenFromHeader(String tokenHeader) {
        if(tokenHeader!=null && tokenHeader.startsWith("Bearer ")){
            return tokenHeader.substring(7); // Removing Bearer
        }
        return null;
    }

    private void authenticate(String username, String password) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
        try{
            manager.authenticate(authenticationToken);
        }
        catch(BadCredentialsException ex){
            throw new BadCredentialsException("Invalid UserName or Password");
        }
    }
}
